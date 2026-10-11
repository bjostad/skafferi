package com.skafferi.service;

import com.skafferi.dto.NotificationSummaryDto;
import com.skafferi.dto.PantryItemSummaryDto;
import com.skafferi.dto.SettingsDto;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final SettingsService settingsService;
    private final InventoryService inventoryService;

    public EmailNotificationService(SettingsService settingsService, InventoryService inventoryService) {
        this.settingsService = settingsService;
        this.inventoryService = inventoryService;
    }

    private JavaMailSenderImpl createMailSender(SettingsDto settings) {
        if (settings.smtpHost() == null || settings.smtpHost().isBlank()) {
            throw new IllegalStateException("SMTP Host is not configured");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.smtpHost().trim());
        sender.setPort(settings.smtpPort() != null ? settings.smtpPort() : 587);

        if (settings.smtpUsername() != null && !settings.smtpUsername().isBlank()) {
            sender.setUsername(settings.smtpUsername().trim());
        }
        if (settings.smtpPassword() != null && !settings.smtpPassword().isBlank()) {
            sender.setPassword(settings.smtpPassword());
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(settings.smtpUsername() != null && !settings.smtpUsername().isBlank()));
        props.put("mail.smtp.connectiontimeout", "6000");
        props.put("mail.smtp.timeout", "6000");
        props.put("mail.smtp.writetimeout", "6000");

        String encryption = settings.smtpEncryption() != null ? settings.smtpEncryption().toUpperCase() : "TLS";
        if ("SSL".equals(encryption)) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(sender.getPort()));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else if ("TLS".equals(encryption)) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        return sender;
    }

    public void sendEmail(String subject, String htmlContent) {
        SettingsDto raw = settingsService.getRawSettings();
        if (raw.smtpHost() == null || raw.smtpHost().isBlank()) {
            throw new IllegalStateException("SMTP Host is not configured");
        }
        if (raw.smtpRecipients() == null || raw.smtpRecipients().isBlank()) {
            throw new IllegalStateException("No recipient email addresses specified");
        }

        JavaMailSenderImpl mailSender = createMailSender(raw);

        String from = (raw.smtpFromAddress() != null && !raw.smtpFromAddress().isBlank())
                ? raw.smtpFromAddress().trim()
                : (raw.smtpUsername() != null && !raw.smtpUsername().isBlank() ? raw.smtpUsername().trim() : "skafferi@home.lan");

        String[] recipients = Arrays.stream(raw.smtpRecipients().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank() && s.contains("@"))
                .toArray(String[]::new);

        if (recipients.length == 0) {
            throw new IllegalStateException("Valid recipient email addresses required");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent email notification: '{}' to {} recipients", subject, recipients.length);
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage(), e);
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    public void sendTestEmail() {
        SettingsDto raw = settingsService.getRawSettings();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 24px; margin: 0;">
                  <div style="max-width: 600px; margin: 0 auto; background-color: #1e293b; border: 1px solid #334155; border-radius: 16px; overflow: hidden; padding: 28px; box-shadow: 0 10px 25px rgba(0,0,0,0.5);">
                    <div style="margin-bottom: 20px;">
                      <span style="color: #6ee7b7; font-size: 22px; font-weight: 800;">Skafferi</span>
                      <span style="margin-left: 8px; background: #064e3b; color: #34d399; font-size: 11px; padding: 3px 8px; border-radius: 9999px; font-weight: 700;">TEST SUCCESSFUL</span>
                    </div>
                    <p style="font-size: 15px; color: #e2e8f0; line-height: 1.5; margin: 0 0 12px 0;">
                      Your SMTP server connection to <strong>%s</strong> is working perfectly!
                    </p>
                    <p style="font-size: 13px; color: #94a3b8; line-height: 1.5; margin: 0 0 20px 0;">
                      Daily expiration alerts and weekly grocery prep emails will be dispatched to this household inbox.
                    </p>
                    <div style="padding: 14px; background-color: #0f172a; border: 1px solid #334155; border-radius: 12px; font-size: 12px; color: #94a3b8; line-height: 1.6;">
                      <div><strong>Host:</strong> %s:%d</div>
                      <div><strong>Encryption:</strong> %s</div>
                      <div><strong>From:</strong> %s</div>
                      <div><strong>Sent At:</strong> %s</div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                raw.smtpHost(),
                raw.smtpHost(),
                raw.smtpPort() != null ? raw.smtpPort() : 587,
                raw.smtpEncryption() != null ? raw.smtpEncryption() : "TLS",
                raw.smtpFromAddress() != null && !raw.smtpFromAddress().isBlank() ? raw.smtpFromAddress() : raw.smtpUsername(),
                dateStr
        );

        sendEmail("Skafferi - SMTP Test Connection Successful", html);
    }

    public void sendDailyExpirationDigest() {
        NotificationSummaryDto notifs = inventoryService.getNotificationSummary();
        int totalActionable = notifs.expiredCount() + notifs.expiringSoonCount() + notifs.freshCheckCount();
        if (totalActionable == 0) {
            log.info("Daily expiration digest skipped: no expiring or expired items.");
            return;
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));

        StringBuilder itemsHtml = new StringBuilder();

        // 1. Expired Items
        if (!notifs.expiredItems().isEmpty()) {
            itemsHtml.append("""
                    <div style="margin-bottom: 20px;">
                      <div style="display: inline-block; background-color: #450a0a; color: #f87171; border: 1px solid #991b1b; padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 700; margin-bottom: 8px;">
                        EXPIRED (%d items)
                      </div>
                      <div style="background-color: #0f172a; border: 1px solid #334155; border-radius: 12px; padding: 12px;">
                    """.formatted(notifs.expiredItems().size()));
            for (PantryItemSummaryDto item : notifs.expiredItems()) {
                itemsHtml.append("""
                        <div style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #1e293b; font-size: 13px;">
                          <span style="color: #fca5a5; font-weight: 600;">%s</span>
                          <span style="color: #94a3b8;">%s %s</span>
                        </div>
                        """.formatted(item.name(), item.totalQuantity(), item.defaultUnit() != null ? item.defaultUnit() : ""));
            }
            itemsHtml.append("</div></div>");
        }

        // 2. Expiring Soon Items
        if (!notifs.expiringSoonItems().isEmpty()) {
            itemsHtml.append("""
                    <div style="margin-bottom: 20px;">
                      <div style="display: inline-block; background-color: #451a03; color: #fbbf24; border: 1px solid #b45309; padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 700; margin-bottom: 8px;">
                        EXPIRING SOON (%d items)
                      </div>
                      <div style="background-color: #0f172a; border: 1px solid #334155; border-radius: 12px; padding: 12px;">
                    """.formatted(notifs.expiringSoonItems().size()));
            for (PantryItemSummaryDto item : notifs.expiringSoonItems()) {
                String expDaysText = item.daysUntilEarliestExpiry() != null ? item.daysUntilEarliestExpiry() + " days left" : "Soon";
                itemsHtml.append("""
                        <div style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #1e293b; font-size: 13px;">
                          <div>
                            <span style="color: #fef08a; font-weight: 600;">%s</span>
                            <span style="color: #94a3b8; font-size: 11px; margin-left: 6px;">(%s)</span>
                          </div>
                          <span style="color: #94a3b8;">%s %s</span>
                        </div>
                        """.formatted(item.name(), expDaysText, item.totalQuantity(), item.defaultUnit() != null ? item.defaultUnit() : ""));
            }
            itemsHtml.append("</div></div>");
        }

        // 3. Fresh Produce Reminders
        if (!notifs.freshCheckItems().isEmpty()) {
            itemsHtml.append("""
                    <div style="margin-bottom: 20px;">
                      <div style="display: inline-block; background-color: #064e3b; color: #34d399; border: 1px solid #047857; padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 700; margin-bottom: 8px;">
                        FRESH PRODUCE CHECK (%d items)
                      </div>
                      <div style="background-color: #0f172a; border: 1px solid #334155; border-radius: 12px; padding: 12px;">
                    """.formatted(notifs.freshCheckItems().size()));
            for (PantryItemSummaryDto item : notifs.freshCheckItems()) {
                itemsHtml.append("""
                        <div style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #1e293b; font-size: 13px;">
                          <span style="color: #a7f3d0; font-weight: 600;">%s</span>
                          <span style="color: #94a3b8;">%s %s</span>
                        </div>
                        """.formatted(item.name(), item.totalQuantity(), item.defaultUnit() != null ? item.defaultUnit() : ""));
            }
            itemsHtml.append("</div></div>");
        }

        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 24px; margin: 0;">
                  <div style="max-width: 600px; margin: 0 auto; background-color: #1e293b; border: 1px solid #334155; border-radius: 16px; overflow: hidden; padding: 28px; box-shadow: 0 10px 25px rgba(0,0,0,0.5);">
                    <div style="margin-bottom: 16px;">
                      <span style="color: #6ee7b7; font-size: 22px; font-weight: 800;">Skafferi</span>
                      <span style="margin-left: 8px; background: #3b82f6; color: #eff6ff; font-size: 11px; padding: 3px 8px; border-radius: 9999px; font-weight: 700;">DAILY DIGEST</span>
                    </div>
                    <p style="font-size: 14px; color: #94a3b8; margin: 0 0 20px 0;">
                      %s &bull; %d items requiring household attention
                    </p>
                    %s
                    <div style="margin-top: 24px; text-align: center;">
                      <a href="http://localhost:8085" style="display: inline-block; background-color: #10b981; color: #022c22; font-weight: 700; font-size: 13px; text-decoration: none; padding: 10px 20px; border-radius: 10px;">Open Pantry Dashboard</a>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(dateStr, totalActionable, itemsHtml.toString());

        sendEmail("Skafferi Daily Expiration Digest: " + totalActionable + " items need attention", html);
    }

    public void sendWeeklyGroceryDigest() {
        NotificationSummaryDto notifs = inventoryService.getNotificationSummary();
        List<PantryItemSummaryDto> lowStock = notifs.lowStockItems();
        if (lowStock.isEmpty()) {
            log.info("Weekly grocery digest skipped: no low stock items.");
            return;
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));

        StringBuilder itemsHtml = new StringBuilder();
        itemsHtml.append("""
                <div style="background-color: #0f172a; border: 1px solid #334155; border-radius: 12px; padding: 12px; margin-bottom: 20px;">
                """);
        for (PantryItemSummaryDto item : lowStock) {
            String qty = item.isOutOfStock() ? "<span style='color: #ef4444; font-weight: 700;'>OUT OF STOCK</span>" : item.totalQuantity() + " " + (item.defaultUnit() != null ? item.defaultUnit() : "");
            itemsHtml.append("""
                    <div style="display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #1e293b; font-size: 13px;">
                      <span style="color: #f1f5f9; font-weight: 600;">%s</span>
                      <span style="color: #cbd5e1;">%s</span>
                    </div>
                    """.formatted(item.name(), qty));
        }
        itemsHtml.append("</div>");

        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 24px; margin: 0;">
                  <div style="max-width: 600px; margin: 0 auto; background-color: #1e293b; border: 1px solid #334155; border-radius: 16px; overflow: hidden; padding: 28px; box-shadow: 0 10px 25px rgba(0,0,0,0.5);">
                    <div style="margin-bottom: 16px;">
                      <span style="color: #6ee7b7; font-size: 22px; font-weight: 800;">Skafferi</span>
                      <span style="margin-left: 8px; background: #8b5cf6; color: #f5f3ff; font-size: 11px; padding: 3px 8px; border-radius: 9999px; font-weight: 700;">WEEKLY GROCERY PREP</span>
                    </div>
                    <p style="font-size: 14px; color: #94a3b8; margin: 0 0 20px 0;">
                      %s &bull; %d staples need replenishment
                    </p>
                    %s
                    <div style="margin-top: 24px; text-align: center;">
                      <a href="http://localhost:8085/#/shopping" style="display: inline-block; background-color: #8b5cf6; color: #ffffff; font-weight: 700; font-size: 13px; text-decoration: none; padding: 10px 20px; border-radius: 10px;">Review Grocery List</a>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(dateStr, lowStock.size(), itemsHtml.toString());

        sendEmail("Skafferi Weekly Grocery Run: " + lowStock.size() + " items to restock", html);
    }

    // Daily at 08:00 AM
    @Scheduled(cron = "0 0 8 * * *")
    public void scheduledDailyDigest() {
        SettingsDto raw = settingsService.getRawSettings();
        if (Boolean.TRUE.equals(raw.smtpEnabled()) && Boolean.TRUE.equals(raw.smtpDailyDigestEnabled())) {
            try {
                sendDailyExpirationDigest();
            } catch (Exception e) {
                log.warn("Scheduled daily expiration digest failed: {}", e.getMessage());
            }
        }
    }

    // Weekly on Friday at 5:00 PM
    @Scheduled(cron = "0 0 17 * * FRI")
    public void scheduledWeeklyDigest() {
        SettingsDto raw = settingsService.getRawSettings();
        if (Boolean.TRUE.equals(raw.smtpEnabled()) && Boolean.TRUE.equals(raw.smtpWeeklyDigestEnabled())) {
            try {
                sendWeeklyGroceryDigest();
            } catch (Exception e) {
                log.warn("Scheduled weekly grocery digest failed: {}", e.getMessage());
            }
        }
    }
}

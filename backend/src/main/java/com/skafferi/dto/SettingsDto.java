package com.skafferi.dto;

public record SettingsDto(
        // Bring!
        String bringEmail,
        String bringPassword,
        String bringListUuid,
        boolean bringAutoSync,

        // Mealie
        String mealieBaseUrl,
        String mealieApiToken,
        Boolean mealieAutoDeduct,

        // Kroger
        String krogerClientId,
        String krogerClientSecret,
        String krogerLocationId,

        // Authentik (OpenID Connect / OIDC)
        boolean authentikEnabled,
        String authentikIssuerUrl,
        String authentikClientId,
        String authentikClientSecret,

        // Google OAuth2
        boolean googleAuthEnabled,
        String googleClientId,
        String googleClientSecret,

        // Freshness & Expiration Reminders
        Integer freshFoodReminderDays,
        Boolean freshFoodReminderEnabled,
        Integer expirationReminderDays,
        Boolean expirationReminderEnabled,

        // SMTP Email Notifications
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        String smtpPassword,
        String smtpEncryption,
        String smtpFromAddress,
        String smtpRecipients,
        Boolean smtpEnabled,
        Boolean smtpDailyDigestEnabled,
        Boolean smtpWeeklyDigestEnabled,

        // Application Metadata
        String appVersion
) {}

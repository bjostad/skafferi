package com.skafferi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skafferi.dto.SettingsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;

@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BringSyncService bringSyncService;

    @Value("${skafferi.data-dir:./data}")
    private String dataDir;

    @Value("${skafferi.integrations.bring.email:}")
    private String defaultBringEmail;

    @Value("${skafferi.integrations.bring.password:}")
    private String defaultBringPassword;

    @Value("${skafferi.integrations.bring.auto-sync:false}")
    private boolean defaultBringAutoSync;

    @Value("${skafferi.integrations.mealie.base-url:}")
    private String defaultMealieUrl;

    @Value("${skafferi.integrations.mealie.api-token:}")
    private String defaultMealieToken;

    @Value("${skafferi.integrations.kroger.client-id:}")
    private String defaultKrogerClientId;

    @Value("${skafferi.integrations.kroger.client-secret:}")
    private String defaultKrogerClientSecret;

    @Value("${skafferi.integrations.kroger.location-id:}")
    private String defaultKrogerLocationId;

    @Value("${skafferi.auth.authentik.enabled:false}")
    private boolean defaultAuthentikEnabled;

    @Value("${skafferi.auth.authentik.issuer-url:}")
    private String defaultAuthentikIssuerUrl;

    @Value("${skafferi.auth.authentik.client-id:}")
    private String defaultAuthentikClientId;

    @Value("${skafferi.auth.authentik.client-secret:}")
    private String defaultAuthentikClientSecret;

    @Value("${skafferi.auth.google.enabled:false}")
    private boolean defaultGoogleAuthEnabled;

    @Value("${skafferi.auth.google.client-id:}")
    private String defaultGoogleClientId;

    @Value("${skafferi.auth.google.client-secret:}")
    private String defaultGoogleClientSecret;

    @Value("${skafferi.version:0.5.2-beta}")
    private String configuredVersion;

    private final org.springframework.boot.info.BuildProperties buildProperties;

    public SettingsService(BringSyncService bringSyncService,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.boot.info.BuildProperties buildProperties) {
        this.bringSyncService = bringSyncService;
        this.buildProperties = buildProperties;
    }

    public String getAppVersion() {
        if (buildProperties != null && buildProperties.getVersion() != null && !buildProperties.getVersion().isBlank()) {
            return buildProperties.getVersion();
        }
        return configuredVersion != null && !configuredVersion.isBlank() ? configuredVersion : "0.5.0-beta";
    }

    @PostConstruct
    public void init() {
        SettingsDto current = getRawSettings();
        if (current.bringEmail() != null && !current.bringEmail().isBlank() &&
            current.bringPassword() != null && !current.bringPassword().isBlank() &&
            !current.bringPassword().equals("********")) {
            log.info("Initializing Bring! integration on startup for: {}", current.bringEmail());
            bringSyncService.authenticate(current.bringEmail(), current.bringPassword());
            if (current.bringListUuid() != null && !current.bringListUuid().isBlank()) {
                bringSyncService.setDefaultListUuid(current.bringListUuid());
            }
        }
    }

    private File getSettingsFile() {
        File file = new File(dataDir, "settings.json");
        if (file.exists()) {
            return file;
        }
        File fallback = new File("./data/settings.json");
        if (fallback.exists()) {
            return fallback;
        }
        return file;
    }

    private SettingsDto getRawSettings() {
        File file = getSettingsFile();
        if (file.exists()) {
            try {
                return objectMapper.readValue(file, SettingsDto.class);
            } catch (IOException e) {
                log.warn("Failed to read raw settings file: {}", e.getMessage());
            }
        }
        return new SettingsDto(
                defaultBringEmail,
                defaultBringPassword,
                "",
                defaultBringAutoSync,
                defaultMealieUrl,
                defaultMealieToken,
                defaultKrogerClientId,
                defaultKrogerClientSecret,
                defaultKrogerLocationId,
                defaultAuthentikEnabled,
                defaultAuthentikIssuerUrl,
                defaultAuthentikClientId,
                defaultAuthentikClientSecret,
                defaultGoogleAuthEnabled,
                defaultGoogleClientId,
                defaultGoogleClientSecret,
                5,
                true,
                3,
                true,
                getAppVersion()
        );
    }

    public SettingsDto getSettings() {
        SettingsDto raw = getRawSettings();
        return new SettingsDto(
                raw.bringEmail(),
                raw.bringPassword() != null && !raw.bringPassword().isBlank() ? "********" : "",
                raw.bringListUuid(),
                raw.bringAutoSync(),
                raw.mealieBaseUrl(),
                raw.mealieApiToken() != null && !raw.mealieApiToken().isBlank() ? "********" : "",
                raw.krogerClientId(),
                raw.krogerClientSecret() != null && !raw.krogerClientSecret().isBlank() ? "********" : "",
                raw.krogerLocationId(),
                raw.authentikEnabled(),
                raw.authentikIssuerUrl(),
                raw.authentikClientId(),
                raw.authentikClientSecret() != null && !raw.authentikClientSecret().isBlank() ? "********" : "",
                raw.googleAuthEnabled(),
                raw.googleClientId(),
                raw.googleClientSecret() != null && !raw.googleClientSecret().isBlank() ? "********" : "",
                raw.freshFoodReminderDays() != null ? raw.freshFoodReminderDays() : 5,
                raw.freshFoodReminderEnabled() != null ? raw.freshFoodReminderEnabled() : true,
                raw.expirationReminderDays() != null ? raw.expirationReminderDays() : 3,
                raw.expirationReminderEnabled() != null ? raw.expirationReminderEnabled() : true,
                getAppVersion()
        );
    }

    public SettingsDto saveSettings(SettingsDto dto) {
        File file = getSettingsFile();
        file.getParentFile().mkdirs();

        // Preserve secrets from raw stored settings if masked
        SettingsDto current = getRawSettings();
        String bringPass = (dto.bringPassword() != null && dto.bringPassword().equals("********")) ? current.bringPassword() : dto.bringPassword();
        String mealieToken = (dto.mealieApiToken() != null && dto.mealieApiToken().equals("********")) ? current.mealieApiToken() : dto.mealieApiToken();
        String krogerSecret = (dto.krogerClientSecret() != null && dto.krogerClientSecret().equals("********")) ? current.krogerClientSecret() : dto.krogerClientSecret();
        String authentikSecret = (dto.authentikClientSecret() != null && dto.authentikClientSecret().equals("********")) ? current.authentikClientSecret() : dto.authentikClientSecret();
        String googleSecret = (dto.googleClientSecret() != null && dto.googleClientSecret().equals("********")) ? current.googleClientSecret() : dto.googleClientSecret();

        int freshDays = dto.freshFoodReminderDays() != null ? dto.freshFoodReminderDays() : (current.freshFoodReminderDays() != null ? current.freshFoodReminderDays() : 5);
        boolean freshEnabled = dto.freshFoodReminderEnabled() != null ? dto.freshFoodReminderEnabled() : (current.freshFoodReminderEnabled() != null ? current.freshFoodReminderEnabled() : true);
        int expDays = dto.expirationReminderDays() != null ? dto.expirationReminderDays() : (current.expirationReminderDays() != null ? current.expirationReminderDays() : 3);
        boolean expEnabled = dto.expirationReminderEnabled() != null ? dto.expirationReminderEnabled() : (current.expirationReminderEnabled() != null ? current.expirationReminderEnabled() : true);

        SettingsDto updated = new SettingsDto(
                dto.bringEmail(),
                bringPass,
                dto.bringListUuid(),
                dto.bringAutoSync(),
                dto.mealieBaseUrl(),
                mealieToken,
                dto.krogerClientId(),
                krogerSecret,
                dto.krogerLocationId(),
                dto.authentikEnabled(),
                dto.authentikIssuerUrl(),
                dto.authentikClientId(),
                authentikSecret,
                dto.googleAuthEnabled(),
                dto.googleClientId(),
                googleSecret,
                freshDays,
                freshEnabled,
                expDays,
                expEnabled,
                getAppVersion()
        );

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, updated);
            
            if (updated.bringEmail() != null && !updated.bringEmail().isBlank() && 
                updated.bringPassword() != null && !updated.bringPassword().equals("********")) {
                bringSyncService.authenticate(updated.bringEmail(), updated.bringPassword());
            }
            if (updated.bringListUuid() != null && !updated.bringListUuid().isBlank()) {
                bringSyncService.setDefaultListUuid(updated.bringListUuid());
            }
        } catch (IOException e) {
            log.error("Failed to save settings to file: {}", e.getMessage());
        }

        return getSettings();
    }

    public int getFreshFoodReminderDays() {
        Integer days = getRawSettings().freshFoodReminderDays();
        return days != null ? days : 5;
    }

    public boolean isFreshFoodReminderEnabled() {
        Boolean enabled = getRawSettings().freshFoodReminderEnabled();
        return enabled != null ? enabled : true;
    }

    public int getExpirationReminderDays() {
        Integer days = getRawSettings().expirationReminderDays();
        return days != null ? days : 3;
    }

    public boolean isExpirationReminderEnabled() {
        Boolean enabled = getRawSettings().expirationReminderEnabled();
        return enabled != null ? enabled : true;
    }
}

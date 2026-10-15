package com.skafferi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SettingsDto(
        // App Customization
        String appName,

        // Bring!
        String bringEmail,
        String bringPassword,
        String bringListUuid,
        boolean bringAutoSync,

        // Mealie
        String mealieBaseUrl,
        String mealieFrontendUrl,
        String mealieHousehold,
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

        // LLM Zero-Waste Chef
        String llmApiKey,
        String llmProvider,
        String llmModel,
        String llmBaseUrl,
        Boolean hasLlmApiKey,
        Boolean hasMealieConnection,

        // Application Metadata
        String appVersion
) {}

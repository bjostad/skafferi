package com.skafferi.service;

import com.skafferi.dto.SettingsDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SettingsServiceTest {

    private BringSyncService bringSyncService;
    private SettingsService settingsService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        bringSyncService = mock(BringSyncService.class);
        settingsService = new SettingsService(bringSyncService, null);
        ReflectionTestUtils.setField(settingsService, "dataDir", tempDir.toString());
        ReflectionTestUtils.setField(settingsService, "defaultAppName", "Skafferi");
    }

    @Test
    void testDefaultAppNameReturnsSkafferi() {
        assertEquals("Skafferi", settingsService.getAppName());
        SettingsDto settings = settingsService.getSettings();
        // appName field in getSettings should be empty or default
        assertTrue(settings.appName() == null || settings.appName().isEmpty() || "Skafferi".equals(settings.appName()));
    }

    @Test
    void testSaveAndRetrieveCustomAppName() {
        SettingsDto current = settingsService.getSettings();
        SettingsDto updated = new SettingsDto(
                "My Homestead Pantry",
                current.bringEmail(),
                current.bringPassword(),
                current.bringListUuid(),
                current.bringAutoSync(),
                current.mealieBaseUrl(),
                current.mealieApiToken(),
                current.mealieAutoDeduct(),
                current.krogerClientId(),
                current.krogerClientSecret(),
                current.krogerLocationId(),
                current.authentikEnabled(),
                current.authentikIssuerUrl(),
                current.authentikClientId(),
                current.authentikClientSecret(),
                current.googleAuthEnabled(),
                current.googleClientId(),
                current.googleClientSecret(),
                current.freshFoodReminderDays(),
                current.freshFoodReminderEnabled(),
                current.expirationReminderDays(),
                current.expirationReminderEnabled(),
                current.smtpHost(),
                current.smtpPort(),
                current.smtpUsername(),
                current.smtpPassword(),
                current.smtpEncryption(),
                current.smtpFromAddress(),
                current.smtpRecipients(),
                current.smtpEnabled(),
                current.smtpDailyDigestEnabled(),
                current.smtpWeeklyDigestEnabled(),
                current.llmApiKey(),
                current.llmProvider(),
                current.llmModel(),
                current.llmBaseUrl(),
                current.hasLlmApiKey(),
                current.hasMealieConnection(),
                current.appVersion()
        );

        SettingsDto saved = settingsService.saveSettings(updated);
        assertNotNull(saved);
        assertEquals("My Homestead Pantry", saved.appName());
        assertEquals("My Homestead Pantry", settingsService.getAppName());

        // Re-read settings
        SettingsDto reRead = settingsService.getSettings();
        assertEquals("My Homestead Pantry", reRead.appName());
        assertEquals("My Homestead Pantry", settingsService.getAppName());
    }

    @Test
    void testBlankAppNameFallsBackToDefault() {
        SettingsDto current = settingsService.getSettings();
        SettingsDto withBlank = new SettingsDto(
                "   ",
                current.bringEmail(),
                current.bringPassword(),
                current.bringListUuid(),
                current.bringAutoSync(),
                current.mealieBaseUrl(),
                current.mealieApiToken(),
                current.mealieAutoDeduct(),
                current.krogerClientId(),
                current.krogerClientSecret(),
                current.krogerLocationId(),
                current.authentikEnabled(),
                current.authentikIssuerUrl(),
                current.authentikClientId(),
                current.authentikClientSecret(),
                current.googleAuthEnabled(),
                current.googleClientId(),
                current.googleClientSecret(),
                current.freshFoodReminderDays(),
                current.freshFoodReminderEnabled(),
                current.expirationReminderDays(),
                current.expirationReminderEnabled(),
                current.smtpHost(),
                current.smtpPort(),
                current.smtpUsername(),
                current.smtpPassword(),
                current.smtpEncryption(),
                current.smtpFromAddress(),
                current.smtpRecipients(),
                current.smtpEnabled(),
                current.smtpDailyDigestEnabled(),
                current.smtpWeeklyDigestEnabled(),
                current.llmApiKey(),
                current.llmProvider(),
                current.llmModel(),
                current.llmBaseUrl(),
                current.hasLlmApiKey(),
                current.hasMealieConnection(),
                current.appVersion()
        );

        SettingsDto saved = settingsService.saveSettings(withBlank);
        assertEquals("", saved.appName());
        assertEquals("Skafferi", settingsService.getAppName());
    }
}

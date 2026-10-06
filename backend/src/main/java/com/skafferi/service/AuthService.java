package com.skafferi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skafferi.domain.User;
import com.skafferi.dto.SettingsDto;
import com.skafferi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SettingsService settingsService;
    private final UserRepository userRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AuthService(SettingsService settingsService, UserRepository userRepository) {
        this.settingsService = settingsService;
        this.userRepository = userRepository;
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> getAvailableProviders(String originUrl) {
        SettingsDto settings = settingsService.getSettings();
        Map<String, Object> providers = new HashMap<>();

        // Authentik (OIDC)
        if (settings.authentikEnabled() && settings.authentikIssuerUrl() != null && !settings.authentikIssuerUrl().isBlank()) {
            String issuer = settings.authentikIssuerUrl().replaceAll("/+$", "");
            String authEndpoint = issuer + "/application/o/authorize/";
            String redirectUri = originUrl + "/api/auth/callback/oidc";
            String scope = "openid profile email";
            String loginUrl = authEndpoint + "?client_id=" + settings.authentikClientId() +
                    "&response_type=code&scope=" + scope.replace(" ", "%20") +
                    "&redirect_uri=" + redirectUri;

            providers.put("authentik", Map.of(
                    "name", "Authentik",
                    "enabled", true,
                    "loginUrl", loginUrl,
                    "redirectUri", redirectUri
            ));
        }

        // Google OAuth2
        if (settings.googleAuthEnabled() && settings.googleClientId() != null && !settings.googleClientId().isBlank()) {
            String authEndpoint = "https://accounts.google.com/o/oauth2/v2/auth";
            String redirectUri = originUrl + "/api/auth/callback/google";
            String scope = "openid profile email";
            String loginUrl = authEndpoint + "?client_id=" + settings.googleClientId() +
                    "&response_type=code&scope=" + scope.replace(" ", "%20") +
                    "&redirect_uri=" + redirectUri;

            providers.put("google", Map.of(
                    "name", "Google",
                    "enabled", true,
                    "loginUrl", loginUrl,
                    "redirectUri", redirectUri
            ));
        }

        return providers;
    }

    @Transactional
    public User handleOidcCallback(String code, String redirectUri) {
        SettingsDto settings = settingsService.getSettings();
        if (!settings.authentikEnabled()) {
            throw new IllegalStateException("Authentik SSO is not enabled");
        }

        try {
            String issuer = settings.authentikIssuerUrl().replaceAll("/+$", "");
            String tokenEndpoint = issuer + "/application/o/token/";
            String userinfoEndpoint = issuer + "/application/o/userinfo/";

            // 1. Exchange code for access token
            MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
            tokenParams.add("grant_type", "authorization_code");
            tokenParams.add("code", code);
            tokenParams.add("client_id", settings.authentikClientId());
            tokenParams.add("client_secret", settings.authentikClientSecret());
            tokenParams.add("redirect_uri", redirectUri);

            String tokenResponse = restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(tokenParams)
                    .retrieve()
                    .body(String.class);

            JsonNode tokenJson = objectMapper.readTree(tokenResponse);
            String accessToken = tokenJson.hasNonNull("access_token") ? tokenJson.get("access_token").asText() : null;

            if (accessToken == null) {
                throw new IllegalStateException("Failed to retrieve access token from Authentik");
            }

            // 2. Fetch UserInfo
            String userinfoResponse = restClient.get()
                    .uri(userinfoEndpoint)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode userJson = objectMapper.readTree(userinfoResponse);
            String username = userJson.hasNonNull("preferred_username") ? userJson.get("preferred_username").asText() : userJson.get("sub").asText();
            String name = userJson.hasNonNull("name") ? userJson.get("name").asText() : username;
            String email = userJson.hasNonNull("email") ? userJson.get("email").asText() : null;

            return syncOrProvisionUser(username, name, email);

        } catch (Exception e) {
            log.error("Authentik OIDC callback failed: {}", e.getMessage());
            throw new RuntimeException("Authentik SSO error: " + e.getMessage(), e);
        }
    }

    @Transactional
    public User handleGoogleCallback(String code, String redirectUri) {
        SettingsDto settings = settingsService.getSettings();
        if (!settings.googleAuthEnabled()) {
            throw new IllegalStateException("Google OAuth is not enabled");
        }

        try {
            String tokenEndpoint = "https://oauth2.googleapis.com/token";
            String userinfoEndpoint = "https://www.googleapis.com/oauth2/v3/userinfo";

            // 1. Exchange code for token
            MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
            tokenParams.add("grant_type", "authorization_code");
            tokenParams.add("code", code);
            tokenParams.add("client_id", settings.googleClientId());
            tokenParams.add("client_secret", settings.googleClientSecret());
            tokenParams.add("redirect_uri", redirectUri);

            String tokenResponse = restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(tokenParams)
                    .retrieve()
                    .body(String.class);

            JsonNode tokenJson = objectMapper.readTree(tokenResponse);
            String accessToken = tokenJson.hasNonNull("access_token") ? tokenJson.get("access_token").asText() : null;

            // 2. Fetch Google Profile
            String userinfoResponse = restClient.get()
                    .uri(userinfoEndpoint)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode userJson = objectMapper.readTree(userinfoResponse);
            String email = userJson.hasNonNull("email") ? userJson.get("email").asText() : "";
            String name = userJson.hasNonNull("name") ? userJson.get("name").asText() : email;
            String username = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;

            return syncOrProvisionUser(username, name, email);

        } catch (Exception e) {
            log.error("Google OAuth callback failed: {}", e.getMessage());
            throw new RuntimeException("Google OAuth error: " + e.getMessage(), e);
        }
    }

    private User syncOrProvisionUser(String username, String displayName, String email) {
        Optional<User> existing = userRepository.findByUsernameIgnoreCase(username);
        if (existing.isPresent()) {
            User u = existing.get();
            if (email != null && (u.getEmail() == null || u.getEmail().isBlank())) {
                u.setEmail(email);
                userRepository.save(u);
            }
            return u;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setDisplayName(displayName);
        newUser.setEmail(email);
        newUser.setRole(userRepository.count() == 0 ? "ADMIN" : "MEMBER");
        newUser.setAvatarColor("#8B5CF6");
        return userRepository.save(newUser);
    }
}

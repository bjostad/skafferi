package com.skafferi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BringSyncService {

    private static final Logger log = LoggerFactory.getLogger(BringSyncService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${skafferi.integrations.bring.email:}")
    private String configuredEmail;

    @Value("${skafferi.integrations.bring.password:}")
    private String configuredPassword;

    @Value("${skafferi.integrations.bring.auto-sync:false}")
    private boolean autoSync;

    private String cachedToken;
    private String cachedUserUuid;
    private String defaultListUuid;

    public BringSyncService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.getbring.com/rest")
                .defaultHeader("X-BRING-API-KEY", "cof4Nc6D8saplXjE3h3HXqHH8m7VU2i1Gs0g85Sp")
                .defaultHeader("X-BRING-CLIENT", "android")
                .defaultHeader("X-BRING-APPLICATION", "bring")
                .defaultHeader("X-BRING-COUNTRY", "US")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public synchronized boolean authenticate(String email, String password) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("email", email);
            formData.add("password", password);

            String response = restClient.post()
                    .uri("/v2/bringauth")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                this.cachedToken = root.hasNonNull("access_token") ? root.get("access_token").asText() : null;
                this.cachedUserUuid = root.hasNonNull("uuid") ? root.get("uuid").asText() : null;
                
                // If bringListUUID is directly in auth response, capture it
                if (root.hasNonNull("bringListUUID")) {
                    this.defaultListUuid = root.get("bringListUUID").asText();
                }

                if (cachedToken != null && cachedUserUuid != null) {
                    if (this.defaultListUuid == null) {
                        loadDefaultList();
                    }
                    log.info("Successfully authenticated with Bring! for {}", email);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Failed to authenticate with Bring! API for {}: {}", email, e.getMessage());
        }
        return false;
    }

    public void setDefaultListUuid(String listUuid) {
        if (listUuid != null && !listUuid.isBlank()) {
            this.defaultListUuid = listUuid;
            log.info("Bring! target list explicitly set to: {}", listUuid);
        }
    }

    public String getDefaultListUuid() {
        return this.defaultListUuid;
    }

    public List<Map<String, String>> loadAllLists() {
        List<Map<String, String>> result = new ArrayList<>();
        if (cachedToken == null || cachedUserUuid == null) {
            return result;
        }

        try {
            String url = "/bringusers/" + cachedUserUuid + "/lists";
            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + cachedToken)
                    .header("X-BRING-USER-UUID", cachedUserUuid)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                if (root.has("lists") && root.get("lists").isArray()) {
                    for (JsonNode listNode : root.get("lists")) {
                        String uuid = listNode.get("listUuid").asText();
                        String name = listNode.hasNonNull("name") ? listNode.get("name").asText() : uuid;
                        result.add(Map.of(
                                "listUuid", uuid,
                                "name", name
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load Bring! lists: {}", e.getMessage());
        }
        return result;
    }

    private void loadDefaultList() {
        List<Map<String, String>> lists = loadAllLists();
        if (!lists.isEmpty()) {
            this.defaultListUuid = lists.get(0).get("listUuid");
            log.info("Bring! default list selected: {}", this.defaultListUuid);
        }
    }

    public boolean addItem(String itemName, String specification) {
        if (cachedToken == null || defaultListUuid == null) {
            log.warn("Bring! integration not authenticated or no list available (token: {}, list: {})", 
                    cachedToken != null, defaultListUuid);
            return false;
        }

        String spec = specification != null ? specification : "";

        // 1. Primary Bring! v2 items endpoint (changes array) - Used by current Bring! apps
        try {
            Map<String, Object> change = Map.of(
                    "accuracy", "0.0",
                    "altitude", "0.0",
                    "latitude", "0.0",
                    "longitude", "0.0",
                    "itemId", itemName,
                    "spec", spec,
                    "operation", "TO_PURCHASE"
            );
            Map<String, Object> payload = Map.of(
                    "changes", List.of(change),
                    "sender", ""
            );

            restClient.put()
                    .uri("/v2/bringlists/" + defaultListUuid + "/items")
                    .header("Authorization", "Bearer " + cachedToken)
                    .header("X-BRING-USER-UUID", cachedUserUuid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully pushed '{}' ({}) to Bring! list {} via v2 changes API", itemName, spec, defaultListUuid);
            return true;
        } catch (Exception e1) {
            log.warn("Bring v2 changes API failed: {}, trying form-urlencoded endpoint...", e1.getMessage());
        }

        // 2. Fallback to classic form-urlencoded PUT
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("purchase", itemName);
            form.add("recently", "");
            form.add("specification", spec);
            form.add("remove", "");
            form.add("sender", "null");

            restClient.put()
                    .uri("/v2/bringlists/" + defaultListUuid)
                    .header("Authorization", "Bearer " + cachedToken)
                    .header("X-BRING-USER-UUID", cachedUserUuid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully pushed '{}' ({}) to Bring! list {} via form-urlencoded fallback", itemName, spec, defaultListUuid);
            return true;
        } catch (Exception e2) {
            log.error("Failed to add item to Bring! via both methods: {}", e2.getMessage());
            return false;
        }
    }

    public List<Map<String, String>> getBringShoppingList() {
        List<Map<String, String>> items = new ArrayList<>();
        if (cachedToken == null || defaultListUuid == null) {
            log.warn("getBringShoppingList: Not authenticated or no default list UUID (token: {}, list: {})",
                    cachedToken != null, defaultListUuid);
            return items;
        }

        try {
            String response = restClient.get()
                    .uri("/v2/bringlists/" + defaultListUuid)
                    .header("Authorization", "Bearer " + cachedToken)
                    .header("X-BRING-USER-UUID", cachedUserUuid)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                log.debug("Bring get list response: {}", response);
                JsonNode root = objectMapper.readTree(response);
                
                // Support both root.items.purchase and root.purchase
                JsonNode purchaseNode = null;
                if (root.has("items") && root.get("items").has("purchase")) {
                    purchaseNode = root.get("items").get("purchase");
                } else if (root.has("purchase")) {
                    purchaseNode = root.get("purchase");
                }

                if (purchaseNode != null && purchaseNode.isArray()) {
                    for (JsonNode item : purchaseNode) {
                        String name = item.hasNonNull("name") ? item.get("name").asText() :
                                     (item.hasNonNull("itemId") ? item.get("itemId").asText() : "");
                        String spec = item.hasNonNull("specification") ? item.get("specification").asText() :
                                     (item.hasNonNull("spec") ? item.get("spec").asText() : "");
                        if (!name.isBlank()) {
                            items.add(Map.of(
                                    "name", name,
                                    "specification", spec
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch Bring! list: {}", e.getMessage());
        }
        return items;
    }

    public boolean isAutoSync() {
        return autoSync;
    }
}

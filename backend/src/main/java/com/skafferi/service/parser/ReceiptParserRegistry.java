package com.skafferi.service.parser;

import com.skafferi.dto.ReceiptParseResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReceiptParserRegistry {

    private final List<ReceiptParserStrategy> parsers;

    public ReceiptParserRegistry(List<ReceiptParserStrategy> parsers) {
        this.parsers = parsers;
    }

    public List<Map<String, String>> getAvailableProviders() {
        return parsers.stream()
                .map(p -> Map.of("id", p.getProviderId(), "name", p.getDisplayName()))
                .collect(Collectors.toList());
    }

    public ReceiptParseResult parse(String rawText, String providerId) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Receipt text cannot be empty");
        }

        // If specific provider requested and not "auto"
        if (providerId != null && !providerId.equalsIgnoreCase("auto") && !providerId.isBlank()) {
            Optional<ReceiptParserStrategy> specific = parsers.stream()
                    .filter(p -> p.getProviderId().equalsIgnoreCase(providerId))
                    .findFirst();
            if (specific.isPresent()) {
                return specific.get().parse(rawText);
            }
        }

        // Auto-detect based on canParse (Kroger checked before Generic fallback)
        for (ReceiptParserStrategy parser : parsers) {
            if (!parser.getProviderId().equals("generic") && parser.canParse(rawText)) {
                return parser.parse(rawText);
            }
        }

        // Fallback to generic parser
        return parsers.stream()
                .filter(p -> p.getProviderId().equals("generic"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No fallback receipt parser available"))
                .parse(rawText);
    }
}

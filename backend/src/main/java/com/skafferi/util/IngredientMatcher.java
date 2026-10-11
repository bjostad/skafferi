package com.skafferi.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Tiered Ingredient Matching Engine:
 * 1. Simple exact & substring containment
 * 2. Simple token overlap & prefix matching
 * 3. Fuzzy matching fallback (measurement/noise stripping, plural stemming, Levenshtein distance)
 *    executed ONLY when simple matches fail.
 */
public class IngredientMatcher {

    private static final Set<String> NOISE_WORDS = new HashSet<>(Arrays.asList(
            // Quantities, measurements & packaging units
            "cup", "cups", "tbsp", "tablespoon", "tablespoons", "tsp", "teaspoon", "teaspoons",
            "oz", "ounce", "ounces", "lb", "lbs", "pound", "pounds", "gram", "grams", "kg", "kilogram",
            "pinch", "pinches", "dash", "dashes", "clove", "cloves", "can", "cans", "bottle",
            "bottles", "package", "packages", "pkg", "pkgs", "slice", "slices", "piece", "pieces",
            "bunch", "bunches", "handful", "handfuls", "head", "heads", "stalk", "stalks", "stick", "sticks",
            // Culinary preparation & adjective noise
            "chopped", "diced", "minced", "sliced", "peeled", "grated", "shredded", "crushed",
            "melted", "softened", "frozen", "fresh", "freshly", "canned", "dry", "dried", "cooked",
            "raw", "warm", "cold", "hot", "ground", "extra", "virgin", "boneless", "skinless",
            "organic", "whole", "halved", "quartered", "pure", "natural", "taste", "optional",
            "large", "medium", "small", "ripe", "yellow", "white", "red", "green"
    ));

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\b(\\d+/\\d+|\\d+(\\.\\d+)?)\\b");

    /**
     * Executes tiered matching:
     * Returns true if a match is found through simple or fallback fuzzy matching.
     */
    public static boolean matches(String ingredient, String pantryItemName) {
        if (ingredient == null || pantryItemName == null) return false;

        String normIng = normalize(ingredient);
        String normStock = normalize(pantryItemName);
        if (normIng.isEmpty() || normStock.isEmpty()) return false;

        // TIER 1: Simple exact match & substring containment
        if (simpleExactOrContainmentMatch(normIng, normStock)) {
            return true;
        }

        // TIER 2: Simple token / prefix match
        if (simpleTokenMatch(normIng, normStock)) {
            return true;
        }

        // TIER 3: Fuzzy matching fallback (executed ONLY when simple matches fail)
        return fuzzyMatch(normIng, normStock);
    }

    private static boolean simpleExactOrContainmentMatch(String normIng, String normStock) {
        if (normIng.equals(normStock)) return true;
        return normIng.contains(normStock) || normStock.contains(normIng);
    }

    private static boolean simpleTokenMatch(String normIng, String normStock) {
        String[] ingWords = normIng.split(" ");
        String[] stockWords = normStock.split(" ");
        for (String iw : ingWords) {
            if (iw.length() < 3) continue;
            for (String sw : stockWords) {
                if (sw.length() < 3) continue;
                if (iw.equals(sw) || iw.startsWith(sw) || sw.startsWith(iw)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean fuzzyMatch(String normIng, String normStock) {
        String cleanIng = cleanNoiseAndStem(normIng);
        String cleanStock = cleanNoiseAndStem(normStock);

        if (cleanIng.isEmpty() || cleanStock.isEmpty()) return false;

        // Cleaned stems match or contain each other
        if (cleanIng.equals(cleanStock) || cleanIng.contains(cleanStock) || cleanStock.contains(cleanIng)) {
            return true;
        }

        String[] ingTokens = cleanIng.split(" ");
        String[] stockTokens = cleanStock.split(" ");

        for (String it : ingTokens) {
            if (it.length() < 3) continue;
            for (String st : stockTokens) {
                if (st.length() < 3) continue;
                if (it.equals(st)) return true;

                // Levenshtein similarity on tokens >= 4 characters
                if (it.length() >= 4 && st.length() >= 4) {
                    double sim = similarity(it, st);
                    if (sim >= 0.80) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String cleanNoiseAndStem(String text) {
        String noNums = NUMERIC_PATTERN.matcher(text).replaceAll("");
        String[] words = noNums.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            w = w.trim();
            if (w.isEmpty() || NOISE_WORDS.contains(w)) {
                continue;
            }
            String stemmed = stemPlural(w);
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(stemmed);
        }
        return sb.toString().trim();
    }

    public static String stemPlural(String word) {
        if (word.length() <= 3) return word;
        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("es") && (word.endsWith("shes") || word.endsWith("ches") || word.endsWith("xes") || word.endsWith("toes"))) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us") && !word.endsWith("is")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    public static double similarity(String s1, String s2) {
        int dist = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - ((double) dist / (double) maxLen);
    }

    public static int levenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(
                        1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1
                );
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }
}

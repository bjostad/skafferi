package com.skafferi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IngredientMatcherTest {

    @Test
    public void testTier1ExactAndSubstringMatches() {
        // Exact
        assertTrue(IngredientMatcher.matches("Garlic", "Garlic"));
        assertTrue(IngredientMatcher.matches("milk", "Milk"));

        // Substring
        assertTrue(IngredientMatcher.matches("Spinach", "Organic Baby Spinach"));
        assertTrue(IngredientMatcher.matches("Tillamook Sharp Cheddar Cheese", "Cheddar Cheese"));
    }

    @Test
    public void testTier2TokenAndPrefixMatches() {
        // Shared significant token
        assertTrue(IngredientMatcher.matches("Yellow Onions", "Onion"));
        assertTrue(IngredientMatcher.matches("Ground Black Pepper", "Black Pepper"));
        assertTrue(IngredientMatcher.matches("Boneless Chicken Breast", "Chicken"));
    }

    @Test
    public void testTier3FuzzyMatchingWithNoiseAndPluralStemming() {
        // Measurements & fractions stripped
        assertTrue(IngredientMatcher.matches("2 cups shredded sharp cheddar cheese", "Tillamook Sharp Cheddar"));
        assertTrue(IngredientMatcher.matches("3 cloves garlic, minced", "Organic Garlic"));
        assertTrue(IngredientMatcher.matches("1/2 tsp kosher salt", "Sea Salt"));

        // Plural stemming (tomatoes -> tomato, cherries -> cherry)
        assertTrue(IngredientMatcher.matches("fresh ripe tomatoes", "Tomato"));
        assertTrue(IngredientMatcher.matches("sweet cherries", "Cherry"));

        // Minor typo / edit distance
        assertTrue(IngredientMatcher.matches("parmigiano cheese", "Grated Parmesan"));
        assertTrue(IngredientMatcher.matches("mozerella cheese", "Fresh Mozzarella"));
    }

    @Test
    public void testNegativeMatches() {
        assertFalse(IngredientMatcher.matches("chicken breasts", "beef patties"));
        assertFalse(IngredientMatcher.matches("soy sauce", "olive oil"));
        assertFalse(IngredientMatcher.matches("flour", "sugar"));
    }
}

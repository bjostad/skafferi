package com.skafferi.config;

import com.skafferi.domain.Category;
import com.skafferi.domain.Location;
import com.skafferi.domain.User;
import com.skafferi.repository.CategoryRepository;
import com.skafferi.repository.LocationRepository;
import com.skafferi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(LocationRepository locationRepository, 
                                      CategoryRepository categoryRepository,
                                      UserRepository userRepository,
                                      org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE items ADD COLUMN perishable BOOLEAN DEFAULT 0");
            } catch (Exception ignored) {}
            try {
                jdbcTemplate.execute("ALTER TABLE items ADD COLUMN package_size TEXT");
            } catch (Exception ignored) {}
            try {
                jdbcTemplate.execute("ALTER TABLE items ADD COLUMN notes TEXT");
            } catch (Exception ignored) {}

            if (userRepository.count() == 0) {
                log.info("Seeding default household administrator user...");
                userRepository.save(new User(
                        "usr-default",
                        "admin",
                        "Family Admin",
                        "admin@skafferi.local",
                        "ADMIN",
                        "#9685ab"
                ));
            }

            if (locationRepository.count() == 0) {
                log.info("Seeding initial pantry locations...");
                locationRepository.saveAll(List.of(
                        new Location("loc-pantry", "Main Pantry", "Walk-in pantry shelving", "Archive", "PANTRY", 1),
                        new Location("loc-fridge", "Kitchen Fridge", "Main kitchen refrigerator", "Refrigerator", "FRIDGE", 2),
                        new Location("loc-freezer", "Upstairs Freezer", "Top fridge freezer unit", "Snowflake", "FREEZER", 3),
                        new Location("loc-deep-freezer", "Downstairs Deep Freezer", "Chest/deep freezer in basement or garage", "Snowflake", "FREEZER", 4),
                        new Location("loc-spices", "Spice Rack", "Counter spice organizer", "Flame", "SPICE", 5),
                        new Location("loc-counter", "Counter / Fruit Bowl", "Open kitchen counters", "LayoutGrid", "PANTRY", 6)
                ));
            }

            if (categoryRepository.count() == 0) {
                log.info("Seeding initial food categories...");
                categoryRepository.saveAll(List.of(
                        new Category("cat-produce", "Produce", "Apple", 1),
                        new Category("cat-dairy", "Dairy & Eggs", "Milk", 2),
                        new Category("cat-meat", "Meat & Seafood", "Beef", 3),
                        new Category("cat-bakery", "Bakery", "Croissant", 4),
                        new Category("cat-canned", "Canned & Jarred", "Package", 5),
                        new Category("cat-grains", "Dry Goods & Grains", "Wheat", 6),
                        new Category("cat-spices", "Spices & Seasonings", "Sparkles", 7),
                        new Category("cat-beverages", "Beverages", "CupSoda", 8),
                        new Category("cat-snacks", "Snacks", "Cookie", 9),
                        new Category("cat-frozen", "Frozen Foods", "IceCream", 10),
                        new Category("cat-household", "Household & Cleaning", "Home", 11)
                ));
            }
        };
    }
}

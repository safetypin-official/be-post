package com.safetypin.post.seeder;

import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.Arrays;


@Slf4j
@Configuration
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;

    public DevDataSeeder(PostRepository postRepository, CategoryRepository categoryRepository) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (!(postRepository.count() == 0 && categoryRepository.count() == 0)) {
            log.info("ℹ️ Data already exist. Skipping seeding.");
            return;
        }

        Category lostItem = new Category("Lost Item");
        Category infrastructureIssue = new Category("Infrastructure Issue");
        Category crimeWatch = new Category("Crime Watch");
        Category lostBook = new Category("Lost Book");

        categoryRepository.saveAllAndFlush(Arrays.asList(lostItem, infrastructureIssue, crimeWatch, lostBook));

        postRepository.saveAllAndFlush(Arrays.asList(
            new Post("dec4c589-3c98-4645-8511-c917cc92a527", "Lost wallet near the park",
                    lostItem, LocalDateTime.parse("2025-03-06T20:46:39.073832"),
                    37.7749, -122.4194),
            new Post("54e68daf-dc42-4a44-9505-7cf56f51ab39", "Street light not working",
                    infrastructureIssue, LocalDateTime.parse("2025-03-06T20:46:40.073832"),
                    40.7128, -74.006),
            new Post("74d472f8-9375-4cc5-a05f-58f7f5177703", "Suspicious activity spotted",
                    crimeWatch, LocalDateTime.parse("2025-03-06T20:46:42.073832"),
                    34.0522, -118.2437),
            new Post("0cf40ff4-08b3-4c32-bd71-7556f89687d3", "Lost wallet near Pacil",
                    lostItem, LocalDateTime.parse("2025-03-06T22:07:07.899287"),
                    -6.365898347375627, 106.8267300354285),
            new Post("3003b842-aa5e-4545-926e-a718ff64b280", "Book left at Gramedia Depok",
                    lostBook, LocalDateTime.parse("2025-03-06T22:07:08.899287"),
                    -6.370909783830162, 106.83398272852651)
        ));

        log.info("✅ Data seeded successfully.");
    }
}

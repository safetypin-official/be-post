package com.safetypin.post.seeder;

import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Slf4j
@Configuration
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {
    private final PostRepository postRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public DevDataSeeder(PostRepository postRepository) {
        this.postRepository = postRepository;
    }
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (postRepository.count() == 0) { // Prevent duplicate seeding
            createPost("dec4c589-3c98-4645-8511-c917cc92a527", "Lost wallet near the park", "Lost Item", "Alert", "2025-03-06T20:46:39.073832", 37.7749, -122.4194);
            createPost("54e68daf-dc42-4a44-9505-7cf56f51ab39", "Street light not working", "Infrastructure Issue", "Report", "2025-03-06T20:46:39.073832", 40.7128, -74.006);
            createPost("74d472f8-9375-4cc5-a05f-58f7f5177703", "Suspicious activity spotted", "Crime Watch", "Warning", "2025-03-06T20:46:39.073832", 34.0522, -118.2437);
            createPost("0cf40ff4-08b3-4c32-bd71-7556f89687d3", "Lost wallet near Pacil", "Lost Item", "Alert", "2025-03-06T22:07:07.899287", -6.365898347375627, 106.8267300354285);
            createPost("3003b842-aa5e-4545-926e-a718ff64b280", "Book left at Gramedia Depok", "Lost Book", "Lost & Found", "2025-03-06T22:07:07.899287", -6.370909783830162, 106.83398272852651);
            System.out.println("✅ Posts seeded successfully.");
        } else {
            System.out.println("ℹ️ Posts already exist. Skipping seeding.");
        }
    }
    @Transactional
    protected void createPost(String uuid, String title, String category, String type, String timestamp, double lat, double lon) {
//        Post newPost = new Post(UUID.fromString(uuid), title, category, type, LocalDateTime.parse(timestamp), geometryFactory.createPoint(new Coordinate(lon, lat)));
        Post newPost = new Post(title, category, type, LocalDateTime.parse(timestamp), lat, lon);
        postRepository.save(newPost);
    }
}

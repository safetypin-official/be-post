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
import java.util.List;


@Slf4j
@Configuration
@Profile({"dev", "staging"})
public class DevDataSeeder implements CommandLineRunner {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;

    public DevDataSeeder(PostRepository postRepository, CategoryRepository categoryRepository) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        // Seed categories first
        seedCategories();
        
        // Then seed posts
        seedPosts();
    }
    
    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            log.info("ℹ️ Categories already exist. Skipping category seeding.");
            return;
        }
        
        // Define categories
        List<Category> categories = Arrays.asList(
            new Category("Lost Item"),
            new Category("Infrastructure Issue"),
            new Category("Crime Watch"),
            new Category("Lost Book"),
            new Category("Lost Pet"),
            new Category("Service Issue"),
            new Category("Flooding"),
            new Category("Stolen Vehicle")
        );
        
        // Save all categories
        categoryRepository.saveAll(categories);
        log.info("✅ Database seeded with categories!");
    }
    
    private void seedPosts() {
        if (postRepository.count() > 0) {
            log.info("ℹ️ Posts already exist. Skipping post seeding.");
            return;
        }

        // Define categories as strings - these must match the names in the categories table
        String lostItem = "Lost Item";
        String infrastructureIssue = "Infrastructure Issue";
        String crimeWatch = "Crime Watch";
        String lostBook = "Lost Book";
        String lostPet = "Lost Pet";
        String serviceIssue = "Service Issue";
        String flooding = "Flooding";
        String stolenVehicle = "Stolen Vehicle";

        // Create and save posts
        postRepository.saveAll(Arrays.asList(
                // Adding the lost wallet and lost bag posts from the example response
                new Post("Dompet saya hilang di sekitar Margonda, tolong hubungi jika menemukan!", "Lost wallet near Margonda",
                        lostItem, LocalDateTime.parse("2025-03-06T20:46:39.073832"),
                        -6.381832, 106.832512),
                        
                new Post("Bagi yang menemukan tas warna hitam di Margo City, tolong hubungi saya. Ada dokumen penting di dalamnya.", "Lost bag at Margo City",
                        lostItem, LocalDateTime.parse("2025-03-06T22:07:07.899287"),
                        -6.369028, 106.832322),

                new Post("Buku catatan tertinggal di Perpustakaan UI lantai 2. Jika menemukannya, tolong dikembalikan ke resepsionis.", "Lost book at UI Library",
                        lostBook, LocalDateTime.parse("2025-03-06T22:07:08.899287"),
                        -6.360382, 106.827097),

                new Post("Ada jalan berlubang cukup besar di Jalan Raya Bogor dekat Cisalak. Pengendara motor harap hati-hati!", "Pothole near Cisalak",
                        infrastructureIssue, LocalDateTime.parse("2025-03-06T22:30:45.123456"),
                        -6.375742, 106.822001),

                new Post("Ada genangan air besar di flyover Kukusan akibat hujan deras tadi malam. Hati-hati yang melintas.", "Flooding at Kukusan flyover",
                        flooding, LocalDateTime.parse("2025-03-07T09:00:30.345678"),
                        -6.374209, 106.814934),

                new Post("ATM di ITC Depok error, sudah coba beberapa kali tapi kartu tidak bisa keluar. Hati-hati yang mau pakai!", "ATM issue at ITC Depok",
                        serviceIssue, LocalDateTime.parse("2025-03-07T10:20:45.789123"),
                        -6.383420, 106.822142)
        ));

        log.info("✅ Database seeded with sample posts!");
    }
}

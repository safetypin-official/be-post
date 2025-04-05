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
import java.util.UUID;


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

        // Create mock user UUIDs
        UUID user1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID user2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID user3 = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID user4 = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID user5 = UUID.fromString("55555555-5555-5555-5555-555555555555");

        // Define categories as strings - these must match the names in the categories table
        String lostItem = "Lost Item";
        String infrastructureIssue = "Infrastructure Issue";
        String crimeWatch = "Crime Watch";
        String lostBook = "Lost Book";
        String lostPet = "Lost Pet";
        String serviceIssue = "Service Issue";
        String flooding = "Flooding";
        String stolenVehicle = "Stolen Vehicle";

        // Create and save posts with associated user IDs
        List<Post> posts = Arrays.asList(
                // User 1 posts
                createPost("Dompet saya hilang di sekitar Margonda, tolong hubungi jika menemukan!", "Lost wallet near Margonda",
                        lostItem, LocalDateTime.parse("2025-03-06T20:46:39.073832"),
                        -6.381832, 106.832512, user1),

                createPost("Bagi yang menemukan tas warna hitam di Margo City, tolong hubungi saya. Ada dokumen penting di dalamnya.", "Lost bag at Margo City",
                        lostItem, LocalDateTime.parse("2025-03-06T22:07:07.899287"),
                        -6.369028, 106.832322, user1),

                // User 2 posts
                createPost("Buku catatan tertinggal di Perpustakaan UI lantai 2. Jika menemukannya, tolong dikembalikan ke resepsionis.", "Lost book at UI Library",
                        lostBook, LocalDateTime.parse("2025-03-06T22:07:08.899287"),
                        -6.360382, 106.827097, user2),

                createPost("Ada jalan berlubang cukup besar di Jalan Raya Bogor dekat Cisalak. Pengendara motor harap hati-hati!", "Pothole near Cisalak",
                        infrastructureIssue, LocalDateTime.parse("2025-03-06T22:30:45.123456"),
                        -6.375742, 106.822001, user2),

                // User 3 posts
                createPost("Ada genangan air besar di flyover Kukusan akibat hujan deras tadi malam. Hati-hati yang melintas.", "Flooding at Kukusan flyover",
                        flooding, LocalDateTime.parse("2025-03-07T09:00:30.345678"),
                        -6.374209, 106.814934, user3),

                createPost("ATM di ITC Depok error, sudah coba beberapa kali tapi kartu tidak bisa keluar. Hati-hati yang mau pakai!", "ATM issue at ITC Depok",
                        serviceIssue, LocalDateTime.parse("2025-03-07T10:20:45.789123"),
                        -6.383420, 106.822142, user3),

                // User 4 posts
                createPost("Kucing persia warna putih hilang di area Beji. Ada kalung merah dan lonceng. Jika menemukan tolong hubungi.", "Lost Persian cat in Beji",
                        lostPet, LocalDateTime.parse("2025-03-08T14:35:12.456789"),
                        -6.378521, 106.828736, user4),

                createPost("Waspada! Ada penjambretan di lampu merah Margonda dekat McDonald's sekitar jam 8 malam. Hati-hati bagi pengguna jalan.", "Robbery at Margonda traffic light",
                        crimeWatch, LocalDateTime.parse("2025-03-08T21:10:33.987654"),
                        -6.376123, 106.830456, user4),

                // User 5 posts
                createPost("Motor Honda Beat hitam hilang di parkiran Depok Town Square. Plat B 1234 ABC. Jika melihat, tolong laporkan ke security.", "Stolen motorcycle at Depok Town Square",
                        stolenVehicle, LocalDateTime.parse("2025-03-09T15:45:22.123456"),
                        -6.390215, 106.823478, user5),

                createPost("Lampu jalan di sepanjang Jalan Margonda mati sejak semalam. Sudah dilaporkan ke Pemkot tapi belum diperbaiki.", "Street lights out on Margonda Road",
                        infrastructureIssue, LocalDateTime.parse("2025-03-09T19:30:17.654321"),
                        -6.380975, 106.829854, user5),

                // Additional posts by various users
                createPost("Ditemukan kunci motor di depan Gramedia Margonda. Silakan hubungi saya untuk mengambilnya.", "Found motorcycle keys near Gramedia",
                        lostItem, LocalDateTime.parse("2025-03-10T10:15:42.123456"),
                        -6.382541, 106.830197, user1),

                createPost("Anjing golden retriever hilang di sekitar UI. Pakai kalung biru. Reward bagi yang menemukan.", "Lost golden retriever near UI",
                        lostPet, LocalDateTime.parse("2025-03-10T16:20:33.654321"),
                        -6.362145, 106.828954, user2),

                createPost("Waspada! Ada pencurian helmet di parkiran Mall Depok. Sudah terjadi beberapa kali minggu ini.", "Helmet theft at Depok Mall",
                        crimeWatch, LocalDateTime.parse("2025-03-11T13:40:21.789456"),
                        -6.385641, 106.826328, user3),

                createPost("Mobil Toyota Avanza silver hilang di parkiran Margo City. Plat B 5678 DEF. Ada stiker Universitas Indonesia di belakang.", "Stolen Avanza at Margo City",
                        stolenVehicle, LocalDateTime.parse("2025-03-11T18:55:09.321654"),
                        -6.369854, 106.831254, user4),

                createPost("Banjir setinggi lutut di perumahan Depok Maharaja. Warga kesulitan keluar rumah.", "Flooding in Depok Maharaja housing complex",
                        flooding, LocalDateTime.parse("2025-03-12T08:10:36.951357"),
                        -6.387541, 106.819632, user5)
        );

        // Save all posts
        postRepository.saveAll(posts);
        log.info("✅ Database seeded with {} sample posts!", posts.size());
    }

    // Helper method to create a Post with postedBy field
    private Post createPost(String caption, String title, String category, LocalDateTime createdAt,
                            Double latitude, Double longitude, UUID postedBy) {
        Post post = new Post(caption, title, category, createdAt, latitude, longitude);
        post.setPostedBy(postedBy);
        return post;
    }
}

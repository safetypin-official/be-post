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
        if (!(postRepository.count() == 0 && categoryRepository.count() == 0)) {
            log.info("ℹ️ Data already exist. Skipping seeding.");
            return;
        }

        Category lostItem = new Category("Lost Item");
        Category infrastructureIssue = new Category("Infrastructure Issue");
        Category crimeWatch = new Category("Crime Watch");
        Category lostBook = new Category("Lost Book");
        Category lostPet = new Category("Lost Pet");
        Category serviceIssue = new Category("Service Issue");
        Category flooding = new Category("Flooding");
        Category stolenVehicle = new Category("Stolen Vehicle");

        categoryRepository.saveAllAndFlush(Arrays.asList(
                lostItem,
                infrastructureIssue,
                crimeWatch,
                lostBook,
                lostPet,
                serviceIssue,
                flooding,
                stolenVehicle
        ));

        postRepository.saveAllAndFlush(Arrays.asList(
                new Post("Dompet saya hilang di sekitar Margonda, tolong hubungi jika menemukan!", "Lost wallet near Margonda",
                        lostItem, LocalDateTime.parse("2025-03-06T20:46:39.073832"),
                        -6.381832, 106.832512),

                new Post("Lampu jalan di Jalan Juanda mati, jadi gelap banget! Mohon segera diperbaiki.", "Street light not working",
                        infrastructureIssue, LocalDateTime.parse("2025-03-06T20:46:40.073832"),
                        -6.366356, 106.834091),

                new Post("Tadi di sekitar UI ada orang mencurigakan bolak-balik sambil lihat-lihat ke kendaraan parkir. Hati-hati!", "Suspicious activity near UI",
                        crimeWatch, LocalDateTime.parse("2025-03-06T20:46:42.073832"),
                        -6.368326, 106.827390),

                new Post("Bagi yang menemukan tas warna hitam di Margo City, tolong hubungi saya. Ada dokumen penting di dalamnya.", "Lost bag at Margo City",
                        lostItem, LocalDateTime.parse("2025-03-06T22:07:07.899287"),
                        -6.369028, 106.832322),

                new Post("Buku catatan tertinggal di Perpustakaan UI lantai 2. Jika menemukannya, tolong dikembalikan ke resepsionis.", "Lost book at UI Library",
                        lostBook, LocalDateTime.parse("2025-03-06T22:07:08.899287"),
                        -6.360382, 106.827097),

                new Post("Ada jalan berlubang cukup besar di Jalan Raya Bogor dekat Cisalak. Pengendara motor harap hati-hati!", "Pothole near Cisalak",
                        infrastructureIssue, LocalDateTime.parse("2025-03-06T22:30:45.123456"),
                        -6.382915, 106.853648),

                new Post("Sepeda motor saya dicuri di parkiran Stasiun Depok Baru siang tadi. Ada yang melihat kejadian ini?", "Motor stolen at Depok Baru Station",
                        crimeWatch, LocalDateTime.parse("2025-03-06T23:10:15.456789"),
                        -6.391740, 106.831705),

                new Post("Saya kehilangan kucing peliharaan di sekitar Beji. Dia berbulu oranye dan memakai kalung merah.", "Lost pet in Beji",
                        lostPet, LocalDateTime.parse("2025-03-07T08:15:20.678912"),
                        -6.375742, 106.822001),

                new Post("Ada genangan air besar di flyover Kukusan akibat hujan deras tadi malam. Hati-hati yang melintas.", "Flooding at Kukusan flyover",
                        infrastructureIssue, LocalDateTime.parse("2025-03-07T09:00:30.345678"),
                        -6.374209, 106.814934),

                new Post("ATM di ITC Depok error, sudah coba beberapa kali tapi kartu tidak bisa keluar. Hati-hati yang mau pakai!", "ATM issue at ITC Depok",
                        serviceIssue, LocalDateTime.parse("2025-03-07T10:20:45.789123"),
                        -6.402345, 106.818943)
        ));

        log.info("✅ Data seeded successfully.");
    }
}

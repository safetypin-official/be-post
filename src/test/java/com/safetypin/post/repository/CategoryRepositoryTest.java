package com.safetypin.post.repository;

import com.safetypin.post.model.Category;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private static final Category
        category1 = new Category("Lost Item"),
        category2 = new Category("Infrastructure Issue"),
        category3 = new Category("Crime Watch");

    @BeforeEach
    void resetRepository() {
        categoryRepository.deleteAll();
        categoryRepository.saveAllAndFlush(Arrays.asList(category1, category2, category3));
    }

    @Test
    void testFindByName() {
        Category result = categoryRepository.findByName("Lost Item");
        assertEquals(category1, result);
    }

    @Test
    void testSaveCategory () {
        // save category then test if it was saved and generated an id
        Category category = new Category("Lost Book");
        categoryRepository.save(category);
        assertNotNull(category.getId());
        assertEquals(category, categoryRepository.findByName("Test Category"));
    }
}

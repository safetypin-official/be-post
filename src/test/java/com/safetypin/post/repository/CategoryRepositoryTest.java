package com.safetypin.post.repository;

import com.safetypin.post.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private static Category category1, category2, category3;

    @BeforeEach
    void resetRepository() {
        categoryRepository.deleteAll();

        category1 = new Category("Lost Item");
        category2 = new Category("Infrastructure Issue");
        category3 = new Category("Crime Watch");

        categoryRepository.saveAllAndFlush(Arrays.asList(category1, category2, category3));
    }

    @Test
    void testFindByName() {
        Category result = categoryRepository.findByName("Lost Item");
        assertEquals(category1.getId(), result.getId());
        assertEquals(category1.getName(), result.getName());
    }

    @Test
    void testSaveCategory () {
        // save category then test if it was saved and generated an id
        Category category = new Category("Lost Book");
        categoryRepository.save(category);
        assertNotNull(category.getId());

        Category result = categoryRepository.findByName("Lost Book");
        assertEquals(category.getId(), result.getId());
    }

    @Test
    void testFindAll () {
        List<Category> result = categoryRepository.findAll();
        assertEquals(3, result.size());
        assertTrue(result.contains(category1));
        assertTrue(result.contains(category2));
        assertTrue(result.contains(category3));
    }

    @Test
    void testDeleteCategory () {
        categoryRepository.delete(category1);

        List<Category> remainingCategories = categoryRepository.findAll();
        assertEquals(2, remainingCategories.size());
        assertFalse(remainingCategories.contains(category1));
    }


}

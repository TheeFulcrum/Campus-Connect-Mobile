package com.example.campusconnectmobile;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class FeedCategoryMapperTest {

    @Test
    public void servicesPreferenceIncludesSpecificListingTypes() {
        Set<String> categories = new HashSet<>(Arrays.asList(FeedCategoryMapper.expand(new String[]{"Services"})));
        assertTrue(categories.contains("services"));
        assertTrue(categories.contains("tutoring"));
        assertTrue(categories.contains("beauty"));
        assertTrue(categories.contains("creative"));
    }

    @Test
    public void goodsPreferenceIncludesSpecificListingTypes() {
        Set<String> categories = new HashSet<>(Arrays.asList(FeedCategoryMapper.expand(new String[]{"Goods & Textbooks"})));
        assertTrue(categories.contains("electronics"));
        assertTrue(categories.contains("books"));
        assertTrue(categories.contains("furniture"));
    }
}
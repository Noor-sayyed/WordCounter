package com.wordcounter.integration;

import com.wordcounter.DefaultTranslator;
import com.wordcounter.WordCounter;
import com.wordcounter.WordCounterImpl;
import com.wordcounter.exception.InvalidWordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class WordCounterIntegrationTest {

    private WordCounter wordCounter;
    private DefaultTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new DefaultTranslator();
        wordCounter = WordCounterImpl.builder()
                .withTranslator(translator)
                .build();
    }

    @Test
    @DisplayName("Should handle real translation workflow")
    void testRealTranslationWorkflow() throws InvalidWordException {
        // Add words in different languages that should be counted as the same
        wordCounter.addWord("flower");  // English
        wordCounter.addWord("flor");    // Spanish
        wordCounter.addWord("blume");   // German
        wordCounter.addWord("fiore");   // Italian

        // All should be counted as "flower"
        assertEquals(4, wordCounter.getCount("flower"));
        assertEquals(4, wordCounter.getCount("flor"));
        assertEquals(4, wordCounter.getCount("blume"));
        assertEquals(4, wordCounter.getCount("fiore"));

        assertEquals(4, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle mixed translated and non-translated words")
    void testMixedTranslatedNonTranslatedWords() throws InvalidWordException {
        // Add translated words
        wordCounter.addWord("house");   // English
        wordCounter.addWord("casa");    // Spanish
        wordCounter.addWord("haus");    // German

        // Add non-translated word
        wordCounter.addWord("computer"); // No translation available

        assertEquals(3, wordCounter.getCount("house"));
        assertEquals(3, wordCounter.getCount("casa"));
        assertEquals(3, wordCounter.getCount("haus"));

        assertEquals(1, wordCounter.getCount("computer"));

        assertEquals(4, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle batch operations with translations")
    void testBatchOperationsWithTranslations() throws InvalidWordException {
        wordCounter.addWords(
                "dog", "perro", "hund", "chien",  // All translate to "dog"
                "cat", "gato", "katze", "chat"    // All translate to "cat"
        );

        assertEquals(4, wordCounter.getCount("dog"));
        assertEquals(4, wordCounter.getCount("cat"));
        assertEquals(8, wordCounter.getTotalWords());

        // Verify individual language counts
        assertEquals(4, wordCounter.getCount("perro"));
        assertEquals(4, wordCounter.getCount("hund"));
        assertEquals(4, wordCounter.getCount("gato"));
        assertEquals(4, wordCounter.getCount("katze"));
    }

    @Test
    @DisplayName("Should maintain consistency under concurrent access")
    void testConcurrentAccessWithTranslations() throws InterruptedException {
        int threadCount = 5;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Each thread adds the same words in different languages
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        wordCounter.addWords("water", "agua", "wasser", "eau");
                    }
                } catch (InvalidWordException e) {
                    fail("No exception should occur: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // All words should be counted as "water"
        int expectedCount = threadCount * operationsPerThread * 4; // 4 words per operation
        assertEquals(expectedCount, wordCounter.getCount("water"));
        assertEquals(expectedCount, wordCounter.getCount("agua"));
        assertEquals(expectedCount, wordCounter.getCount("wasser"));
        assertEquals(expectedCount, wordCounter.getCount("eau"));
        assertEquals(expectedCount, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle case insensitivity with translations")
    void testCaseInsensitivityWithTranslations() throws InvalidWordException {
        wordCounter.addWord("FLOWER");
        wordCounter.addWord("flower");
        wordCounter.addWord("Flor");
        wordCounter.addWord("FLOR");
        wordCounter.addWord("Blume");
        wordCounter.addWord("BLUME");

        // All should be counted as the same word
        assertEquals(6, wordCounter.getCount("flower"));
        assertEquals(6, wordCounter.getCount("FLOWER"));
        assertEquals(6, wordCounter.getCount("flor"));
        assertEquals(6, wordCounter.getCount("FLOR"));
        assertEquals(6, wordCounter.getCount("blume"));
        assertEquals(6, wordCounter.getCount("BLUME"));
    }

    @Test
    @DisplayName("Should handle reset with translated words")
    void testResetWithTranslatedWords() throws InvalidWordException {
        // Add some translated words
        wordCounter.addWords("house", "casa", "haus", "maison");
        assertEquals(4, wordCounter.getCount("house"));
        assertEquals(4, wordCounter.getTotalWords());

        // Reset
        wordCounter.reset();

        // All counts should be zero
        assertEquals(0, wordCounter.getCount("house"));
        assertEquals(0, wordCounter.getCount("casa"));
        assertEquals(0, wordCounter.getCount("haus"));
        assertEquals(0, wordCounter.getCount("maison"));
        assertEquals(0, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle non-existent translations gracefully")
    void testNonExistentTranslations() throws InvalidWordException {
        // Add words that don't have translations
        wordCounter.addWord("programming");
        wordCounter.addWord("java");
        wordCounter.addWord("computer");

        // These should be counted separately
        assertEquals(1, wordCounter.getCount("programming"));
        assertEquals(1, wordCounter.getCount("java"));
        assertEquals(1, wordCounter.getCount("computer"));
        assertEquals(3, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle empty counter queries")
    void testEmptyCounterQueries() {
        // Empty counter should return 0 for any query
        assertEquals(0, wordCounter.getCount("anything"));
        assertEquals(0, wordCounter.getCount("flower"));
        assertEquals(0, wordCounter.getCount("flor"));
        assertEquals(0, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should verify translator functionality")
    void testTranslatorFunctionality() {
        // Test direct translator functionality
        assertEquals("flower", translator.translate("flor"));
        assertEquals("flower", translator.translate("blume"));
        assertEquals("flower", translator.translate("fiore"));
        assertEquals("flower", translator.translate("fleur"));

        assertEquals("house", translator.translate("casa"));
        assertEquals("house", translator.translate("haus"));
        assertEquals("house", translator.translate("maison"));

        // Non-translated words should return themselves
        assertEquals("computer", translator.translate("computer"));
        assertEquals("programming", translator.translate("programming"));

        // Test translation availability
        assertTrue(translator.isTranslationAvailable("flor"));
        assertTrue(translator.isTranslationAvailable("casa"));
        assertFalse(translator.isTranslationAvailable("computer"));
        assertFalse(translator.isTranslationAvailable("nonexistent"));
    }

    @Test
    @DisplayName("Should handle whitespace with translations")
    void testWhitespaceWithTranslations() throws InvalidWordException {
        // Add words with whitespace that should be normalized
        wordCounter.addWord(" flower ");
        wordCounter.addWord("  FLOR  ");
        wordCounter.addWord("\tblume\n");

        assertEquals(3, wordCounter.getCount("flower"));
        assertEquals(3, wordCounter.getCount("flor"));
        assertEquals(3, wordCounter.getCount("blume"));
        assertEquals(3, wordCounter.getTotalWords());
    }
}
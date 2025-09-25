package com.wordcounter;

import com.wordcounter.exception.InvalidWordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WordCounterTest {

    private WordCounter wordCounter;

    @Mock
    private Translator mockTranslator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        wordCounter = WordCounterImpl.builder()
                .withTranslator(mockTranslator)
                .build();
    }

    @Test
    @DisplayName("Should add single word successfully")
    void testAddSingleWord() throws InvalidWordException {
        // Given
        when(mockTranslator.translate("hello")).thenReturn("hello");

        // When
        wordCounter.addWord("hello");

        // Then
        assertEquals(1, wordCounter.getCount("hello"));
        assertEquals(1, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle multiple same words")
    void testAddMultipleSameWords() throws InvalidWordException {
        // Given
        when(mockTranslator.translate("hello")).thenReturn("hello");

        // When
        wordCounter.addWord("hello");
        wordCounter.addWord("hello");
        wordCounter.addWord("hello");

        // Then
        assertEquals(3, wordCounter.getCount("hello"));
        assertEquals(3, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle translated words as same")
    void testTranslatedWordsCountedAsSame() throws InvalidWordException {
        // Given
        when(mockTranslator.translate("flower")).thenReturn("flower");
        when(mockTranslator.translate("flor")).thenReturn("flower");
        when(mockTranslator.translate("blume")).thenReturn("flower");

        // When
        wordCounter.addWord("flower");
        wordCounter.addWord("flor");
        wordCounter.addWord("blume");

        // Then
        assertEquals(3, wordCounter.getCount("flower"));
        assertEquals(3, wordCounter.getCount("flor"));
        assertEquals(3, wordCounter.getCount("blume"));
        assertEquals(3, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should reject words with non-alphabetic characters")
    void testRejectNonAlphabeticWords() {
        // Test cases for invalid words
        String[] invalidWords = {"hello123", "hello!", "hello world", "hello-world", "", "  "};

        for (String invalidWord : invalidWords) {
            InvalidWordException exception = assertThrows(
                    InvalidWordException.class,
                    () -> wordCounter.addWord(invalidWord),
                    "Should reject word: '" + invalidWord + "'"
            );
            assertEquals(invalidWord, exception.getInvalidWord());
        }

        // Verify no words were added
        assertEquals(0, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle null word gracefully")
    void testNullWordHandling() {
        InvalidWordException exception = assertThrows(
                InvalidWordException.class,
                () -> wordCounter.addWord(null)
        );
        assertNull(exception.getInvalidWord());
        assertTrue(exception.getMessage().contains("null"));
    }

    @Test
    @DisplayName("Should add multiple words via list")
    void testAddWordsList() throws InvalidWordException {
        // Given
        List<String> words = Arrays.asList("apple", "banana", "apple");
        when(mockTranslator.translate("apple")).thenReturn("apple");
        when(mockTranslator.translate("banana")).thenReturn("banana");

        // When
        wordCounter.addWords(words);

        // Then
        assertEquals(2, wordCounter.getCount("apple"));
        assertEquals(1, wordCounter.getCount("banana"));
        assertEquals(3, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should add multiple words via varargs")
    void testAddWordsVarargs() throws InvalidWordException {
        // Given
        when(mockTranslator.translate("cat")).thenReturn("cat");
        when(mockTranslator.translate("dog")).thenReturn("dog");

        // When
        wordCounter.addWords("cat", "dog", "cat");

        // Then
        assertEquals(2, wordCounter.getCount("cat"));
        assertEquals(1, wordCounter.getCount("dog"));
        assertEquals(3, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle null list gracefully")
    void testNullListHandling() {
        List<String> nullList = null;
        InvalidWordException exception = assertThrows(
                InvalidWordException.class,
                () -> wordCounter.addWords(nullList)
        );
        assertTrue(exception.getMessage().contains("null"));
    }

    @Test
    @DisplayName("Should handle null varargs gracefully")
    void testNullVarargsHandling() {
        String[] nullArray = null;
        InvalidWordException exception = assertThrows(
                InvalidWordException.class,
                () -> wordCounter.addWords(nullArray)
        );
        assertTrue(exception.getMessage().contains("null"));
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    void testConcurrentAccess() throws InterruptedException, InvalidWordException {
        // Given
        when(mockTranslator.translate("concurrent")).thenReturn("concurrent");
        int threadCount = 10;
        int wordsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger exceptions = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < wordsPerThread; j++) {
                        wordCounter.addWord("concurrent");
                    }
                } catch (InvalidWordException e) {
                    exceptions.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        assertEquals(0, exceptions.get(), "No exceptions should occur");
        assertEquals(threadCount * wordsPerThread, wordCounter.getCount("concurrent"));
        assertEquals(threadCount * wordsPerThread, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should reset counter correctly")
    void testResetCounter() throws InvalidWordException {
        // Given
        when(mockTranslator.translate("test")).thenReturn("test");
        wordCounter.addWord("test");
        wordCounter.addWord("test");

        // When
        wordCounter.reset();

        // Then
        assertEquals(0, wordCounter.getCount("test"));
        assertEquals(0, wordCounter.getTotalWords());
    }

    @Test
    @DisplayName("Should handle case insensitive words")
    void testCaseInsensitiveWords() throws InvalidWordException {
        // Given
        when(mockTranslator.translate("hello")).thenReturn("hello");

        // When
        wordCounter.addWord("Hello");
        wordCounter.addWord("HELLO");
        wordCounter.addWord("hello");

        // Then
        assertEquals(3, wordCounter.getCount("hello"));
        assertEquals(3, wordCounter.getCount("Hello"));
        assertEquals(3, wordCounter.getCount("HELLO"));
    }

    @Test
    @DisplayName("Should handle empty string word")
    void testEmptyStringWord() {
        InvalidWordException exception = assertThrows(
                InvalidWordException.class,
                () -> wordCounter.addWord("")
        );
        assertEquals("", exception.getInvalidWord());
    }

    @Test
    @DisplayName("Should handle whitespace-only word")
    void testWhitespaceOnlyWord() {
        InvalidWordException exception = assertThrows(
                InvalidWordException.class,
                () -> wordCounter.addWord("   ")
        );
        assertEquals("   ", exception.getInvalidWord());
    }

    @Test
    @DisplayName("Should return zero for non-existent word")
    void testNonExistentWord() {
        when(mockTranslator.translate("nonexistent")).thenReturn("nonexistent");
        assertEquals(0, wordCounter.getCount("nonexistent"));
    }

    @Test
    @DisplayName("Should return zero for null word query")
    void testNullWordQuery() {
        assertEquals(0, wordCounter.getCount(null));
    }

    @Test
    @DisplayName("Should handle mixed valid and invalid words in batch")
    void testMixedValidInvalidWords() throws InvalidWordException {
        // Given
        when(mockTranslator.translate("valid")).thenReturn("valid");
        wordCounter.addWord("valid");

        // When/Then - should fail on first invalid word
        InvalidWordException exception = assertThrows(
                InvalidWordException.class,
                () -> wordCounter.addWords("valid", "invalid123", "anothervali")
        );

        // Should still have the first valid word
        assertEquals(1, wordCounter.getCount("valid"));
        assertEquals("invalid123", exception.getInvalidWord());
    }
}
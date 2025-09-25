package com.wordcounter;

import com.wordcounter.exception.InvalidWordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;


public class WordValidatorTest {

    @Test
    @DisplayName("Should validate correct alphabetic words")
    void testValidAlphabeticWords() {
        // These should not throw exceptions
        assertDoesNotThrow(() -> WordValidator.validateWord("hello"));
        assertDoesNotThrow(() -> WordValidator.validateWord("HELLO"));
        assertDoesNotThrow(() -> WordValidator.validateWord("Hello"));
        assertDoesNotThrow(() -> WordValidator.validateWord("word"));
        assertDoesNotThrow(() -> WordValidator.validateWord("test"));
        assertDoesNotThrow(() -> WordValidator.validateWord("a"));
        assertDoesNotThrow(() -> WordValidator.validateWord("A"));
    }

    @Test
    @DisplayName("Should reject words with numbers")
    void testRejectWordsWithNumbers() {
        InvalidWordException exception = assertThrows(
                InvalidWordException.class,
                () -> WordValidator.validateWord("hello123")
        );
        assertTrue(exception.getMessage().contains("non-alphabetic"));
        assertEquals("hello123", exception.getInvalidWord());
    }

    @Test
    @DisplayName("Should reject words with special characters")
    void testRejectWordsWithSpecialCharacters() {
        String[] invalidWords = {"hello!", "hello@", "hello#", "hello$", "hello%",
                "hello^", "hello&", "hello*", "hello(", "hello)"};

        for (String invalidWord : invalidWords) {
            InvalidWordException exception = assertThrows(
                    InvalidWordException.class,
                    () -> WordValidator.validateWord(invalidWord),
                    "Should reject word with special character: " + invalidWord
            );
            assertTrue(exception.getMessage().contains("non-alphabetic"));
            assertEquals(invalidWord, exception.getInvalidWord());
        }
    }

    @Test
    @DisplayName("Should reject words with spaces")
    void testRejectWordsWithSpaces() {
        String[] invalidWords = {"hello world", " hello", "hello ", " hello "};

        for (String invalidWord : invalidWords) {
            InvalidWordException exception = assertThrows(
                    InvalidWordException.class,
                    () -> WordValidator.validateWord(invalidWord),
                    "Should reject word with spaces: '" + invalidWord + "'"
            );
            assertEquals(invalidWord, exception.getInvalidWord());
        }
    }

    @Test
    @DisplayName("Should reject null words")
    void testRejectNullWords() {
        InvalidWordException exception = assertThrows(
                InvalidWordException.class,
                () -> WordValidator.validateWord(null)
        );
        assertTrue(exception.getMessage().contains("null"));
        assertNull(exception.getInvalidWord());
    }

    @Test
    @DisplayName("Should reject empty words")
    void testRejectEmptyWords() {
        InvalidWordException exception = assertThrows(
                InvalidWordException.class,
                () -> WordValidator.validateWord("")
        );
        assertTrue(exception.getMessage().contains("empty"));
        assertEquals("", exception.getInvalidWord());
    }

    @Test
    @DisplayName("Should reject whitespace-only words")
    void testRejectWhitespaceOnlyWords() {
        String[] whitespaceWords = {" ", "  ", "   ", "\t", "\n", " \t "};

        for (String whitespaceWord : whitespaceWords) {
            InvalidWordException exception = assertThrows(
                    InvalidWordException.class,
                    () -> WordValidator.validateWord(whitespaceWord),
                    "Should reject whitespace-only word: '" + whitespaceWord + "'"
            );
            assertEquals(whitespaceWord, exception.getInvalidWord());
        }
    }

    @Test
    @DisplayName("Should normalize words correctly")
    void testNormalizeWord() {
        assertEquals("hello", WordValidator.normalizeWord("hello"));
        assertEquals("hello", WordValidator.normalizeWord("HELLO"));
        assertEquals("hello", WordValidator.normalizeWord("Hello"));
        assertEquals("hello", WordValidator.normalizeWord(" hello "));
        assertEquals("hello", WordValidator.normalizeWord("  HELLO  "));
        assertEquals("test", WordValidator.normalizeWord("\tTEST\n"));
    }

    @Test
    @DisplayName("Should handle null in normalization")
    void testNormalizeNullWord() {
        assertNull(WordValidator.normalizeWord(null));
    }

    @Test
    @DisplayName("Should normalize empty string")
    void testNormalizeEmptyString() {
        assertEquals("", WordValidator.normalizeWord(""));
        assertEquals("", WordValidator.normalizeWord("   "));
    }

    @Test
    @DisplayName("Should validate words with leading/trailing whitespace after trimming")
    void testValidateWordsWithWhitespace() throws InvalidWordException {
        // These should be valid after trimming
        assertDoesNotThrow(() -> WordValidator.validateWord(" hello "));
        assertDoesNotThrow(() -> WordValidator.validateWord("  WORLD  "));
        assertDoesNotThrow(() -> WordValidator.validateWord("\ttest\n"));
    }

    @Test
    @DisplayName("Should reject words with hyphens and underscores")
    void testRejectWordsWithHyphensAndUnderscores() {
        String[] invalidWords = {"hello-world", "hello_world", "test-case", "snake_case"};

        for (String invalidWord : invalidWords) {
            InvalidWordException exception = assertThrows(
                    InvalidWordException.class,
                    () -> WordValidator.validateWord(invalidWord),
                    "Should reject word: " + invalidWord
            );
            assertEquals(invalidWord, exception.getInvalidWord());
        }
    }

    @Test
    @DisplayName("Should reject words with dots and commas")
    void testRejectWordsWithDotsAndCommas() {
        String[] invalidWords = {"hello.world", "hello,world", "test.", ".test", "hello..world"};

        for (String invalidWord : invalidWords) {
            InvalidWordException exception = assertThrows(
                    InvalidWordException.class,
                    () -> WordValidator.validateWord(invalidWord),
                    "Should reject word: " + invalidWord
            );
            assertEquals(invalidWord, exception.getInvalidWord());
        }
    }
}
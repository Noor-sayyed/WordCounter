package com.wordcounter;
import com.wordcounter.exception.InvalidWordException;
import java.util.regex.Pattern;


public class WordValidator {

    private static final Pattern ALPHABETIC_PATTERN = Pattern.compile("^[a-zA-Z]+$");


    public static void validateWord(String word) throws InvalidWordException {
        if (word == null || word.trim().isEmpty()) {
            throw new InvalidWordException("Word cannot be null or empty", word);
        }

        String trimmedWord = word.trim();
        if (!ALPHABETIC_PATTERN.matcher(trimmedWord).matches()) {
            throw new InvalidWordException(
                    "Word contains non-alphabetic characters: " + trimmedWord,
                    trimmedWord
            );
        }
    }


    public static String normalizeWord(String word) {
        if (word == null) {
            return null;
        }
        return word.trim().toLowerCase();
    }
}
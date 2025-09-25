package com.wordcounter;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


public class DefaultTranslator implements Translator {

    private final Map<String, String> translationCache;
    private final ObjectMapper objectMapper;
    private final List<String> supportedLanguages;
    private final Map<String, String> staticTranslations; // Fallback for common words

    public DefaultTranslator() {
        this.translationCache = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        this.supportedLanguages = Arrays.asList("es", "de", "fr", "it", "pt", "nl", "ru", "zh", "ja", "ko");
        this.staticTranslations = new ConcurrentHashMap<>();
        initializeStaticTranslations();
    }


    private void initializeStaticTranslations() {
        // Common words in multiple languages
        staticTranslations.put("flor", "flower");      // Spanish
        staticTranslations.put("blume", "flower");     // German
        staticTranslations.put("fiore", "flower");     // Italian
        staticTranslations.put("fleur", "flower");     // French
        staticTranslations.put("bloem", "flower");     // Dutch

        staticTranslations.put("casa", "house");       // Spanish
        staticTranslations.put("haus", "house");       // German
        staticTranslations.put("maison", "house");     // French
        staticTranslations.put("casa", "house");       // Italian
        staticTranslations.put("huis", "house");       // Dutch

        staticTranslations.put("agua", "water");       // Spanish
        staticTranslations.put("wasser", "water");     // German
        staticTranslations.put("eau", "water");        // French
        staticTranslations.put("acqua", "water");      // Italian
        staticTranslations.put("water", "water");      // Dutch (same)

        staticTranslations.put("perro", "dog");        // Spanish
        staticTranslations.put("hund", "dog");         // German
        staticTranslations.put("chien", "dog");        // French
        staticTranslations.put("cane", "dog");         // Italian
        staticTranslations.put("hond", "dog");         // Dutch

        staticTranslations.put("gato", "cat");         // Spanish
        staticTranslations.put("katze", "cat");        // German
        staticTranslations.put("chat", "cat");         // French
        staticTranslations.put("gatto", "cat");        // Italian
        staticTranslations.put("kat", "cat");          // Dutch

        staticTranslations.put("libro", "book");       // Spanish
        staticTranslations.put("buch", "book");        // German
        staticTranslations.put("livre", "book");       // French
        staticTranslations.put("libro", "book");       // Italian
        staticTranslations.put("boek", "book");        // Dutch

        staticTranslations.put("coche", "car");        // Spanish
        staticTranslations.put("auto", "car");         // German
        staticTranslations.put("voiture", "car");      // French
        staticTranslations.put("macchina", "car");     // Italian
        staticTranslations.put("auto", "car");         // Dutch

        staticTranslations.put("mesa", "table");       // Spanish
        staticTranslations.put("tisch", "table");      // German
        staticTranslations.put("table", "table");      // French (same)
        staticTranslations.put("tavolo", "table");     // Italian
        staticTranslations.put("tafel", "table");      // Dutch
    }

    @Override
    public String translate(String word) {
        if (word == null || word.trim().isEmpty()) {
            return word;
        }

        String normalizedWord = word.toLowerCase().trim();

        // Check cache first
        if (translationCache.containsKey(normalizedWord)) {
            return translationCache.get(normalizedWord);
        }

        // Try automatic translation
        try {
            String translation = translateWithService(normalizedWord);
            if (translation != null && !translation.equals(normalizedWord)) {
                translationCache.put(normalizedWord, translation);
                return translation;
            }
        } catch (Exception e) {
            System.out.println("Translation service failed for: " + word + ", using fallback");
        }

        // Fallback to static translations
        String staticTranslation = staticTranslations.get(normalizedWord);
        if (staticTranslation != null) {
            translationCache.put(normalizedWord, staticTranslation);
            return staticTranslation;
        }

        // If no translation found, return original word
        translationCache.put(normalizedWord, normalizedWord);
        return normalizedWord;
    }


    private String translateWithService(String word) throws Exception {
        // Try to detect language and translate to English
        for (String langCode : supportedLanguages) {
            String translation = callTranslationAPI(word, langCode, "en");
            if (translation != null && !translation.equals(word) && isValidEnglishWord(translation)) {
                return translation.toLowerCase();
            }
        }
        return null;
    }


    private String callTranslationAPI(String text, String fromLang, String toLang) {
        try {
            String encodedText = URLEncoder.encode(text, "UTF-8");
            String urlStr = String.format(
                    "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s",
                    encodedText, fromLang, toLang
            );

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds timeout
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response
                JsonNode jsonNode = objectMapper.readTree(response.toString());
                JsonNode responseData = jsonNode.get("responseData");
                if (responseData != null) {
                    String translatedText = responseData.get("translatedText").asText();
                    double match = responseData.get("match").asDouble();

                    // Only accept high-confidence translations
                    if (match > 0.7 && !translatedText.toLowerCase().equals(text.toLowerCase())) {
                        return translatedText.trim().toLowerCase();
                    }
                }
            }
        } catch (Exception e) {
            // Silent failure, will try next language or fallback
        }
        return null;
    }


    private boolean isValidEnglishWord(String word) {
        // Basic validation - English words typically don't have certain character patterns
        word = word.toLowerCase();

        // Too short or too long
        if (word.length() < 2 || word.length() > 20) return false;

        // Contains non-alphabetic characters
        if (!word.matches("^[a-z]+$")) return false;

        // Some patterns that are unlikely in English
        if (word.contains("ñ") || word.contains("ç") || word.contains("ü") ||
                word.contains("ä") || word.contains("ö") || word.contains("é") ||
                word.contains("è") || word.contains("à") || word.contains("ì")) {
            return false;
        }

        // Very basic English word patterns
        return true;
    }

    @Override
    public boolean isTranslationAvailable(String word) {
        if (word == null) return false;

        String normalizedWord = word.toLowerCase().trim();

        // Check if we have it in cache
        if (translationCache.containsKey(normalizedWord)) {
            return !translationCache.get(normalizedWord).equals(normalizedWord);
        }

        // Check static translations
        if (staticTranslations.containsKey(normalizedWord)) {
            return true;
        }

        // For unknown words, we can't determine without actually trying to translate
        return false;
    }


    public CompletableFuture<String> translateAsync(String word) {
        return CompletableFuture.supplyAsync(() -> translate(word));
    }


    public Map<String, String> translateBatch(List<String> words) {
        Map<String, String> results = new ConcurrentHashMap<>();

        words.parallelStream().forEach(word -> {
            String translation = translate(word);
            results.put(word, translation);
        });

        return results;
    }


    public void clearCache() {
        translationCache.clear();
    }


    public int getCacheSize() {
        return translationCache.size();
    }


    public void addCustomTranslation(String foreignWord, String englishWord) {
        if (foreignWord != null && englishWord != null) {
            staticTranslations.put(foreignWord.toLowerCase().trim(), englishWord.toLowerCase().trim());
            // Update cache if it exists
            translationCache.put(foreignWord.toLowerCase().trim(), englishWord.toLowerCase().trim());
        }
    }


    public TranslationStats getStats() {
        int cacheSize = translationCache.size();
        int staticSize = staticTranslations.size();
        int successful = (int) translationCache.values().stream()
                .filter(translation -> !translationCache.entrySet().stream()
                        .anyMatch(entry -> entry.getKey().equals(translation)))
                .count();

        return new TranslationStats(cacheSize, staticSize, successful);
    }


    public static class TranslationStats {
        private final int cacheSize;
        private final int staticTranslations;
        private final int successfulTranslations;

        public TranslationStats(int cacheSize, int staticTranslations, int successfulTranslations) {
            this.cacheSize = cacheSize;
            this.staticTranslations = staticTranslations;
            this.successfulTranslations = successfulTranslations;
        }

        public int getCacheSize() { return cacheSize; }
        public int getStaticTranslations() { return staticTranslations; }
        public int getSuccessfulTranslations() { return successfulTranslations; }
    }
}
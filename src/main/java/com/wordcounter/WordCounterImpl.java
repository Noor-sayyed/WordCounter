package com.wordcounter;


import com.wordcounter.exception.InvalidWordException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;


public class WordCounterImpl implements WordCounter {

    private final ConcurrentHashMap<String, AtomicInteger> wordCounts;
    private final Translator translator;
    private final AtomicInteger totalWords;

    public static class Builder {
        private Translator translator = new DefaultTranslator();

        public Builder withTranslator(Translator translator) {
            this.translator = translator;
            return this;
        }

        public WordCounterImpl build() {
            return new WordCounterImpl(translator);
        }
    }

    private WordCounterImpl(Translator translator) {
        this.wordCounts = new ConcurrentHashMap<>();
        this.translator = translator;
        this.totalWords = new AtomicInteger(0);
    }


    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void addWord(String word) throws InvalidWordException {
        WordValidator.validateWord(word);

        String normalizedWord = WordValidator.normalizeWord(word);
        String translatedWord = translator.translate(normalizedWord);

        // Thread-safe increment using computeIfAbsent and AtomicInteger
        wordCounts.computeIfAbsent(translatedWord, k -> new AtomicInteger(0))
                .incrementAndGet();
        totalWords.incrementAndGet();
    }

    @Override
    public void addWords(List<String> words) throws InvalidWordException {
        if (words == null) {
            throw new InvalidWordException("Word list cannot be null", null);
        }

        for (String word : words) {
            addWord(word);
        }
    }

    @Override
    public void addWords(String... words) throws InvalidWordException {
        if (words == null) {
            throw new InvalidWordException("Word array cannot be null", null);
        }

        addWords(Arrays.asList(words));
    }

    @Override
    public int getCount(String word) {
        if (word == null) {
            return 0;
        }

        String normalizedWord = WordValidator.normalizeWord(word);
        String translatedWord = translator.translate(normalizedWord);

        AtomicInteger count = wordCounts.get(translatedWord);
        return count != null ? count.get() : 0;
    }

    @Override
    public void reset() {
        wordCounts.clear();
        totalWords.set(0);
    }

    @Override
    public int getTotalWords() {
        return totalWords.get();
    }


    public int getUniqueWordCount() {
        return wordCounts.size();
    }


    public boolean isEmpty() {
        return totalWords.get() == 0;
    }
}
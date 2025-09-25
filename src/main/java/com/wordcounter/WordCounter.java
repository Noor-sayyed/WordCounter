package com.wordcounter;
import java.util.List;
import com.wordcounter.exception.InvalidWordException;

public interface WordCounter {

    void addWord(String word) throws InvalidWordException;

    void addWords(List<String> words) throws InvalidWordException;

    void addWords(String... words) throws InvalidWordException;

    int getCount(String word);

    void reset();

    int getTotalWords();
}
package com.wordcounter;
public interface Translator {


    String translate(String word);

    boolean isTranslationAvailable(String word);
}
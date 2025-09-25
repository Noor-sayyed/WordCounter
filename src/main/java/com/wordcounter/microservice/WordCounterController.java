package com.wordcounter.microservice;

import com.wordcounter.WordCounter;
import com.wordcounter.WordCounterImpl;
import com.wordcounter.exception.InvalidWordException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.HashMap;


@RestController
@RequestMapping("/api/wordcounter")
@CrossOrigin(origins = "*")
public class WordCounterController {

    private final WordCounter wordCounter;


    public WordCounterController() {
        this.wordCounter = WordCounterImpl.builder().build();
    }


    @PostMapping("/words")
    public ResponseEntity<Map<String, Object>> addWord(@RequestBody Map<String, String> request) {
        try {
            String word = request.get("word");
            wordCounter.addWord(word);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Word added successfully");
            response.put("word", word);
            response.put("totalWords", wordCounter.getTotalWords());

            return ResponseEntity.ok(response);
        } catch (InvalidWordException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("invalidWord", e.getInvalidWord());

            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PostMapping("/words/batch")
    public ResponseEntity<Map<String, Object>> addWords(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> words = request.get("words");
            if (words == null || words.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Word list cannot be null or empty");

                return ResponseEntity.badRequest().body(errorResponse);
            }

            wordCounter.addWords(words);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", words.size() + " words added successfully");
            response.put("wordsAdded", words.size());
            response.put("totalWords", wordCounter.getTotalWords());

            return ResponseEntity.ok(response);
        } catch (InvalidWordException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("invalidWord", e.getInvalidWord());

            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/words/{word}/count")
    public ResponseEntity<Map<String, Object>> getWordCount(@PathVariable String word) {
        try {
            int count = wordCounter.getCount(word);

            Map<String, Object> response = new HashMap<>();
            response.put("word", word);
            response.put("count", count);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error retrieving word count: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("totalWords", wordCounter.getTotalWords());
            response.put("success", true);

            if (wordCounter instanceof WordCounterImpl) {
                WordCounterImpl impl = (WordCounterImpl) wordCounter;
                response.put("uniqueWords", impl.getUniqueWordCount());
                response.put("isEmpty", impl.isEmpty());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error retrieving statistics: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        try {
            wordCounter.reset();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Word counter reset successfully");
            response.put("totalWords", wordCounter.getTotalWords());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error resetting counter: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "WordCounter");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
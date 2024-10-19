package com.example.learnlingua.rest;

import com.example.learnlingua.model.Card;
import com.example.learnlingua.model.User;
import com.example.learnlingua.service.CardService;
import com.example.learnlingua.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/learn")
public class LearningController {

    private final CardService cardService;
    private final UserService userService;

    @Autowired
    public LearningController(CardService cardService, UserService userService) {
        this.cardService = cardService;
        this.userService = userService;
    }

    @GetMapping()
    public ResponseEntity<Map<String, Object>> getCardsForLearning() {
        User currentUser = getCurrentUser();
        List<Card> cards = cardService.getCardsForLearning(currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("cards", cards);
        response.put("currentCardIndex", cardService.getCurrentCardIndex(currentUser));

        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<Map<String, Object>> submitAnswers(@RequestBody Map<String, Boolean> answers) {
        User currentUser = getCurrentUser();
        Map<String, Object> response = new HashMap<>();

        try {
            cardService.processAnswers(currentUser, answers);

            // Визначаємо, чи потрібно видавати нові картки
            if (cardService.isSessionComplete(currentUser)) {
                response.put("completed", true);
                // Можливо, тут потрібно перенаправити на сторінку результатів?
            } else {
                List<Card> newCards = cardService.getCardsForLearning(currentUser);
                response.put("cards", newCards);
                response.put("currentCardIndex", cardService.getCurrentCardIndex(currentUser));
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Допоміжний метод для отримання поточного користувача
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Поточний користувач не знайдений"));
    }
}
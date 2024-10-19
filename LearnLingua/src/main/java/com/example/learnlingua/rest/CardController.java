package com.example.learnlingua.rest;

import com.example.learnlingua.model.Card;
import com.example.learnlingua.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@PreAuthorize("hasRole('ADMIN')") // Дозволено тільки для ADMIN
public class CardController {

    private final CardService cardService;

    @Autowired
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Card> getCardById(@PathVariable Long id) {
        return cardService.getCardById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping()
    public ResponseEntity<List<Card>> getCards(@RequestParam(required = false) String word,
                                               @RequestParam(required = false) String partOfSpeech) {
        if (word != null) {
            return ResponseEntity.ok(cardService.getCardsByWord(word));
        } else if (partOfSpeech != null) {
            return ResponseEntity.ok(cardService.getCardsByPartOfSpeech(partOfSpeech));
        } else {
            return ResponseEntity.ok(cardService.getAllCards());
        }
    }

    @PostMapping()
    public ResponseEntity<Card> createCard(@RequestBody Card card) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(card));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Card> updateCard(@PathVariable Long id, @RequestBody Card card) {
        try {
            return ResponseEntity.ok(cardService.updateCard(id, card));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        try {
            cardService.deleteCard(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
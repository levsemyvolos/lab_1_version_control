//package com.example.learnlingua.service;
//
//import com.example.learnlingua.model.Card;
//import com.example.learnlingua.model.User;
//import com.example.learnlingua.model.UserProgress;
//import com.example.learnlingua.repository.CardRepository;
//import com.example.learnlingua.repository.UserProgressRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.*;
//
//@Service
//public class CardService {
//
//    private static final int MAX_WORDS_PER_SESSION = 5;
//    private static final int NEW_WORDS_PER_SESSION = 2;
//    private final CardRepository cardRepository;
//    private final UserProgressRepository progressRepository;
//    private final Map<Long, Integer> userCurrentCardIndex = new HashMap<>();
//
//    @Autowired
//    public CardService(CardRepository cardRepository, UserProgressRepository progressRepository) {
//        this.cardRepository = cardRepository;
//        this.progressRepository = progressRepository;
//    }
//
//    public List<Card> getCardsForLearning(User user) {
//        List<Card> cardsForLearning = new ArrayList<>();
//        List<Card> learnedCards = getLearnedCards(user);
//        List<Card> newCards = getNewCards(user, learnedCards);
//
//        int learnedIndex = 0;
//        int newIndex = 0;
//        for (int i = 0; i < MAX_WORDS_PER_SESSION; i++) {
//            if (i % 2 == 0 && learnedIndex < learnedCards.size() || newIndex >= newCards.size()) {
//                cardsForLearning.add(learnedCards.get(learnedIndex++));
//            } else if (newIndex < newCards.size()) {
//                cardsForLearning.add(newCards.get(newIndex++));
//            }
//        }
//        userCurrentCardIndex.put(user.getId(), 0); // Скидаємо індекс поточної картки
//        return cardsForLearning;
//    }
//
//    private List<Card> getLearnedCards(User user) {
//        List<UserProgress> userProgress = progressRepository.findByUserId(user.getId());
//        List<Card> learnedCards = new ArrayList<>();
//        LocalDate today = LocalDate.now(ZoneId.systemDefault());
//
//        userProgress.stream()
//                .filter(progress -> progress.getDue() != null && !today.isAfter(progress.getDue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()))
//                .sorted(Comparator.comparing(UserProgress::getDue))
//                .forEach(progress -> learnedCards.add(progress.getCard()));
//
//        if (learnedCards.isEmpty()) {
//            List<Card> allCards = cardRepository.findAll();
//            Collections.shuffle(allCards);
//            learnedCards.addAll(allCards.subList(0, Math.min(MAX_WORDS_PER_SESSION - NEW_WORDS_PER_SESSION, allCards.size())));
//        }
//
//        if (learnedCards.size() < MAX_WORDS_PER_SESSION - NEW_WORDS_PER_SESSION) {
//            userProgress.stream()
//                    .filter(progress -> !learnedCards.contains(progress.getCard()))
//                    .sorted(Comparator.comparing(UserProgress::getDue))
//                    .limit(MAX_WORDS_PER_SESSION - NEW_WORDS_PER_SESSION - learnedCards.size())
//                    .forEach(progress -> learnedCards.add(progress.getCard()));
//        }
//
//        return learnedCards;
//    }
//
//    private List<Card> getNewCards(User user, List<Card> learnedCards) {
//        List<Card> newCards = new ArrayList<>();
//        List<Card> allCards = cardRepository.findAll();
//        Collections.shuffle(allCards);
//
//        for (Card card : allCards) {
//            if (!learnedCards.contains(card) && newCards.size() < NEW_WORDS_PER_SESSION) {
//                newCards.add(card);
//            }
//        }
//        return newCards;
//    }
//
//    public UserProgress startLearningCard(User user, Card card) {
//        UserProgress progress = progressRepository.findByUserIdAndCardId(user.getId(), card.getId())
//                .orElseGet(() -> {
//                    UserProgress newProgress = new UserProgress();
//                    newProgress.setUser(user);
//                    newProgress.setCard(card);
//                    newProgress.setLearnedLevel(0);
//                    newProgress.setLastUpdated(new Date());
//                    newProgress.setEase(2.5);
//                    newProgress.setInterval(0);
//                    newProgress.setReps(0);
//                    return newProgress;
//                });
//
//        progress.setLastUpdated(new Date());
//        return progressRepository.save(progress);
//    }
//
//    public void updateProgress(UserProgress progress, boolean isCorrect) {
//        if (isCorrect) {
//            progress.setEase(Math.max(1.3, progress.getEase() + 0.1 - (5 - progress.getLearnedLevel()) * (0.08 + (5 - progress.getLearnedLevel()) * 0.02)));
//            progress.setLearnedLevel(progress.getLearnedLevel() + 1);
//        } else {
//            progress.setEase(Math.max(1.3, progress.getEase() - 0.2 - (5 - progress.getLearnedLevel()) * (0.08 + (5 - progress.getLearnedLevel()) * 0.02)));
//            progress.setLearnedLevel(Math.max(0, progress.getLearnedLevel() - 1));
//        }
//
//        int[] intervals = {10, 60, 720, 2880, 10080 /* і так далі */};
//        int newInterval = progress.getReps() < intervals.length ? intervals[progress.getReps()] : intervals[intervals.length - 1];
//        progress.setInterval(newInterval);
//        progress.setDue(Date.from(LocalDateTime.now().plusMinutes(newInterval).atZone(ZoneId.systemDefault()).toInstant()));
//
//        progress.setReps(progress.getReps() + 1);
//        progressRepository.save(progress);
//    }
//
//    public Optional<Card> getCardById(Long cardId) {
//        return cardRepository.findById(cardId);
//    }
//
//    public void processAnswers(User user, Map<String, Boolean> answers) {
//        answers.forEach((cardId, isCorrect) -> {
//            long parsedCardId = Long.parseLong(cardId);
//            Optional<Card> cardOptional = getCardById(parsedCardId);
//            if (cardOptional.isPresent()) {
//                Card card = cardOptional.get();
//                UserProgress progress = startLearningCard(user, card);
//                updateProgress(progress, isCorrect);
//            } else {
//                throw new IllegalArgumentException("Картку з ID " + cardId + " не знайдено.");
//            }
//        });
//
//        // Збільшуємо індекс поточної картки
//        userCurrentCardIndex.computeIfPresent(user.getId(), (userId, index) -> index + answers.size());
//    }
//
//    public int getCurrentCardIndex(User user) {
//        return userCurrentCardIndex.getOrDefault(user.getId(), 0);
//    }
//
//    public boolean isSessionComplete(User user) {
//        return getCurrentCardIndex(user) >= MAX_WORDS_PER_SESSION;
//    }
//}
package com.example.learnlingua.service;

import com.example.learnlingua.model.Card;
import com.example.learnlingua.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CardService {

    private final CardRepository cardRepository;

    @Autowired
    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    public Optional<Card> getCardById(Long id) {
        return cardRepository.findById(id);
    }

    public List<Card> getCardsByWord(String word) {
        return cardRepository.findByWordContainingIgnoreCase(word);
    }

    public List<Card> getCardsByPartOfSpeech(String partOfSpeech) {
        return cardRepository.findByPartOfSpeech(partOfSpeech);
    }

    public Card createCard(Card card) {
        return cardRepository.save(card);
    }

    public Card updateCard(Long id, Card card) {
        if (!cardRepository.existsById(id)) {
            throw new IllegalArgumentException("Картку з ID " + id + " не знайдено.");
        }
        card.setId(id);
        return cardRepository.save(card);
    }

    public void deleteCard(Long id) {
        if (!cardRepository.existsById(id)) {
            throw new IllegalArgumentException("Картку з ID " + id + " не знайдено.");
        }
        cardRepository.deleteById(id);
    }
}
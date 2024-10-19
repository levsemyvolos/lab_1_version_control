package com.example.learnlingua.repository;

import com.example.learnlingua.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByWordContainingIgnoreCase(String word);

    List<Card> findByPartOfSpeech(String partOfSpeech);
}
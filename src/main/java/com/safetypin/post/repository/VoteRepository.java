package com.safetypin.post.repository;

import com.safetypin.post.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Vote.VoteId> {
}
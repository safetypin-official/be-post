package com.safetypin.post.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.safetypin.post.model.Vote;

public interface VoteRepository extends JpaRepository<Vote, Vote.VoteId> {

    @Modifying
    @Query("DELETE FROM Vote v WHERE v.id.userId = :userId")
    int deleteVotesByUserId(@Param("userId") UUID userId);
}
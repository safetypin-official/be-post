package com.safetypin.post.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
public abstract class BasePost {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String caption;

    @Column(nullable = false, columnDefinition = "timestamp")
    private LocalDateTime createdAt;

    @Column
    private UUID postedBy;
}

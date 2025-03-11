package com.safetypin.post.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Comment extends BasePost {
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    public Post commentAt;
}

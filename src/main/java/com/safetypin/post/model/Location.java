package com.safetypin.post.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a geographic location
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private double latitude;
    private double longitude;
}

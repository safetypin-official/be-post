package com.safetypin.post.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "posts")
public class Post extends BasePost {

    @Transient
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    @Column(nullable = false)
    private String title = "";

    @JsonIgnore
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location = geometryFactory.createPoint(new Coordinate(0.0d, 0.0d));

    // Changed from UUID to String and column name to "name"
    @Column(nullable = false, name = "name")
    private String category;

    // Additional fields as needed

    // Add constructor that accepts latitude and longitude as separate parameters
    public Post(String caption, String title, String category, LocalDateTime createdAt, Double latitude, Double longitude) {
        this.setCaption(caption);
        this.setTitle(title);
        this.setCategory(category);
        this.setCreatedAt(createdAt);
        // Validate and convert latitude and longitude to Point
        validateCoordinates(latitude, longitude);
        this.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
    }

    /**
     * Static method to create a new Builder instance
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Validates that both latitude and longitude are not null
     *
     * @param latitude  the latitude value
     * @param longitude the longitude value
     * @throws IllegalArgumentException if either latitude or longitude is null
     */
    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            throw new IllegalArgumentException("Both latitude and longitude cannot be null");
        } else if (latitude == null) {
            throw new IllegalArgumentException("Latitude cannot be null");
        } else if (longitude == null) {
            throw new IllegalArgumentException("Longitude cannot be null");
        }
    }

    // Methods to get latitude and longitude from the Point
    public Double getLatitude() {
        return location != null ? location.getY() : null;
    }

    // Fix the method name typo and implementation
    public void setLatitude(Double latitude) {
        if (latitude == null) {
            throw new IllegalArgumentException("Latitude cannot be null");
        }
    
        if (location != null) {
            // Create a new Point with the updated latitude
            location = geometryFactory.createPoint(new Coordinate(location.getX(), latitude));
        } else {
            throw new IllegalArgumentException("Location must be initialized before setting latitude");
        }
    }

    public Double getLongitude() {
        return location != null ? location.getX() : null;
    }

    public void setLongitude(Double longitude) {
        if (longitude == null) {
            throw new IllegalArgumentException("Longitude cannot be null");
        }
    
        if (location != null) {
            // Create a new Point with the updated longitude
            location = geometryFactory.createPoint(new Coordinate(longitude, location.getY()));
        } else {
            throw new IllegalArgumentException("Location must be initialized before setting longitude");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null) {
            setCreatedAt(LocalDateTime.now());
        }
    }

    /**
     * Builder class for Post
     */
    public static class Builder {
        private UUID id;
        private String caption;
        private String title;
        private String category;
        private LocalDateTime createdAt;
        private Double latitude;
        private Double longitude;

        public Builder() {
            /* This constructor is intentionally empty as it is used by the Builder pattern.
             * No initialization is needed here since all fields are set via the builder methods.
             */
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder caption(String caption) {
            this.caption = caption;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder location(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public Post build() {
            Post post = new Post();
            post.setId(id);
            post.setCaption(caption);
            post.setTitle(title);
            post.setCategory(category);
            post.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());

            // Validate and set location
            if (latitude == null || longitude == null) {
                throw new IllegalArgumentException("Both latitude and longitude must be provided");
            }
            post.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));

            return post;
        }
    }
}

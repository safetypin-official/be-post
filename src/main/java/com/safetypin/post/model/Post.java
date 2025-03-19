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

    // Changed from Category entity to String - fix column name to match database schema
    @Column(nullable = false, name = "category_id")
    private String category;

    // Additional fields as needed

    // Add constructor that accepts latitude and longitude as separate parameters
    public Post(String caption, String title, String category, LocalDateTime createdAt, Double latitude, Double longitude) {
        this.setCaption(caption);
        this.setTitle(title);
        this.setCategory(category);
        this.setCreatedAt(createdAt);
        // Convert latitude and longitude to Point
        if (latitude != null && longitude != null) {
            this.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        }
    }

    /**
     * Static method to create a new Builder instance
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // Methods to get latitude and longitude from the Point
    public Double getLatitude() {
        return location != null ? location.getY() : null;
    }

    // Fix the method name typo and implementation
    public void setLatitude(Double latitude) {
        if (location != null) {
            // Create a new Point with the updated latitude
            location = geometryFactory.createPoint(new Coordinate(location.getX(), latitude));
        } else if (latitude != null) {
            // If location is null but latitude is provided, create a new Point
            location = geometryFactory.createPoint(new Coordinate(0, latitude));
        }
    }

    public Double getLongitude() {
        return location != null ? location.getX() : null;
    }

    public void setLongitude(Double longitude) {
        if (location != null) {
            // Create a new Point with the updated longitude
            location = geometryFactory.createPoint(new Coordinate(longitude, location.getY()));
        } else if (longitude != null) {
            // If location is null but longitude is provided, create a new Point
            location = geometryFactory.createPoint(new Coordinate(longitude, 0));
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

            // Set location if latitude and longitude are provided
            if (latitude != null && longitude != null) {
                post.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
            }

            return post;
        }
    }
}

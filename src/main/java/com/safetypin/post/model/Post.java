package com.safetypin.post.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {

    @Transient
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String content;
    @Column(nullable = true)
    private String title;
    @Column(nullable = true)
    private String category;
    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    // Additional fields as needed

    // Add constructor that accepts latitude and longitude as separate parameters
    public Post(UUID id, String content, String title, String category, LocalDateTime createdAt, Double latitude, Double longitude) {
        this.id = id;
        this.content = content;
        this.title = title;
        this.category = category;
        this.createdAt = createdAt;
        // Convert latitude and longitude to Point
        if (latitude != null && longitude != null) {
            this.location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
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
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

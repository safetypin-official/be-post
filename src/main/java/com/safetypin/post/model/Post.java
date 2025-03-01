package com.safetypin.post.model;

import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
    
    // Add a GeometryFactory for creating new Point objects
    @Transient
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    
    // Additional fields as needed

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Double getLongitude() {
        return location != null ? location.getX() : null;
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

    public void setLongitude(Double longitude) {
        if (location != null) {
            // Create a new Point with the updated longitude
            location = geometryFactory.createPoint(new Coordinate(longitude, location.getY()));
        } else if (longitude != null) {
            // If location is null but longitude is provided, create a new Point
            location = geometryFactory.createPoint(new Coordinate(longitude, 0));
        }
    }

    // Add constructor that accepts latitude and longitude as separate parameters
    public Post(Long id, String content, String title, String category, LocalDateTime createdAt, Double latitude, Double longitude) {
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

    
}

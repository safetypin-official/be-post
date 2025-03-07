package com.safetypin.post.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "posts")
public class Post {

    @Transient
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String caption;

    @Column()
    private String title;

    @Column(nullable = false)
    @ManyToOne
    private Category category;

    @Column(nullable = false, columnDefinition = "timestamp")
    private LocalDateTime createdAt;

    //@JsonSerialize(using = PointSerializer.class)
    @JsonIgnore
    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    // Additional fields as needed

    // Add constructor that accepts latitude and longitude as separate parameters
    public Post(String caption, String title, Category category, LocalDateTime createdAt, Double latitude, Double longitude) {
        this.caption = caption;
        this.title = title;
        this.category = category;
        this.createdAt = createdAt;
        // Convert latitude and longitude to Point
        if (latitude != null && longitude != null) {
            this.location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        }
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
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

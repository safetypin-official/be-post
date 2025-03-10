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
public class Post extends BasePost{

    @Transient
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    @Column(nullable = true)
    private String title;

    @Column(nullable = true)
    private String category;

    //@JsonSerialize(using = PointSerializer.class)
    @JsonIgnore
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location = geometryFactory.createPoint(new Coordinate(0.0d, 0.0d));

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
}

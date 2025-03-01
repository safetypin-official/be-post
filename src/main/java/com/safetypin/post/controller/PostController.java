package com.safetypin.post.controller;

import com.safetypin.post.service.LocationService;
import com.safetypin.post.service.PostService;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final LocationService locationService;
    private final GeometryFactory geometryFactory;

    public PostController(PostService postService, LocationService locationService) {
        this.postService = postService;
        this.locationService = locationService;
        this.geometryFactory = new GeometryFactory();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPosts(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false, defaultValue = "10.0") Double radius,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // If lat and lon are provided, use them
            Double latitude = lat;
            Double longitude = lon;

            // If not provided, try to use user's stored location
            if (latitude == null || longitude == null) {
                Point userLocation = locationService.getCurrentUserLocation();
                if (userLocation != null) {
                    latitude = userLocation.getY();
                    longitude = userLocation.getX();
                } else {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "Location required");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(errorResponse);
                }
            }

            LocalDateTime fromDateTime = dateFrom != null ?
                    LocalDateTime.of(dateFrom, LocalTime.MIN) : null;
            LocalDateTime toDateTime = dateTo != null ?
                    LocalDateTime.of(dateTo, LocalTime.MAX) : null;

            Pageable pageable = PageRequest.of(page, size);

            Page<?> posts = postService.findPostsByLocation(
                    latitude, longitude, radius, category, fromDateTime, toDateTime, pageable);

            return ResponseEntity.ok()
                               .contentType(MediaType.APPLICATION_JSON)
                               .body(posts);
        } catch (NumberFormatException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid location parameters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                               .contentType(MediaType.APPLICATION_JSON)
                               .body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error processing request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .contentType(MediaType.APPLICATION_JSON)
                               .body(errorResponse);
        }
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Invalid location parameters");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .contentType(MediaType.APPLICATION_JSON)
                           .body(errorResponse);
    }
}

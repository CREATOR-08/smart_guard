package com.smartguard.controller;

import com.smartguard.model.Point;
import com.smartguard.model.PolygonAnalysisResponse;
import com.smartguard.service.ArtGalleryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polygon/analyze")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ArtGalleryController {

    private final ArtGalleryService artGalleryService;

    public ArtGalleryController(ArtGalleryService artGalleryService) {
        this.artGalleryService = artGalleryService;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String checkApi() {
        return "API working";
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PolygonAnalysisResponse analyzePolygon(@RequestBody List<Point> vertices) {
        return artGalleryService.analyzePolygon(vertices);
    }
}
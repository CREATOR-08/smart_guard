package com.smartguard.controller;

import com.smartguard.model.Point;
import com.smartguard.model.PolygonAnalysisResponse;
import com.smartguard.service.ArtGalleryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polygon")
@CrossOrigin(origins = "*")
public class ArtGalleryController {

    private final ArtGalleryService artGalleryService;

    public ArtGalleryController(ArtGalleryService artGalleryService) {
        this.artGalleryService = artGalleryService;
    }

    @PostMapping("/analyze")
    public PolygonAnalysisResponse analyzePolygon(@RequestBody List<Point> vertices) {
        return artGalleryService.analyzePolygon(vertices);
    }
}
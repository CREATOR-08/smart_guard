package com.smartguard.service;

import com.smartguard.algorithms.GreedySetCover;
import com.smartguard.algorithms.PolygonColoring;
import com.smartguard.algorithms.PolygonTriangulator;
import com.smartguard.algorithms.PolygonValidator;
import com.smartguard.algorithms.VisibilityCalculator;
import com.smartguard.geometry.GeometryUtils;
import com.smartguard.model.Point;
import com.smartguard.model.PolygonAnalysisResponse;
import com.smartguard.model.Triangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ArtGalleryService {

    private static final double EPSILON = GeometryUtils.EPSILON;

    public PolygonAnalysisResponse analyzePolygon(List<Point> vertices) {
        PolygonAnalysisResponse response = new PolygonAnalysisResponse();
        int n = vertices == null ? 0 : vertices.size();
        response.setVertexCount(n);
        response.setEdgeCount(n);

        if (n < 3) {
            response.setValid(false);
            response.setStatusMessage("Invalid polygon: At least 3 vertices are required.");
            return response;
        }

        String intersectionError = PolygonValidator.checkSelfIntersections(vertices);
        if (intersectionError != null) {
            response.setValid(false);
            response.setStatusMessage(intersectionError);
            return response;
        }

        double signedArea = GeometryUtils.computeSignedArea(vertices);
        if (Math.abs(signedArea) < EPSILON) {
            response.setValid(false);
            response.setStatusMessage("Invalid polygon: Vertices are collinear.");
            return response;
        }

        response.setVertexTypes(GeometryUtils.classifyVertices(vertices));

        List<int[]> diagonals = new ArrayList<>();
        List<Triangle> triangles = PolygonTriangulator.monotoneTriangulate(vertices, diagonals);

        if (triangles.size() != n - 2) {
            response.setValid(false);
            response.setStatusMessage("Triangulation failed: Polygon may have degenerate overlapping edges.");
            return response;
        }

        response.setValid(true);
        response.setStatusMessage("Valid simple polygon");
        response.setTriangles(triangles);
        response.setDiagonals(diagonals);

        int[] colors = PolygonColoring.threeColorPolygon(n, triangles);
        List<Integer> colorList = new ArrayList<>(n);
        Map<Integer, Integer> colorCounts = new HashMap<>();
        colorCounts.put(0, 0);
        colorCounts.put(1, 0);
        colorCounts.put(2, 0);

        for (int c : colors) {
            colorList.add(c);
            colorCounts.put(c, colorCounts.getOrDefault(c, 0) + 1);
        }
        response.setVertexColors(colorList);
        response.setColorCounts(colorCounts);

        int minColor = 0;
        for (int c = 1; c < 3; c++) {
            if (colorCounts.get(c) < colorCounts.get(minColor)) {
                minColor = c;
            }
        }
        response.setSmallestColorClass(minColor);
        response.setTheoreticalUpperBound(Math.max(1, n / 3));

        List<Integer> artGalleryCameras = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (colors[i] == minColor) {
                artGalleryCameras.add(i);
            }
        }
        response.setArtGalleryCameras(artGalleryCameras);

        Map<Integer, List<Integer>> visibilityMap = VisibilityCalculator.computeVertexToTriangleVisibility(vertices, triangles);
        response.setCameraCoveredTriangles(visibilityMap);

        List<Integer> greedyCameras = GreedySetCover.computeGreedySetCover(n, triangles.size(), visibilityMap);
        response.setGreedySetCoverCameras(greedyCameras);
        response.setCoveragePercentage(100.0);

        return response;
    }
}

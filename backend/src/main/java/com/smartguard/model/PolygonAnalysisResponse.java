package com.smartguard.model;

import java.util.List;
import java.util.Map;

public class PolygonAnalysisResponse {
    private boolean valid;
    private String statusMessage;
    private int vertexCount;
    private int edgeCount;

    // Step 2: Ear Clipping Triangulation
    private List<Triangle> triangles;
    private List<int[]> diagonals;

    // Step 3: 3-Coloring
    private List<Integer> vertexColors; // 0 = Red, 1 = Green, 2 = Blue for each original vertex index
    private Map<Integer, Integer> colorCounts;
    private int smallestColorClass;

    // Step 4: Camera Placement (Art Gallery Bound vs Greedy Set Cover)
    private int theoreticalUpperBound; // floor(n / 3)
    private List<Integer> artGalleryCameras;
    private List<Integer> greedySetCoverCameras;
    private double coveragePercentage;
    private Map<Integer, List<Integer>> cameraCoveredTriangles; // vertexIndex -> list of triangle indices covered

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public void setVertexCount(int vertexCount) {
        this.vertexCount = vertexCount;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public void setEdgeCount(int edgeCount) {
        this.edgeCount = edgeCount;
    }

    public List<Triangle> getTriangles() {
        return triangles;
    }

    public void setTriangles(List<Triangle> triangles) {
        this.triangles = triangles;
    }

    public List<int[]> getDiagonals() {
        return diagonals;
    }

    public void setDiagonals(List<int[]> diagonals) {
        this.diagonals = diagonals;
    }

    public List<Integer> getVertexColors() {
        return vertexColors;
    }

    public void setVertexColors(List<Integer> vertexColors) {
        this.vertexColors = vertexColors;
    }

    public Map<Integer, Integer> getColorCounts() {
        return colorCounts;
    }

    public void setColorCounts(Map<Integer, Integer> colorCounts) {
        this.colorCounts = colorCounts;
    }

    public int getSmallestColorClass() {
        return smallestColorClass;
    }

    public void setSmallestColorClass(int smallestColorClass) {
        this.smallestColorClass = smallestColorClass;
    }

    public int getTheoreticalUpperBound() {
        return theoreticalUpperBound;
    }

    public void setTheoreticalUpperBound(int theoreticalUpperBound) {
        this.theoreticalUpperBound = theoreticalUpperBound;
    }

    public List<Integer> getArtGalleryCameras() {
        return artGalleryCameras;
    }

    public void setArtGalleryCameras(List<Integer> artGalleryCameras) {
        this.artGalleryCameras = artGalleryCameras;
    }

    public List<Integer> getGreedySetCoverCameras() {
        return greedySetCoverCameras;
    }

    public void setGreedySetCoverCameras(List<Integer> greedySetCoverCameras) {
        this.greedySetCoverCameras = greedySetCoverCameras;
    }

    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public Map<Integer, List<Integer>> getCameraCoveredTriangles() {
        return cameraCoveredTriangles;
    }

    public void setCameraCoveredTriangles(Map<Integer, List<Integer>> cameraCoveredTriangles) {
        this.cameraCoveredTriangles = cameraCoveredTriangles;
    }
}
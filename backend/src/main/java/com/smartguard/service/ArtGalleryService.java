package com.smartguard.service;

import com.smartguard.model.Point;
import com.smartguard.model.PolygonAnalysisResponse;
import com.smartguard.model.Triangle;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArtGalleryService {

    private static final double EPSILON = 1e-9;

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

        // 1. Validate simple polygon (no self-intersections)
        String intersectionError = checkSelfIntersections(vertices);
        if (intersectionError != null) {
            response.setValid(false);
            response.setStatusMessage(intersectionError);
            return response;
        }

        double signedArea = computeSignedArea(vertices);
        if (Math.abs(signedArea) < EPSILON) {
            response.setValid(false);
            response.setStatusMessage("Invalid polygon: Vertices are collinear.");
            return response;
        }
        // ... existing collinear check ...
        if (Math.abs(signedArea) < EPSILON) {
            response.setValid(false);
            response.setStatusMessage("Invalid polygon: Vertices are collinear.");
            return response;
        }

        // ADD THIS LINE HERE: Classify sweep-line vertices
        response.setVertexTypes(classifyVertices(vertices));

        // 2. Ear Clipping Triangulation
        List<int[]> diagonals = new ArrayList<>();
        List<Triangle> triangles = earClippingTriangulate(vertices, signedArea, diagonals);

        if (triangles.size() != n - 2) {
            response.setValid(false);
            response.setStatusMessage("Triangulation failed: Polygon may have degenerate overlapping edges.");
            return response;
        }

        response.setValid(true);
        response.setStatusMessage("Valid simple polygon");
        response.setTriangles(triangles);
        response.setDiagonals(diagonals);

        // 3. 3-Color the Triangulated Graph via Dual Tree BFS
        int[] colors = threeColorPolygon(n, triangles);
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

        // Find smallest color class (Fisk's Theorem Guard Set)
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

        // 4. Compute Visibility & Greedy Set Cover over Triangles
        Map<Integer, List<Integer>> visibilityMap = computeVertexToTriangleVisibility(vertices, triangles);
        response.setCameraCoveredTriangles(visibilityMap);

        List<Integer> greedyCameras = computeGreedySetCover(n, triangles.size(), visibilityMap);
        response.setGreedySetCoverCameras(greedyCameras);
        response.setCoveragePercentage(100.0);

        return response;
    }

    private String checkSelfIntersections(List<Point> pts) {
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            Point a1 = pts.get(i);
            Point a2 = pts.get((i + 1) % n);
            for (int j = i + 2; j < n; j++) {
                // Skip adjacent edges sharing a vertex
                if (i == 0 && j == n - 1) continue;
                Point b1 = pts.get(j);
                Point b2 = pts.get((j + 1) % n);
                if (segmentsIntersect(a1, a2, b1, b2)) {
                    return String.format("Invalid polygon: Edge P%d-P%d intersects Edge P%d-P%d",
                            i + 1, ((i + 1) % n) + 1, j + 1, ((j + 1) % n) + 1);
                }
            }
        }
        return null;
    }

    private double computeSignedArea(List<Point> pts) {
        double sum = 0.0;
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            Point curr = pts.get(i);
            Point next = pts.get((i + 1) % n);
            sum += (curr.getX() * next.getY() - next.getX() * curr.getY());
        }
        return sum * 0.5;
    }

    private List<Triangle> earClippingTriangulate(List<Point> pts, double signedArea, List<int[]> diagonals) {
        int n = pts.size();
        List<Integer> active = new ArrayList<>(n);
        // Normalize vertex order so interior is always to the left of directed edges (positive area)
        if (signedArea > 0) {
            for (int i = 0; i < n; i++) active.add(i);
        } else {
            for (int i = n - 1; i >= 0; i--) active.add(i);
        }

        List<Triangle> result = new ArrayList<>();
        int guardCounter = 0;
        int maxIterations = n * n * 2;

        while (active.size() > 3 && guardCounter < maxIterations) {
            guardCounter++;
            boolean earFound = false;
            int m = active.size();

            for (int i = 0; i < m; i++) {
                int prevIdx = active.get((i - 1 + m) % m);
                int currIdx = active.get(i);
                int nextIdx = active.get((i + 1) % m);

                Point a = pts.get(prevIdx);
                Point b = pts.get(currIdx);
                Point c = pts.get(nextIdx);

                if (isEar(a, b, c, prevIdx, currIdx, nextIdx, active, pts)) {
                    result.add(new Triangle(prevIdx, currIdx, nextIdx, currIdx));
                    diagonals.add(new int[]{prevIdx, nextIdx});
                    active.remove(i);
                    earFound = true;
                    break;
                }
            }

            if (!earFound) {
                break;
            }
        }

        if (active.size() == 3) {
            result.add(new Triangle(active.get(0), active.get(1), active.get(2), active.get(1)));
        }

        return result;
    }

    private boolean isEar(Point a, Point b, Point c,
                          int idxA, int idxB, int idxC,
                          List<Integer> active, List<Point> pts) {
        // Must be strictly convex under normalized orientation
        if (crossProduct(a, b, c) <= EPSILON) {
            return false;
        }

        // No other active vertex may lie inside or on the boundary of triangle (a, b, c)
        for (int idx : active) {
            if (idx == idxA || idx == idxB || idx == idxC) continue;
            Point p = pts.get(idx);
            if (isPointInTriangle(p, a, b, c)) {
                return false;
            }
        }
        return true;
    }

    private int[] threeColorPolygon(int n, List<Triangle> triangles) {
        int[] colors = new int[n];
        Arrays.fill(colors, -1);

        if (triangles.isEmpty()) return colors;

        // Build adjacency list of dual tree (triangles sharing 2 vertices)
        int tCount = triangles.size();
        List<List<Integer>> adj = new ArrayList<>(tCount);
        for (int i = 0; i < tCount; i++) adj.add(new ArrayList<>());

        for (int i = 0; i < tCount; i++) {
            for (int j = i + 1; j < tCount; j++) {
                if (sharesEdge(triangles.get(i), triangles.get(j))) {
                    adj.get(i).add(j);
                    adj.get(j).add(i);
                }
            }
        }

        boolean[] visited = new boolean[tCount];
        Queue<Integer> queue = new LinkedList<>();

        // Seed the first triangle with colors 0, 1, 2
        Triangle first = triangles.get(0);
        colors[first.getV1()] = 0;
        colors[first.getV2()] = 1;
        colors[first.getV3()] = 2;
        visited[0] = true;
        queue.offer(0);

        while (!queue.isEmpty()) {
            int currIdx = queue.poll();
            for (int nextIdx : adj.get(currIdx)) {
                if (!visited[nextIdx]) {
                    visited[nextIdx] = true;
                    Triangle t = triangles.get(nextIdx);
                    int[] verts = t.toArray();

                    // Exactly two vertices are already colored; assign the remaining color (3 - c1 - c2)
                    int uncoloredVertex = -1;
                    int sumColored = 0;
                    int coloredCount = 0;
                    for (int v : verts) {
                        if (colors[v] != -1) {
                            sumColored += colors[v];
                            coloredCount++;
                        } else {
                            uncoloredVertex = v;
                        }
                    }
                    if (coloredCount == 2 && uncoloredVertex != -1) {
                        colors[uncoloredVertex] = 3 - sumColored;
                    }
                    queue.offer(nextIdx);
                }
            }
        }

        // Safety fallback for any isolated vertex
        for (int i = 0; i < n; i++) {
            if (colors[i] == -1) colors[i] = i % 3;
        }
        return colors;
    }

    private boolean sharesEdge(Triangle t1, Triangle t2) {
        int shared = 0;
        int[] a = t1.toArray();
        int[] b = t2.toArray();
        for (int u : a) {
            for (int v : b) {
                if (u == v) shared++;
            }
        }
        return shared == 2;
    }

    private Map<Integer, List<Integer>> computeVertexToTriangleVisibility(
            List<Point> pts, List<Triangle> triangles) {
        int n = pts.size();
        Map<Integer, List<Integer>> visibility = new HashMap<>();

        for (int v = 0; v < n; v++) {
            List<Integer> covered = new ArrayList<>();
            Point guard = pts.get(v);

            for (int tIdx = 0; tIdx < triangles.size(); tIdx++) {
                Triangle t = triangles.get(tIdx);
                // A vertex always covers any triangle Incident to it
                if (t.getV1() == v || t.getV2() == v || t.getV3() == v) {
                    covered.add(tIdx);
                    continue;
                }

                // Otherwise, check if guard has unobstructed line of sight inside the polygon
                // to the triangle's centroid and all three of its vertices
                Point p1 = pts.get(t.getV1());
                Point p2 = pts.get(t.getV2());
                Point p3 = pts.get(t.getV3());
                Point centroid = new Point(
                        (p1.getX() + p2.getX() + p3.getX()) / 3.0,
                        (p1.getY() + p2.getY() + p3.getY()) / 3.0
                );

                if (isSegmentInsidePolygon(guard, centroid, pts)
                        && isSegmentInsidePolygon(guard, p1, pts)
                        && isSegmentInsidePolygon(guard, p2, pts)
                        && isSegmentInsidePolygon(guard, p3, pts)) {
                    covered.add(tIdx);
                }
            }
            visibility.put(v, covered);
        }
        return visibility;
    }

    private List<Integer> computeGreedySetCover(
            int n, int totalTriangles, Map<Integer, List<Integer>> visibility) {
        Set<Integer> uncovered = new HashSet<>();
        for (int i = 0; i < totalTriangles; i++) uncovered.add(i);

        List<Integer> selectedCameras = new ArrayList<>();

        while (!uncovered.isEmpty()) {
            int bestVertex = -1;
            int maxNewlyCovered = -1;

            for (int v = 0; v < n; v++) {
                List<Integer> canSee = visibility.getOrDefault(v, Collections.emptyList());
                int count = 0;
                for (int tIdx : canSee) {
                    if (uncovered.contains(tIdx)) count++;
                }
                if (count > maxNewlyCovered) {
                    maxNewlyCovered = count;
                    bestVertex = v;
                }
            }

            if (bestVertex == -1 || maxNewlyCovered == 0) {
                break;
            }

            selectedCameras.add(bestVertex);
            for (int tIdx : visibility.get(bestVertex)) {
                uncovered.remove(tIdx);
            }
        }
        return selectedCameras;
    }

    private boolean isSegmentInsidePolygon(Point a, Point b, List<Point> pts) {
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            Point e1 = pts.get(i);
            Point e2 = pts.get((i + 1) % n);

            // Skip edges that share an endpoint with segment (a, b)
            if (samePoint(a, e1) || samePoint(a, e2) || samePoint(b, e1) || samePoint(b, e2)) {
                continue;
            }
            if (segmentsIntersect(a, b, e1, e2)) {
                return false;
            }
        }
        Point mid = new Point((a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0);
        return isPointInsidePolygon(mid, pts);
    }

    private boolean isPointInsidePolygon(Point p, List<Point> pts) {
        boolean inside = false;
        int n = pts.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point pi = pts.get(i);
            Point pj = pts.get(j);
            boolean intersect = ((pi.getY() > p.getY()) != (pj.getY() > p.getY())) &&
                    (p.getX() < (pj.getX() - pi.getX()) * (p.getY() - pi.getY()) / (pj.getY() - pi.getY() + EPSILON) + pi.getX());
            if (intersect) inside = !inside;
        }
        return inside;
    }

    private boolean samePoint(Point a, Point b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY()) < 1e-6;
    }

    private double crossProduct(Point a, Point b, Point c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY())
                - (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    private boolean isPointInTriangle(Point p, Point a, Point b, Point c) {
        double d1 = crossProduct(a, b, p);
        double d2 = crossProduct(b, c, p);
        double d3 = crossProduct(c, a, p);

        boolean hasNeg = (d1 < -EPSILON) || (d2 < -EPSILON) || (d3 < -EPSILON);
        boolean hasPos = (d1 > EPSILON) || (d2 > EPSILON) || (d3 > EPSILON);
        return !(hasNeg && hasPos);
    }

    private boolean segmentsIntersect(Point p1, Point q1, Point p2, Point q2) {
        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        if (o1 != o2 && o3 != o4) return true;
        if (o1 == 0 && onSegment(p1, p2, q1)) return true;
        if (o2 == 0 && onSegment(p1, q2, q1)) return true;
        if (o3 == 0 && onSegment(p2, p1, q2)) return true;
        return o4 == 0 && onSegment(p2, q1, q2);
    }

    private int orientation(Point p, Point q, Point r) {
        double val = (q.getY() - p.getY()) * (r.getX() - q.getX())
                - (q.getX() - p.getX()) * (r.getY() - q.getY());
        if (Math.abs(val) < EPSILON) return 0;
        return (val > 0) ? 1 : 2;
    }

    private boolean onSegment(Point p, Point q, Point r) {
        return q.getX() <= Math.max(p.getX(), r.getX()) + EPSILON &&
                q.getX() >= Math.min(p.getX(), r.getX()) - EPSILON &&
                q.getY() <= Math.max(p.getY(), r.getY()) + EPSILON &&
                q.getY() >= Math.min(p.getY(), r.getY()) - EPSILON;
    }




    private Map<Integer, String> classifyVertices(List<Point> pts) {
        Map<Integer, String> types = new HashMap<>();
        int n = pts.size();
        double signedArea = computeSignedArea(pts);
        
        for (int i = 0; i < n; i++) {
            Point prev = pts.get((i - 1 + n) % n);
            Point curr = pts.get(i);
            Point next = pts.get((i + 1) % n);

            // On HTML Canvas, higher Y value means it is visually "below"
            boolean prevBelow = prev.getY() > curr.getY();
            boolean nextBelow = next.getY() > curr.getY();
            
            // Check if the vertex is convex based on the polygon's winding order
            double cp = crossProduct(prev, curr, next);
            boolean isConvex = (signedArea > 0) ? (cp <= 0) : (cp >= 0);

            if (prevBelow && nextBelow) {
                types.put(i, isConvex ? "START" : "SPLIT");
            } else if (!prevBelow && !nextBelow) {
                types.put(i, isConvex ? "END" : "MERGE");
            } else {
                types.put(i, "REGULAR");
            }
        }
        return types;
    }
}
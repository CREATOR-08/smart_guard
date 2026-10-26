package com.smartguard.algorithms;

import com.smartguard.geometry.GeometryUtils;
import com.smartguard.model.Point;
import java.util.List;

public class PolygonValidator {
    public static String checkSelfIntersections(List<Point> pts) {
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            Point a1 = pts.get(i);
            Point a2 = pts.get((i + 1) % n);
            for (int j = i + 2; j < n; j++) {
                if (i == 0 && j == n - 1) {
                    continue;
                }
                Point b1 = pts.get(j);
                Point b2 = pts.get((j + 1) % n);
                if (GeometryUtils.segmentsIntersect(a1, a2, b1, b2)) {
                    return String.format("Invalid polygon: Edge P%d-P%d intersects Edge P%d-P%d",
                            i + 1, ((i + 1) % n) + 1, j + 1, ((j + 1) % n) + 1);
                }
            }
        }
        return null;
    }
}
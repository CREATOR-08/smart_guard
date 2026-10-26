package com.smartguard.algorithms;

import com.smartguard.model.Point;
import com.smartguard.model.Triangle;

import java.util.List;

public final class PolygonTriangulator {

    private PolygonTriangulator() {
    }

    public static List<Triangle> monotoneTriangulate(List<Point> points, List<int[]> diagonals) {
        return MonotoneTriangulator.triangulate(points, diagonals);
    }

    @Deprecated
    public static List<Triangle> earClippingTriangulate(
            List<Point> points, double signedArea, List<int[]> diagonals) {
        return monotoneTriangulate(points, diagonals);
    }
}

package com.smartguard.algorithms;

import com.smartguard.geometry.GeometryUtils;
import com.smartguard.model.Point;

import java.util.List;

public final class VertexClassifier {

    public enum VertexType {
        START,
        END,
        SPLIT,
        MERGE,
        REGULAR
    }

    private VertexClassifier() {
        // Utility class.
    }

    public static VertexType[] classifyVertices(List<Point> points) {

        if (points == null || points.size() < 3) {
            throw new IllegalArgumentException(
                    "A polygon must contain at least 3 vertices."
            );
        }

        for (Point point : points) {
            if (point == null || !Double.isFinite(point.getX()) || !Double.isFinite(point.getY())) {
                throw new IllegalArgumentException(
                        "Polygon vertices must have finite coordinates."
                );
            }
        }

        int n = points.size();
        double signedArea = GeometryUtils.computeSignedArea(points);

        if (Math.abs(signedArea) < GeometryUtils.EPSILON) {
            throw new IllegalArgumentException(
                    "Cannot classify vertices of a zero-area polygon."
            );
        }

        VertexType[] types = new VertexType[n];

        for (int i = 0; i < n; i++) {
            int previous = (i - 1 + n) % n;
            int next = (i + 1) % n;
            Point prev = points.get(previous);
            Point curr = points.get(i);
            Point following = points.get(next);
            double turn = GeometryUtils.crossProduct(prev, curr, following);
            if (Math.abs(turn) <= GeometryUtils.EPSILON) {
                types[i] = VertexType.REGULAR;
                continue;
            }

            boolean convex = signedArea > 0
                    ? turn > GeometryUtils.EPSILON
                    : turn < -GeometryUtils.EPSILON;
            boolean isTop = compareSweepOrder(curr, prev) < 0
                    && compareSweepOrder(curr, following) < 0;
            boolean isBottom = compareSweepOrder(prev, curr) < 0
                    && compareSweepOrder(following, curr) < 0;

            types[i] = isTop
                    ? (convex ? VertexType.START : VertexType.SPLIT)
                    : isBottom
                    ? (convex ? VertexType.END : VertexType.MERGE)
                    : VertexType.REGULAR;
        }

        return types;
    }

    private static int compareSweepOrder(Point a, Point b) {
        int yOrder = Double.compare(a.getY(), b.getY());
        return yOrder != 0 ? yOrder : Double.compare(a.getX(), b.getX());
    }
}
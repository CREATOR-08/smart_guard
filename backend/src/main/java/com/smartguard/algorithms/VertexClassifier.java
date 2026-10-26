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

        int n = points.size();
        double signedArea = GeometryUtils.computeSignedArea(points);

        if (Math.abs(signedArea) < GeometryUtils.EPSILON) {
            throw new IllegalArgumentException(
                    "Cannot classify vertices of a zero-area polygon."
            );
        }

        VertexType[] types = new VertexType[n];

        for (int i = 0; i < n; i++) {

            Point prev = points.get((i - 1 + n) % n);
            Point curr = points.get(i);
            Point next = points.get((i + 1) % n);

            double cross = GeometryUtils.crossProduct(prev, curr, next);
            if (Math.abs(cross) <= GeometryUtils.EPSILON) {
                types[i] = VertexType.REGULAR;
                continue;
            }

            boolean convex = cross * signedArea > 0;
            boolean abovePrev = compareSweepOrder(points, i, (i - 1 + n) % n) < 0;
            boolean aboveNext = compareSweepOrder(points, i, (i + 1) % n) < 0;
            boolean belowPrev = compareSweepOrder(points, (i - 1 + n) % n, i) < 0;
            boolean belowNext = compareSweepOrder(points, (i + 1) % n, i) < 0;

            if (abovePrev && aboveNext) {

                types[i] = convex
                        ? VertexType.START
                        : VertexType.SPLIT;

            } else if (belowPrev && belowNext) {

                types[i] = convex
                        ? VertexType.END
                        : VertexType.MERGE;

            } else {

                types[i] = VertexType.REGULAR;
            }
        }

        return types;
    }

    private static int compareSweepOrder(List<Point> points, int indexA, int indexB) {
        Point a = points.get(indexA);
        Point b = points.get(indexB);
        int yOrder = Double.compare(b.getY(), a.getY());
        if (yOrder != 0) {
            return yOrder;
        }

        int xOrder = Double.compare(a.getX(), b.getX());
        if (xOrder != 0) {
            return xOrder;
        }
        return Integer.compare(indexA, indexB);
    }
}
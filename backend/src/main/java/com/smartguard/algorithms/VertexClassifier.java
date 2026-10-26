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

            // Normalize the turn direction for either polygon orientation.
            double cross = GeometryUtils.crossProduct(prev, curr, next);

            boolean convex = signedArea > 0
                    ? cross > GeometryUtils.EPSILON
                    : cross < -GeometryUtils.EPSILON;

            // The sweep processes higher vertices before lower vertices.
            boolean abovePrev = isAbove(curr, prev);
            boolean aboveNext = isAbove(curr, next);

            boolean belowPrev = isAbove(prev, curr);
            boolean belowNext = isAbove(next, curr);

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

    /**
     * Determines sweep-line order.
     * Higher y comes first; for equal y, smaller x comes first.
     */
    private static boolean isAbove(Point a, Point b) {

        if (a.getY() > b.getY() + GeometryUtils.EPSILON) {
            return true;
        }

        if (Math.abs(a.getY() - b.getY())
                <= GeometryUtils.EPSILON) {

            return a.getX() < b.getX() - GeometryUtils.EPSILON;
        }

        return false;
    }
}
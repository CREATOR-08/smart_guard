
package com.smartguard.geometry;

import com.smartguard.algorithms.VertexClassifier;
import com.smartguard.model.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeometryUtils {

    public static final double EPSILON = 1e-9;

    public static double computeSignedArea(List<Point> pts) {
        if (pts == null || pts.size() < 3) {
            return 0.0;
        }

        double sum = 0.0;
        int n = pts.size();

        for (int i = 0; i < n; i++) {
            Point curr = pts.get(i);
            Point next = pts.get((i + 1) % n);

            sum += curr.getX() * next.getY()
                    - next.getX() * curr.getY();
        }

        return sum * 0.5;
    }

    public static boolean isSegmentInsidePolygon(
            Point a, Point b, List<Point> pts) {

        if (a == null || b == null || pts == null || pts.size() < 3) {
            return false;
        }

        int n = pts.size();

        for (int i = 0; i < n; i++) {
            Point e1 = pts.get(i);
            Point e2 = pts.get((i + 1) % n);

            if (samePoint(a, e1) || samePoint(a, e2)
                    || samePoint(b, e1) || samePoint(b, e2)) {
                continue;
            }

            if (segmentsIntersect(a, b, e1, e2)) {
                return false;
            }
        }

        Point midpoint = new Point(
                (a.getX() + b.getX()) / 2.0,
                (a.getY() + b.getY()) / 2.0
        );

        return isPointInsidePolygon(midpoint, pts);
    }

    public static boolean isPointInsidePolygon(
            Point p, List<Point> pts) {

        if (p == null || pts == null || pts.size() < 3) {
            return false;
        }

        boolean inside = false;
        int n = pts.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point pi = pts.get(i);
            Point pj = pts.get(j);

            boolean crossesHorizontalRay =
                    (pi.getY() > p.getY()) != (pj.getY() > p.getY());

            if (crossesHorizontalRay) {
                double intersectionX =
                        (pj.getX() - pi.getX())
                                * (p.getY() - pi.getY())
                                / (pj.getY() - pi.getY())
                                + pi.getX();

                if (p.getX() < intersectionX) {
                    inside = !inside;
                }
            }
        }

        return inside;
    }

    public static boolean samePoint(Point a, Point b) {
        if (a == null || b == null) {
            return false;
        }

        return Math.hypot(
                a.getX() - b.getX(),
                a.getY() - b.getY()
        ) < 1e-6;
    }

    public static double crossProduct(
            Point a, Point b, Point c) {

        return (b.getX() - a.getX())
                * (c.getY() - a.getY())
                - (b.getY() - a.getY())
                * (c.getX() - a.getX());
    }

    public static boolean isPointInTriangle(
            Point p, Point a, Point b, Point c) {

        double d1 = crossProduct(a, b, p);
        double d2 = crossProduct(b, c, p);
        double d3 = crossProduct(c, a, p);

        boolean hasNeg =
                d1 < -EPSILON
                        || d2 < -EPSILON
                        || d3 < -EPSILON;

        boolean hasPos =
                d1 > EPSILON
                        || d2 > EPSILON
                        || d3 > EPSILON;

        return !(hasNeg && hasPos);
    }

    public static boolean segmentsIntersect(
            Point p1, Point q1, Point p2, Point q2) {

        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        if (o1 != o2 && o3 != o4) {
            return true;
        }

        if (o1 == 0 && onSegment(p1, p2, q1)) {
            return true;
        }

        if (o2 == 0 && onSegment(p1, q2, q1)) {
            return true;
        }

        if (o3 == 0 && onSegment(p2, p1, q2)) {
            return true;
        }

        return o4 == 0 && onSegment(p2, q1, q2);
    }

    public static int orientation(
            Point p, Point q, Point r) {

        double val =
                (q.getY() - p.getY()) * (r.getX() - q.getX())
                        - (q.getX() - p.getX())
                        * (r.getY() - q.getY());

        if (Math.abs(val) < EPSILON) {
            return 0;
        }

        return val > 0 ? 1 : 2;
    }

    public static boolean onSegment(
            Point p, Point q, Point r) {

        return q.getX() <= Math.max(p.getX(), r.getX()) + EPSILON
                && q.getX() >= Math.min(p.getX(), r.getX()) - EPSILON
                && q.getY() <= Math.max(p.getY(), r.getY()) + EPSILON
                && q.getY() >= Math.min(p.getY(), r.getY()) - EPSILON;
    }

    /**
     * Classifies vertices using the dedicated VertexClassifier.
     *
     * Retains the Map<Integer, String> return type for compatibility
     * with existing SmartGuard code.
     */
    public static Map<Integer, String> classifyVertices(
            List<Point> pts) {

        VertexClassifier.VertexType[] classifications =
                VertexClassifier.classifyVertices(pts);

        Map<Integer, String> types = new HashMap<>();

        for (int i = 0; i < classifications.length; i++) {
            types.put(i, classifications[i].name());
        }

        return types;
    }
}

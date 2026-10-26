package com.smartguard.algorithms;

import com.smartguard.geometry.GeometryUtils;
import com.smartguard.model.Point;
import com.smartguard.model.Triangle;
import java.util.ArrayList;
import java.util.List;

public class PolygonTriangulator {
    public static List<Triangle> earClippingTriangulate(List<Point> pts, double signedArea, List<int[]> diagonals) {
        int n = pts.size();
        List<Integer> active = new ArrayList<>(n);
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

    private static boolean isEar(Point a, Point b, Point c,
                                 int idxA, int idxB, int idxC,
                                 List<Integer> active, List<Point> pts) {
        if (GeometryUtils.crossProduct(a, b, c) <= GeometryUtils.EPSILON) {
            return false;
        }

        for (int idx : active) {
            if (idx == idxA || idx == idxB || idx == idxC) continue;
            Point p = pts.get(idx);
            if (GeometryUtils.isPointInTriangle(p, a, b, c)) {
                return false;
            }
        }
        return true;
    }
}

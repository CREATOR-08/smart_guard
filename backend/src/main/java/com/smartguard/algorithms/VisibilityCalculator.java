package com.smartguard.algorithms;

import com.smartguard.geometry.GeometryUtils;
import com.smartguard.model.Point;
import com.smartguard.model.Triangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisibilityCalculator {
    public static Map<Integer, List<Integer>> computeVertexToTriangleVisibility(
            List<Point> pts, List<Triangle> triangles) {
        int n = pts.size();
        Map<Integer, List<Integer>> visibility = new HashMap<>();

        for (int v = 0; v < n; v++) {
            List<Integer> covered = new ArrayList<>();
            Point guard = pts.get(v);

            for (int tIdx = 0; tIdx < triangles.size(); tIdx++) {
                Triangle t = triangles.get(tIdx);
                if (t.getV1() == v || t.getV2() == v || t.getV3() == v) {
                    covered.add(tIdx);
                    continue;
                }

                Point p1 = pts.get(t.getV1());
                Point p2 = pts.get(t.getV2());
                Point p3 = pts.get(t.getV3());
                Point centroid = new Point(
                        (p1.getX() + p2.getX() + p3.getX()) / 3.0,
                        (p1.getY() + p2.getY() + p3.getY()) / 3.0
                );

                if (GeometryUtils.isSegmentInsidePolygon(guard, centroid, pts)
                        && GeometryUtils.isSegmentInsidePolygon(guard, p1, pts)
                        && GeometryUtils.isSegmentInsidePolygon(guard, p2, pts)
                        && GeometryUtils.isSegmentInsidePolygon(guard, p3, pts)) {
                    covered.add(tIdx);
                }
            }
            visibility.put(v, covered);
        }
        return visibility;
    }
}

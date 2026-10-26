package com.smartguard.algorithms;

import com.smartguard.geometry.GeometryUtils;
import com.smartguard.model.Point;
import com.smartguard.model.Triangle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MonotoneTriangulator {

    private MonotoneTriangulator() {
    }

    public static List<Triangle> triangulate(List<Point> points, List<int[]> diagonals) {
        int originalDiagonalCount = diagonals.size();
        List<List<Integer>> pieces = MonotonePartitioner.partition(points, diagonals);
        List<Triangle> triangles = new ArrayList<>();
        Set<Long> addedDiagonals = new HashSet<>();

        for (List<Integer> piece : pieces) {
            if (!isYMonotone(piece, points)) {
                removeAddedDiagonals(diagonals, originalDiagonalCount);
                return earClip(points, diagonals);
            }
            triangulatePiece(piece, points, triangles, diagonals, addedDiagonals);
        }

        if (isCompleteTriangulation(points, triangles)) {
            return triangles;
        }
        removeAddedDiagonals(diagonals, originalDiagonalCount);
        return earClip(points, diagonals);
    }

    private static void removeAddedDiagonals(List<int[]> diagonals, int originalSize) {
        while (diagonals.size() > originalSize) {
            diagonals.remove(diagonals.size() - 1);
        }
    }

    private static boolean isCompleteTriangulation(List<Point> points, List<Triangle> triangles) {
        if (triangles.size() != points.size() - 2) {
            return false;
        }

        double polygonArea = Math.abs(GeometryUtils.computeSignedArea(points));
        double triangleArea = 0.0;
        for (Triangle triangle : triangles) {
            Point a = points.get(triangle.getV1());
            Point b = points.get(triangle.getV2());
            Point c = points.get(triangle.getV3());
            triangleArea += Math.abs(GeometryUtils.crossProduct(a, b, c)) * 0.5;
            Point centroid = new Point(
                    (a.getX() + b.getX() + c.getX()) / 3.0,
                    (a.getY() + b.getY() + c.getY()) / 3.0
            );
            if (!GeometryUtils.isPointInsidePolygon(centroid, points)) {
                return false;
            }
        }
        return Math.abs(polygonArea - triangleArea) <= Math.max(1e-8, polygonArea * 1e-8);
    }

    private static List<Triangle> earClip(List<Point> points, List<int[]> diagonals) {
        int n = points.size();
        double winding = Math.signum(GeometryUtils.computeSignedArea(points));
        List<Integer> active = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            active.add(i);
        }

        List<Triangle> triangles = new ArrayList<>();
        int maxIterations = n * n;
        while (active.size() > 3 && maxIterations-- > 0) {
            boolean clipped = false;
            for (int i = 0; i < active.size(); i++) {
                int previous = active.get((i - 1 + active.size()) % active.size());
                int current = active.get(i);
                int next = active.get((i + 1) % active.size());
                Point a = points.get(previous);
                Point b = points.get(current);
                Point c = points.get(next);
                if (GeometryUtils.crossProduct(a, b, c) * winding <= GeometryUtils.EPSILON) {
                    continue;
                }

                boolean containsVertex = false;
                for (int vertex : active) {
                    if (vertex == previous || vertex == current || vertex == next) {
                        continue;
                    }
                    if (GeometryUtils.isPointInTriangle(points.get(vertex), a, b, c)) {
                        containsVertex = true;
                        break;
                    }
                }
                if (containsVertex) {
                    continue;
                }

                triangles.add(new Triangle(previous, current, next, current));
                addPolygonDiagonal(previous, next, n, diagonals);
                active.remove(i);
                clipped = true;
                break;
            }
            if (!clipped) {
                break;
            }
        }

        if (active.size() == 3) {
            triangles.add(new Triangle(active.get(0), active.get(1), active.get(2), active.get(1)));
        }
        return triangles;
    }

    private static void addPolygonDiagonal(int a, int b, int vertexCount, List<int[]> diagonals) {
        if ((a + 1) % vertexCount == b || (b + 1) % vertexCount == a) {
            return;
        }
        for (int[] diagonal : diagonals) {
            if ((diagonal[0] == a && diagonal[1] == b)
                    || (diagonal[0] == b && diagonal[1] == a)) {
                return;
            }
        }
        diagonals.add(new int[]{a, b});
    }

    private static void triangulatePiece(List<Integer> piece, List<Point> points,
                                         List<Triangle> triangles, List<int[]> diagonals,
                                         Set<Long> addedDiagonals) {
        int size = piece.size();
        if (size < 3) {
            return;
        }

        int topPosition = 0;
        int bottomPosition = 0;
        for (int i = 1; i < size; i++) {
            if (compareVertices(points.get(piece.get(i)), points.get(piece.get(topPosition)),
                    piece.get(i), piece.get(topPosition)) < 0) {
                topPosition = i;
            }
            if (compareVertices(points.get(piece.get(i)), points.get(piece.get(bottomPosition)),
                    piece.get(i), piece.get(bottomPosition)) > 0) {
                bottomPosition = i;
            }
        }

        int[] chains = new int[points.size()];
        int position = topPosition;
        while (position != bottomPosition) {
            chains[piece.get(position)] = 1;
            position = (position + 1) % size;
        }
        chains[piece.get(bottomPosition)] = 2;

        List<Integer> sorted = new ArrayList<>(piece);
        sorted.sort((a, b) -> compareVertices(points.get(a), points.get(b), a, b));

        List<Integer> stack = new ArrayList<>();
        stack.add(sorted.get(0));
        stack.add(sorted.get(1));

        for (int i = 2; i < sorted.size() - 1; i++) {
            int current = sorted.get(i);
            int stackTop = stack.get(stack.size() - 1);
            if (chains[current] != chains[stackTop]) {
                int last = stack.remove(stack.size() - 1);
                while (!stack.isEmpty()) {
                    int next = stack.remove(stack.size() - 1);
                    addTriangle(current, last, next, points, triangles, diagonals, addedDiagonals, piece);
                    last = next;
                }
                stack.add(sorted.get(i - 1));
                stack.add(current);
            } else {
                int last = stack.remove(stack.size() - 1);
                while (!stack.isEmpty()) {
                    int next = stack.get(stack.size() - 1);
                    if (!isDiagonalInsidePiece(current, next, piece, points)) {
                        break;
                    }
                    addTriangle(current, last, next, points, triangles, diagonals, addedDiagonals, piece);
                    last = stack.remove(stack.size() - 1);
                }
                stack.add(last);
                stack.add(current);
            }
        }

        int bottom = sorted.get(sorted.size() - 1);
        int last = stack.remove(stack.size() - 1);
        while (!stack.isEmpty()) {
            int next = stack.remove(stack.size() - 1);
            addTriangle(bottom, last, next, points, triangles, diagonals, addedDiagonals, piece);
            last = next;
        }
    }

    private static void addTriangle(int a, int b, int c, List<Point> points,
                                    List<Triangle> triangles, List<int[]> diagonals,
                                    Set<Long> addedDiagonals, List<Integer> piece) {
        if (Math.abs(GeometryUtils.crossProduct(points.get(a), points.get(b), points.get(c)))
                <= GeometryUtils.EPSILON) {
            return;
        }

        triangles.add(new Triangle(a, b, c, b));
        addDiagonalIfInternal(a, b, piece, diagonals, addedDiagonals);
        addDiagonalIfInternal(b, c, piece, diagonals, addedDiagonals);
        addDiagonalIfInternal(c, a, piece, diagonals, addedDiagonals);
    }

    private static void addDiagonalIfInternal(int a, int b, List<Integer> piece,
                                              List<int[]> diagonals, Set<Long> addedDiagonals) {
        int positionA = piece.indexOf(a);
        int positionB = piece.indexOf(b);
        if (Math.abs(positionA - positionB) == 1
                || Math.abs(positionA - positionB) == piece.size() - 1) {
            return;
        }
        long key = diagonalKey(a, b);
        if (addedDiagonals.add(key)) {
            diagonals.add(new int[]{a, b});
        }
    }

    private static long diagonalKey(int a, int b) {
        int low = Math.min(a, b);
        int high = Math.max(a, b);
        return ((long) low << 32) | (high & 0xffffffffL);
    }

    private static boolean isDiagonalInsidePiece(int a, int b, List<Integer> piece,
                                                  List<Point> points) {
        int positionA = piece.indexOf(a);
        int positionB = piece.indexOf(b);
        if (Math.abs(positionA - positionB) == 1
                || Math.abs(positionA - positionB) == piece.size() - 1) {
            return true;
        }

        Point start = points.get(a);
        Point end = points.get(b);
        for (int vertex : piece) {
            if (vertex == a || vertex == b) {
                continue;
            }
            Point candidate = points.get(vertex);
            if (GeometryUtils.orientation(start, end, candidate) == 0
                    && GeometryUtils.onSegment(start, candidate, end)) {
                return false;
            }
        }

        List<Point> piecePoints = new ArrayList<>(piece.size());
        for (int vertex : piece) {
            piecePoints.add(points.get(vertex));
        }
        return GeometryUtils.isSegmentInsidePolygon(
                points.get(a), points.get(b), piecePoints);
    }

    private static boolean isYMonotone(List<Integer> piece, List<Point> points) {
        if (piece.size() < 3) {
            return false;
        }

        int topPosition = 0;
        int bottomPosition = 0;
        for (int i = 1; i < piece.size(); i++) {
            if (compareVertices(points.get(piece.get(i)), points.get(piece.get(topPosition)),
                    piece.get(i), piece.get(topPosition)) < 0) {
                topPosition = i;
            }
            if (compareVertices(points.get(piece.get(i)), points.get(piece.get(bottomPosition)),
                    piece.get(i), piece.get(bottomPosition)) > 0) {
                bottomPosition = i;
            }
        }

        return chainIsMonotone(piece, points, topPosition, bottomPosition, 1)
                && chainIsMonotone(piece, points, topPosition, bottomPosition, -1);
    }

    private static boolean chainIsMonotone(List<Integer> piece, List<Point> points,
                                           int start, int end, int direction) {
        int position = start;
        double previousY = points.get(piece.get(position)).getY();
        while (position != end) {
            position = (position + direction + piece.size()) % piece.size();
            double currentY = points.get(piece.get(position)).getY();
            if (currentY > previousY + GeometryUtils.EPSILON) {
                return false;
            }
            previousY = currentY;
        }
        return true;
    }

    private static int compareVertices(Point a, Point b, int indexA, int indexB) {
        int yOrder = Double.compare(b.getY(), a.getY());
        if (yOrder != 0) {
            return yOrder;
        }
        int xOrder = Double.compare(a.getX(), b.getX());
        return xOrder != 0 ? xOrder : Integer.compare(indexA, indexB);
    }
}

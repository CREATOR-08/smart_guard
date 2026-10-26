package com.smartguard.algorithms;

import com.smartguard.geometry.GeometryUtils;
import com.smartguard.model.Point;

import java.util.ArrayList;
import java.util.List;

public final class MonotonePartitioner {

    private MonotonePartitioner() {
    }

    public static List<List<Integer>> partition(List<Point> points, List<int[]> diagonals) {
        List<Integer> polygon = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            polygon.add(i);
        }

        List<List<Integer>> pieces = new ArrayList<>();
        if (!partitionPiece(polygon, points, pieces, diagonals, 0)) {
            return List.of();
        }
        return pieces;
    }

    private static boolean partitionPiece(List<Integer> piece, List<Point> points,
                                          List<List<Integer>> pieces, List<int[]> diagonals,
                                          int depth) {
        if (isYMonotone(piece, points)) {
            pieces.add(piece);
            return true;
        }
        if (piece.size() < 4 || depth >= points.size()) {
            return false;
        }

        List<Integer> bestFirst = null;
        List<Integer> bestSecond = null;
        int bestScore = Integer.MAX_VALUE;
        int bestLargestPiece = Integer.MAX_VALUE;

        for (int i = 0; i < piece.size(); i++) {
            for (int j = i + 2; j < piece.size(); j++) {
                if (i == 0 && j == piece.size() - 1) {
                    continue;
                }

                int a = piece.get(i);
                int b = piece.get(j);
                if (!isInternalDiagonal(a, b, piece, points)) {
                    continue;
                }

                List<Integer> first = path(piece, i, j);
                List<Integer> second = path(piece, j, i);
                int score = yExtremaCount(first, points) + yExtremaCount(second, points);
                int largestPiece = Math.max(first.size(), second.size());
                if (score < bestScore || (score == bestScore && largestPiece < bestLargestPiece)) {
                    bestFirst = first;
                    bestSecond = second;
                    bestScore = score;
                    bestLargestPiece = largestPiece;
                }
            }
        }

        if (bestFirst == null) {
            return false;
        }

        int initialPieceCount = pieces.size();
        int initialDiagonalCount = diagonals.size();
        int[] diagonal = {bestFirst.get(bestFirst.size() - 1), bestSecond.get(bestSecond.size() - 1)};
        diagonals.add(diagonal);
        if (!partitionPiece(bestFirst, points, pieces, diagonals, depth + 1)
                || !partitionPiece(bestSecond, points, pieces, diagonals, depth + 1)) {
            while (pieces.size() > initialPieceCount) {
                pieces.remove(pieces.size() - 1);
            }
            while (diagonals.size() > initialDiagonalCount) {
                diagonals.remove(diagonals.size() - 1);
            }
            return false;
        }
        return true;
    }

    static boolean isYMonotone(List<Integer> piece, List<Point> points) {
        double topY = Double.NEGATIVE_INFINITY;
        double bottomY = Double.POSITIVE_INFINITY;
        for (int vertex : piece) {
            topY = Math.max(topY, points.get(vertex).getY());
            bottomY = Math.min(bottomY, points.get(vertex).getY());
        }

        for (int top = 0; top < piece.size(); top++) {
            if (Math.abs(points.get(piece.get(top)).getY() - topY) > GeometryUtils.EPSILON) {
                continue;
            }
            for (int bottom = 0; bottom < piece.size(); bottom++) {
                if (Math.abs(points.get(piece.get(bottom)).getY() - bottomY) <= GeometryUtils.EPSILON
                        && chainIsMonotone(piece, points, top, bottom, 1)
                        && chainIsMonotone(piece, points, top, bottom, -1)) {
                    return true;
                }
            }
        }
        return false;
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

    private static int yExtremaCount(List<Integer> piece, List<Point> points) {
        List<Integer> directions = new ArrayList<>(piece.size());
        for (int i = 0; i < piece.size(); i++) {
            double delta = points.get(piece.get((i + 1) % piece.size())).getY()
                    - points.get(piece.get(i)).getY();
            directions.add(Math.abs(delta) <= GeometryUtils.EPSILON ? 0 : delta > 0 ? 1 : -1);
        }

        int extrema = 0;
        for (int i = 0; i < directions.size(); i++) {
            if (directions.get(i) == 0) {
                continue;
            }
            int next = (i + 1) % directions.size();
            while (directions.get(next) == 0 && next != i) {
                next = (next + 1) % directions.size();
            }
            if (next != i && !directions.get(i).equals(directions.get(next))) {
                extrema++;
            }
        }
        return extrema;
    }

    private static boolean isInternalDiagonal(int a, int b, List<Integer> piece, List<Point> points) {
        int positionA = piece.indexOf(a);
        int positionB = piece.indexOf(b);
        if (Math.abs(positionA - positionB) == 1
                || Math.abs(positionA - positionB) == piece.size() - 1) {
            return false;
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
        return GeometryUtils.isSegmentInsidePolygon(start, end, piecePoints);
    }

    private static List<Integer> path(List<Integer> piece, int start, int end) {
        List<Integer> path = new ArrayList<>();
        int position = start;
        while (true) {
            path.add(piece.get(position));
            if (position == end) {
                return path;
            }
            position = (position + 1) % piece.size();
        }
    }
}

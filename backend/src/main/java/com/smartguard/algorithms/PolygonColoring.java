package com.smartguard.algorithms;

import com.smartguard.model.Triangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PolygonColoring {
    public static int[] threeColorPolygon(int n, List<Triangle> triangles) {
        int[] colors = new int[n];
        Arrays.fill(colors, -1);

        if (triangles == null || triangles.isEmpty()) return colors;

        int tCount = triangles.size();
        List<List<Integer>> adj = new ArrayList<>(tCount);
        for (int i = 0; i < tCount; i++) {
            adj.add(new ArrayList<>());
        }

        for (int i = 0; i < tCount; i++) {
            for (int j = i + 1; j < tCount; j++) {
                if (sharesEdge(triangles.get(i), triangles.get(j))) {
                    adj.get(i).add(j);
                    adj.get(j).add(i);
                }
            }
        }

        boolean[] visited = new boolean[tCount];
        Queue<Integer> queue = new LinkedList<>();

        Triangle first = triangles.get(0);
        colors[first.getV1()] = 0;
        colors[first.getV2()] = 1;
        colors[first.getV3()] = 2;
        visited[0] = true;
        queue.offer(0);

        while (!queue.isEmpty()) {
            int currIdx = queue.poll();
            for (int nextIdx : adj.get(currIdx)) {
                if (!visited[nextIdx]) {
                    visited[nextIdx] = true;
                    Triangle t = triangles.get(nextIdx);
                    int[] verts = t.toArray();

                    int uncoloredVertex = -1;
                    int sumColored = 0;
                    int coloredCount = 0;
                    for (int v : verts) {
                        if (colors[v] != -1) {
                            sumColored += colors[v];
                            coloredCount++;
                        } else {
                            uncoloredVertex = v;
                        }
                    }
                    if (coloredCount == 2 && uncoloredVertex != -1) {
                        colors[uncoloredVertex] = 3 - sumColored;
                    }
                    queue.offer(nextIdx);
                }
            }
        }

        for (int i = 0; i < n; i++) {
            if (colors[i] == -1) colors[i] = i % 3;
        }
        return colors;
    }

    private static boolean sharesEdge(Triangle t1, Triangle t2) {
        int shared = 0;
        int[] a = t1.toArray();
        int[] b = t2.toArray();
        for (int u : a) {
            for (int v : b) {
                if (u == v) shared++;
            }
        }
        return shared == 2;
    }
}

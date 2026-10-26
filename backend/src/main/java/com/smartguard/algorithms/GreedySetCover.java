package com.smartguard.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GreedySetCover {
    public static List<Integer> computeGreedySetCover(
            int n, int totalTriangles, Map<Integer, List<Integer>> visibility) {
        Set<Integer> uncovered = new HashSet<>();
        for (int i = 0; i < totalTriangles; i++) {
            uncovered.add(i);
        }

        List<Integer> selectedCameras = new ArrayList<>();

        while (!uncovered.isEmpty()) {
            int bestVertex = -1;
            int maxNewlyCovered = -1;

            for (int v = 0; v < n; v++) {
                List<Integer> canSee = visibility.getOrDefault(v, Collections.emptyList());
                int count = 0;
                for (int tIdx : canSee) {
                    if (uncovered.contains(tIdx)) count++;
                }
                if (count > maxNewlyCovered) {
                    maxNewlyCovered = count;
                    bestVertex = v;
                }
            }

            if (bestVertex == -1 || maxNewlyCovered == 0) {
                break;
            }

            selectedCameras.add(bestVertex);
            for (int tIdx : visibility.get(bestVertex)) {
                uncovered.remove(tIdx);
            }
        }
        return selectedCameras;
    }
}

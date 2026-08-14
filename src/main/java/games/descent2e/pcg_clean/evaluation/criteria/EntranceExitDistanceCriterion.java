package games.descent2e.pcg_clean.evaluation.criteria;

import games.descent2e.pcg_clean.domain.PlacedTile;
import games.descent2e.pcg_clean.evaluation.EvaluationContext;
import games.descent2e.pcg_clean.evaluation.FitnessCriterion;

import java.util.*;

/** Rewards a target shortest-path length through the physical-piece graph. */
public record EntranceExitDistanceCriterion(int targetDistance, double weight) implements FitnessCriterion {
    public EntranceExitDistanceCriterion {
        if (targetDistance < 1) throw new IllegalArgumentException("Target distance must be positive");
    }

    @Override public String name() { return "entrance-exit-distance"; }

    @Override
    public double score(EvaluationContext context) {
        OptionalInt entrance = tileWithPrefix(context, "entrance");
        OptionalInt exit = tileWithPrefix(context, "exit");
        if (entrance.isEmpty() || exit.isEmpty()) return 0;
        int distance = shortestDistance(entrance.getAsInt(), exit.getAsInt(), context);
        return distance < 0 ? 0 : 1.0 / (1.0 + Math.abs(distance - targetDistance) / (double) targetDistance);
    }

    private OptionalInt tileWithPrefix(EvaluationContext context, String prefix) {
        return context.genome().tiles().stream()
                .filter(tile -> tile.tileId().toLowerCase(Locale.ROOT).startsWith(prefix))
                .mapToInt(PlacedTile::instanceId).findFirst();
    }

    private int shortestDistance(int start, int target, EvaluationContext context) {
        Queue<Integer> queue = new ArrayDeque<>();
        Map<Integer, Integer> distance = new HashMap<>();
        queue.add(start);
        distance.put(start, 0);
        while (!queue.isEmpty()) {
            int current = queue.remove();
            if (current == target) return distance.get(current);
            for (int next : context.layout().graph().getOrDefault(current, Set.of())) {
                if (distance.putIfAbsent(next, distance.get(current) + 1) == null) queue.add(next);
            }
        }
        return -1;
    }
}

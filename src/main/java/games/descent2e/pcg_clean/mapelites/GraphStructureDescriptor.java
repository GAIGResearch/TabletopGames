package games.descent2e.pcg_clean.mapelites;

import games.descent2e.pcg_clean.spatial.SpatialCandidate;

import java.util.Map;
import java.util.Set;

/** A 6x5 structural map: branching pieces versus independent graph cycles. */
public final class GraphStructureDescriptor implements BehaviorDescriptor {
    private static final int BRANCH_BINS = 6; // 0, 1, 2, 3, 4, 5+
    private static final int CYCLE_BINS = 5;  // 0, 1, 2, 3, 4+

    @Override
    public EliteCell describe(SpatialCandidate candidate) {
        Map<Integer, Set<Integer>> graph = candidate.evaluation().phenotype().layout().graph();
        int branches = (int) graph.values().stream().filter(neighbours -> neighbours.size() >= 3).count();
        int vertices = graph.size();
        int edges = graph.values().stream().mapToInt(Set::size).sum() / 2;
        int components = components(graph);
        int cycles = Math.max(0, edges - vertices + components);
        return new EliteCell(Math.min(branches, BRANCH_BINS - 1), Math.min(cycles, CYCLE_BINS - 1));
    }

    @Override public int branchingBins() { return BRANCH_BINS; }
    @Override public int cycleBins() { return CYCLE_BINS; }
    @Override public String branchingLabel(int bin) { return bin == BRANCH_BINS - 1 ? bin + "+" : Integer.toString(bin); }
    @Override public String cycleLabel(int bin) { return bin == CYCLE_BINS - 1 ? bin + "+" : Integer.toString(bin); }

    private int components(Map<Integer, Set<Integer>> graph) {
        java.util.Set<Integer> remaining = new java.util.LinkedHashSet<>(graph.keySet());
        int count = 0;
        while (!remaining.isEmpty()) {
            count++;
            visit(remaining.iterator().next(), graph, remaining);
        }
        return count;
    }

    private void visit(int id, Map<Integer, Set<Integer>> graph, java.util.Set<Integer> remaining) {
        if (!remaining.remove(id)) return;
        graph.getOrDefault(id, Set.of()).forEach(next -> visit(next, graph, remaining));
    }
}

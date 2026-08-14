package games.descent2e.pcg_clean.mapelites;

/** Coordinates of one behavioral niche in the MAP-Elites archive. */
public record EliteCell(int branchingBin, int cycleBin) implements Comparable<EliteCell> {
    @Override public int compareTo(EliteCell other) {
        int branch = Integer.compare(branchingBin, other.branchingBin);
        return branch != 0 ? branch : Integer.compare(cycleBin, other.cycleBin);
    }
}

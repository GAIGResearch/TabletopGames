package games.descent2e.pcg_clean.mapelites;

@FunctionalInterface
public interface MapElitesListener {
    void archiveChanged(MapElitesSnapshot snapshot);
    static MapElitesListener none() { return ignored -> {}; }
}

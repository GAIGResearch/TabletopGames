package games.descent2e.pcg_clean.domain;

/** A compact genome entry. Position is derived by BoardLayoutEngine. */
public record PlacedTile(int instanceId, String tileId, int quarterTurns) {
    public PlacedTile { quarterTurns = Math.floorMod(quarterTurns, 4); }
}

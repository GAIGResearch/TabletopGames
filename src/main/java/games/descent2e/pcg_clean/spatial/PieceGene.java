package games.descent2e.pcg_clean.spatial;

/** State of one physical piece. */
public record PieceGene(boolean selected, int face, int x, int y, int quarterTurns) {
    public PieceGene { quarterTurns = Math.floorMod(quarterTurns, 4); }
    public PieceGene withSelected(boolean value) { return new PieceGene(value, face, x, y, quarterTurns); }
    public PieceGene withFace(int value) { return new PieceGene(selected, value, x, y, quarterTurns); }
    public PieceGene withPosition(int newX, int newY) { return new PieceGene(selected, face, newX, newY, quarterTurns); }
    public PieceGene withRotation(int value) { return new PieceGene(selected, face, x, y, value); }
}

package games.ventlife.components;

public enum Terrain {
    BLACK_SMOKER("Black Smoker"), BASALT_RIDGE("Basalt Ridge"), DIFFUSE_VENTS("Diffuse Vents"),
    MICROBIAL_MAT("Microbial Mat");

    public final String label;

    Terrain(String label) {
        this.label = label;
    }

    /**
     * The terrain with the given label, as written in the tile file.
     */
    public static Terrain fromLabel(String label) {
        for (Terrain t : values())
            if (t.label.equalsIgnoreCase(label.trim()))
                return t;
        throw new IllegalArgumentException("Unknown terrain: " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}

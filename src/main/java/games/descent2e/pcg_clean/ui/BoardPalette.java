package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.domain.Cell;

import java.awt.Color;
import java.util.Map;

/** Rendering policy is replaceable without changing board or EA classes. */
public final class BoardPalette {
    private final Map<Cell, Color> colours;

    public BoardPalette(Map<Cell, Color> colours) { this.colours = Map.copyOf(colours); }

    public Color colour(Cell cell) { return colours.getOrDefault(cell, new Color(190, 190, 190)); }

    public static BoardPalette defaultPalette() {
        return new BoardPalette(Map.of(
                Cell.OPEN, new Color(232, 224, 190),
                Cell.PLAIN, new Color(214, 195, 143),
                Cell.WATER, new Color(82, 150, 201),
                Cell.PIT, new Color(79, 66, 58),
                Cell.BLOCK, new Color(105, 105, 105),
                Cell.OTHER, new Color(177, 140, 187)));
    }
}

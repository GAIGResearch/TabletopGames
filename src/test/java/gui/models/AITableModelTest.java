package gui.models;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for {@link AITableModel}, the model behind the GUI's "AI Window". The window is built from
 * each player's decision statistics, whose values may be scalars or per-player {@code double[]}
 * arrays (MCTS reports {@code nodeValue} / {@code heuristicValue} this way). The model must render
 * all of these rather than throwing.
 */
public class AITableModelTest {

    private Map<AbstractAction, Map<String, Object>> statsWith(Map<String, Object> values) {
        Map<AbstractAction, Map<String, Object>> stats = new LinkedHashMap<>();
        stats.put(new DoNothing(), values);
        return stats;
    }

    @Test
    public void rendersArrayValuedStatsAsText() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("visits", 42);
        values.put("visitProportion", 0.5);
        values.put("nodeValue", new double[]{0.52, 0.48});  // the case that used to throw

        AITableModel model = new AITableModel(statsWith(values));

        // one row, plus an Action column and one column per stat
        assertEquals(1, model.getRowCount());
        assertEquals(4, model.getColumnCount());

        int nodeValueCol = columnIndex(model, "nodeValue");
        assertEquals(String.class, model.getColumnClass(nodeValueCol));
        assertEquals("[0.52, 0.48]", model.getValueAt(0, nodeValueCol));
    }

    @Test
    public void keepsScalarStatsTyped() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("visits", 42);
        values.put("visitProportion", 0.5);
        values.put("label", "best");

        AITableModel model = new AITableModel(statsWith(values));

        assertEquals(Integer.class, model.getColumnClass(columnIndex(model, "visits")));
        assertEquals(Double.class, model.getColumnClass(columnIndex(model, "visitProportion")));
        assertEquals(String.class, model.getColumnClass(columnIndex(model, "label")));
        assertEquals(42, model.getValueAt(0, columnIndex(model, "visits")));
    }

    private int columnIndex(AITableModel model, String name) {
        for (int c = 0; c < model.getColumnCount(); c++)
            if (name.equals(model.getColumnName(c)))
                return c;
        throw new AssertionError("No column named " + name);
    }
}

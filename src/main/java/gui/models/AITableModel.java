package gui.models;

import core.actions.AbstractAction;
import org.jetbrains.annotations.Nls;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class AITableModel extends AbstractTableModel {

    List<core.actions.AbstractAction> keys = new ArrayList<>();
    List<String> columnNames = new ArrayList<>();
    List<Class<?>> dataClasses = new ArrayList<>();
    List<List<Object>> dataValues = new ArrayList<>();

    public AITableModel(Map<core.actions.AbstractAction, Map<String, Object>> data) {
        for (AbstractAction action : data.keySet()) {
            keys.add(action);
            Map<String, Object> localData = data.get(action);
            List<Object> actionValues = new ArrayList<>(localData.size());
            for (String dataType : localData.keySet()) {
                // Scalars (Integer/Double/String) display in their own right; anything else - notably
                // the per-player double[] values MCTS reports - is rendered as text so the table can
                // still be shown rather than throwing.
                Object datum = displayValue(localData.get(dataType));
                int index = columnNames.indexOf(dataType);
                if (index > -1) {
                    actionValues.add(index, datum);
                } else {
                    // new item
                    Class<?> klass;
                    if (datum instanceof Integer) {
                        klass = Integer.class;
                    } else if (datum instanceof Double) {
                        klass = Double.class;
                    } else {
                        klass = String.class;
                    }
                    actionValues.add(columnNames.size(), datum);
                    columnNames.add(dataType);
                    dataClasses.add(klass);
                }
            }
            dataValues.add(actionValues);
        }
    }

    /**
     * Keeps scalar values (Integer/Double/String) as-is so they sort and render naturally, and turns
     * everything else into a readable string. Per-player {@code double[]} stats are formatted to two
     * decimal places, e.g. {@code [0.52, 0.48]}.
     */
    private static Object displayValue(Object datum) {
        if (datum instanceof Integer || datum instanceof Double || datum instanceof String)
            return datum;
        if (datum instanceof double[] doubles) {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (double d : doubles)
                joiner.add(String.format("%.2f", d));
            return joiner.toString();
        }
        return String.valueOf(datum);
    }

    @Override
    public int getRowCount() {
        return dataValues.size();
    }

    @Override
    public int getColumnCount() {
        return 1 + columnNames.size();
    }

    @Nls
    @Override
    public String getColumnName(int columnIndex) {
        return columnIndex == 0 ? "Action" : columnNames.get(columnIndex - 1);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? String.class : dataClasses.get(columnIndex - 1);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return keys.get(rowIndex).toString();
        } else {
            return dataValues.get(rowIndex).get(columnIndex - 1);
        }
    }

}

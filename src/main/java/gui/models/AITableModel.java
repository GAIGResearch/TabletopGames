package gui.models;

import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AITableModel extends AbstractTableModel {

    Map<core.actions.AbstractAction, Long> decisionCounts;
    List<core.actions.AbstractAction> keys;
    double N;
    String[] columnNames = new String[]{"Action", "Visits", "Percent"};
    Class<?>[] columnClasses = new Class[]{String.class, int.class, double.class};

    public AITableModel(Map<core.actions.AbstractAction, Long> decisionCounts) {
        this.decisionCounts = decisionCounts;
        N = decisionCounts.values().stream().mapToInt(Long::intValue).sum();
        keys = decisionCounts.entrySet().stream()
                .sorted((e1, e2)  -> (int) (e2.getValue() - e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public int getRowCount() {
        return decisionCounts.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
        // decision, count, percent
    }

    @Nls
    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return keys.get(columnIndex).toString();
            case 1:
                return decisionCounts.get(keys.get(columnIndex)).intValue();
            case 2:
                return ((int) (1000.0 * decisionCounts.get(keys.get(columnIndex)).intValue() / N)) / 10.0;
            default:
                throw new IllegalArgumentException("Unexpected columnIndex " + columnIndex);
        }
    }

}

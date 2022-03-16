package gui.models;

import core.actions.AbstractAction;
import org.jetbrains.annotations.Nls;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                Object datum = localData.get(dataType);
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
                    } else if (datum instanceof String) {
                        klass = String.class;
                    } else {
                        throw new AssertionError("Unknown class for " + datum);
                    }
                    actionValues.add(columnNames.size(), datum);
                    columnNames.add(dataType);
                    dataClasses.add(klass);
                }
            }
            dataValues.add(actionValues);
        }
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

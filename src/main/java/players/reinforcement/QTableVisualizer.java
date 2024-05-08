package players.reinforcement;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class QTableVisualizer {
    private JFrame frame;
    private DefaultTableModel tableModel;

    public QTableVisualizer() {
        frame = new JFrame("Q-Table Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(600, 400));

        
        tableModel = new DefaultTableModel(new Object[] { "Iteration", "State-Action", "Q-Value" }, 0);
        JTable table = new JTable(tableModel);
        frame.add(new JScrollPane(table));
        frame.setVisible(true);
    }

    public void updateQTable(Map<String, Double> qTable, int iteration) {
        
        qTable.forEach((key, value) -> tableModel.addRow(new Object[] { iteration, key, value }));
    }
}

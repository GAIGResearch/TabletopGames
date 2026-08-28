package games.descent2e.pcg;

import utilities.Pair;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;

public class MapElitesGUI {

    private final JFrame window;
    private JPanel panel;
    private int id = 0;

    public MapElitesGUI(CreateOffspring co, int id) {
        window = new JFrame();
        window.setTitle("Descent Procedural Content Generation - Results");
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setSize(1200, 500);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setLayout(new GridLayout(0, 1,5, 5));

        panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1,5, 5));
        panel.setBackground(Color.CYAN);

        this.id = id;

        Border blackline = BorderFactory.createLineBorder(Color.black);

        JPanel information = new JPanel();
        information.setLayout(new BoxLayout(information, BoxLayout.Y_AXIS));
        TitledBorder infoBorder = new TitledBorder(blackline, "Generation Run " + id + ": Created " + co.feasible.size() + " Feasible Boards out of " +
                co.feasibleList.size() + " Board Generations (" + ((float) ((1000 * co.feasible.size() / co.feasibleList.size())) / 10f) + "%)");
        infoBorder.setTitleJustification(TitledBorder.CENTER);
        information.setBorder(infoBorder);
        information.setBackground(Color.CYAN);

        JPanel informationContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        informationContainer.setBackground(Color.CYAN);
        informationContainer.add(new JLabel("Ideal Board Size: " + co.IDEAL_SIZE + ", "));
        informationContainer.add(new JLabel("Ideal Group Count: " + co.IDEAL_GROUP + ", "));
        informationContainer.add(new JLabel("Ideal Average Health: " + co.IDEAL_HEALTH + ", "));
        informationContainer.add(new JLabel("Ideal Board Height: " + co.IDEAL_HEIGHT + ", "));
        informationContainer.add(new JLabel("Ideal Board Width: " + co.IDEAL_WIDTH));

        JPanel weightsContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        weightsContainer.setBackground(Color.CYAN);
        weightsContainer.add(new JLabel("Board Size Weight: " + co.W_SIZE + ", "));
        weightsContainer.add(new JLabel("Group Count Weight: " + co.W_GROUPS + ", "));
        weightsContainer.add(new JLabel("Average Health Weight: " + co.W_HEALTH + ", "));
        weightsContainer.add(new JLabel("Board Height Weight: " + co.W_HEIGHT + ", "));
        weightsContainer.add(new JLabel("Board Width Weight: " + co.W_WIDTH));

        information.add(informationContainer);
        information.add(weightsContainer);
        panel.add(information);

        if (!co.feasible.isEmpty()) {
            JButton feasible = createButton("All Feasible Boards");
            ShowFeasibleBoards list = new ShowFeasibleBoards(co);
            list.prepare(co.feasible, co.feasibleFitness);
            panel.add(feasible);
            feasible.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    list.show();
                    //hide();
                }
            });
        }

        HashMap<Pair<Float, Float>, Pair<Integer, Float>> sizeGroups = co.map_SizeVsGroups;

        if (!sizeGroups.isEmpty()) {
            String name = "Size Vs Groups";
            JButton sizeVSgroups = createButton(name);

            ShowMAPElite elite = new ShowMAPElite(sizeGroups, name);
            elite.prepare(co);

            sizeVSgroups.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    elite.show();
                    //hide();
                }
            });

            panel.add(sizeVSgroups);
        }

        HashMap<Pair<Float, Float>, Pair<Integer, Float>> healthGroups = co.map_HealthVsGroups;

        if (!healthGroups.isEmpty()) {
            String name = "Health Vs Groups";
            JButton healthVSgroups = createButton(name);

            ShowMAPElite elite = new ShowMAPElite(healthGroups, name);
            elite.prepare(co);

            healthVSgroups.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    elite.show();
                    //hide();
                }
            });

            panel.add(healthVSgroups);
        }

        HashMap<Pair<Float, Float>, Pair<Integer, Float>> heightWidth = co.map_HeightVsWidth;

        if (!heightWidth.isEmpty()) {
            String name = "Height Vs Width";
            JButton heightVSwidth = createButton(name);

            ShowMAPElite elite = new ShowMAPElite(heightWidth, name);
            elite.prepare(co);

            heightVSwidth.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    elite.show();
                    //hide();
                }
            });

            panel.add(heightVSwidth);
        }

        JButton close = createButton("Close");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.dispose();
            }
        });

        panel.add(close);

        window.add(panel, BorderLayout.CENTER);
    }

    public void show() {
        window.setVisible(true);
    }

    public void hide() {
        window.setVisible(false);
    }

    public JButton createButton(String label) {
        JButton button = new JButton(label);
        button.setFocusable(false);
        //button.setFont(new Font("Arial", Font.PLAIN, 10));
        return button;
    }
}

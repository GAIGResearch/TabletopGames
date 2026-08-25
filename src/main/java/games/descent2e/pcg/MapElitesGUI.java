package games.descent2e.pcg;

import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;

public class MapElitesGUI {

    private final JFrame window;
    private JPanel panel;

    public MapElitesGUI(CreateOffspring co) {
        window = new JFrame();
        window.setTitle("Descent Procedural Content Generation - Results");
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setSize(1200, 500);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setLayout(new GridLayout(0, 1,10, 10));

        panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1,10, 10));
        panel.setBackground(Color.CYAN);

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

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
        window.setTitle("Descent Procedural Content Generation - MAP Elites");
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setSize(1200, 500);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setLayout(new GridLayout(0, 1,10, 10));

        panel = new JPanel(new GridLayout(0, 1, 10, 10));
        panel.setBackground(Color.CYAN);

        HashMap<Pair<Float, Float>, Pair<Integer, Float>> sizeGroups = co.map_SizeVsGroups;

        if (!sizeGroups.isEmpty()) {
            String name = "Size Vs Groups";
            Button sizeVSgroups = createButton(name);

            ShowMAPElite elite = new ShowMAPElite(sizeGroups, name);
            elite.prepare(co);

            sizeVSgroups.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    elite.show();
                    hide();
                }
            });

            panel.add(sizeVSgroups);
        }

        HashMap<Pair<Float, Float>, Pair<Integer, Float>> healthGroups = co.map_HealthVsGroups;

        if (!healthGroups.isEmpty()) {
            String name = "Health VS Groups";
            Button healthVSgroups = createButton(name);

            ShowMAPElite elite = new ShowMAPElite(healthGroups, name);
            elite.prepare(co);

            healthVSgroups.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    elite.show();
                    hide();
                }
            });

            panel.add(healthVSgroups);
        }

        Button close = createButton("Close");
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

    public Button createButton(String label) {
        Button button = new Button(label);
        button.setFocusable(false);
        button.setFont(new Font("Arial", Font.PLAIN, 10));
        return button;
    }
}

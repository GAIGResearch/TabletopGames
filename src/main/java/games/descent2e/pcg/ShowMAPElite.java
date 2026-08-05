package games.descent2e.pcg;

import core.components.BoardNode;
import core.components.GraphBoard;
import games.descent2e.concepts.Quest;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class ShowMAPElite {
    private final JFrame window;
    private JPanel panel;
    private Pair<Pair<Float, Float>, Pair<Float, Float>> minMax;
    private int xRange;
    private int yRange;
    private float minFitness = Float.MAX_VALUE;
    private float maxFitness = Float.MIN_VALUE;

    private float[][] fitness;
    private int[][] id;

    private final HashMap<Pair<Float, Float>, Pair<Integer, Float>> elite;

    public ShowMAPElite(HashMap<Pair<Float, Float>, Pair<Integer, Float>> elite, String name) {
        window = new JFrame();
        window.setTitle(name);
        window.setSize(1600, 700);
        window.setResizable(true);
        window.setLocationRelativeTo(null);
        window.setLayout(new GridLayout(0,1, 10, 10));

        panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.CYAN);

        this.elite = elite;
    }

    public void show() {
        window.setVisible(true);
    }

    public void hide() {
        window.setVisible(false);
    }

    public void prepare() {
        minMax(elite);

        JPanel grid = new JPanel(new GridLayout(xRange, 1, 0,0));

        System.out.println(xRange + " " + yRange);

        id = new int[xRange][yRange];
        fitness = new float[xRange][yRange];

        for (Pair<Float, Float> key : elite.keySet()) {
            Pair<Integer, Float> values = elite.get(key);
            int x = (int) (key.a - minMax.a.a);
            int y = (int) (key.b - minMax.b.a);

            id[x][y] = values.a;
            fitness[x][y] = values.b;
        }

        int counter = 0;
        for (int i = xRange; i > 0; i--) {
            JPanel row = new JPanel(new GridLayout(1, yRange, 0,0));
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < yRange; j++) {
                counter++;

                int myID = id[i-1][j];
                float fit = fitness[i-1][j];

                line.append(myID).append(" ");

                Button button = new Button(Integer.toString(counter));
                button.setFont(new Font("Arial", Font.PLAIN, 1));

                button.setBackground(getColour(fit));
                row.add(button);

                if (fit > 0f) {

                    Pair<Quest, GraphBoard> quest = GenerateBoards.getQuestByID(myID, true);
                    if (quest != null) {
                        Quest q = quest.a;
                        GraphBoard b = quest.b;

                        ShowMAPElite parent = this;

                        button.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                System.out.println("ID: " + myID);
                                System.out.println("Fitness: " + fit);
                                for (String[] monster : q.getMonsters()) {
                                    System.out.println(Arrays.toString(monster));
                                }
                                for (BoardNode node : b.getBoardNodes()) {
                                    System.out.println(node.getComponentName());
                                }
                                ShowMap map = new ShowMap(q, b, myID, fit, parent);
                                map.show();
                                hide();
                            }
                        });
                    }
                }
                else
                    button.setEnabled(false);
            }
            //System.out.println(line);
            grid.add(row);
        }
        System.out.println();
        for (int i = xRange; i > 0; i--) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < yRange; j++)
                line.append(fitness[i-1][j]).append(" ");
            //System.out.println(line);
        }

        panel.add(grid);
        window.add(panel);

    }

    void minMax(HashMap<Pair<Float, Float>, Pair<Integer, Float>> elite) {
        Pair<Float, Float> x = new Pair<>(Float.MAX_VALUE, Float.MIN_VALUE);
        Pair<Float, Float> y = new Pair<>(Float.MAX_VALUE, Float.MIN_VALUE);

        for (Pair<Float, Float> key : elite.keySet()) {
            if (key.a < x.a)
                x.a = key.a;
            if (key.a > x.b)
                x.b = key.a;
            if (key.b < y.a)
                y.a = key.b;
            if (key.b > y.b)
                y.b = key.b;

            float fitness = elite.get(key).b;

            if (fitness < minFitness)
                minFitness = fitness;
            if (fitness > maxFitness)
                maxFitness = fitness;

        }

        System.out.println("X Range: " + x.a + "-" + x.b);
        System.out.println("Y Range: " + y.a + "-" + y.b);

        minMax = new Pair<>(x, y);

        // Ranges are inclusive, so 1 larger
        xRange = (int) (x.b - x.a + 1);
        yRange = (int) (y.b - y.a + 1);
    }

    private Color getColour(double fitness) {
        if (fitness < minFitness)
            return Color.WHITE;

        if (maxFitness == minFitness)
            return Color.RED;

        return interpolate((fitness - minFitness) / (maxFitness - minFitness));
    }

    // Cycle through Blue, Cyan, Green, Yellow, Red
    private Color interpolate(double t) {

        if (t < 0.25) {
            return blend(Color.BLUE, Color.CYAN, t / 0.25);
        } else if (t < 0.5) {
            return blend(Color.CYAN, Color.GREEN, (t - 0.25) / 0.25);
        } else if (t < 0.75) {
            return blend(Color.GREEN, Color.YELLOW, (t - 0.5) / 0.25);
        } else {
            return blend(Color.YELLOW, Color.RED, (t - 0.75) / 0.25);
        }
    }

    private Color blend(Color x, Color y, double t) {
        int r = (int)((x.getRed() * (1 - t)) + (y.getRed() * t));
        int g = (int)((x.getGreen() * (1 - t)) + (y.getGreen() * t));
        int b = (int)((x.getBlue() * (1 - t)) + (y.getBlue() * t));
        return new Color(r, g, b);
    }
}

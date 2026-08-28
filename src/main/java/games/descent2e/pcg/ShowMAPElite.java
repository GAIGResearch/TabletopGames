package games.descent2e.pcg;

import core.components.BoardNode;
import core.components.GraphBoard;
import games.descent2e.concepts.Quest;
import utilities.Pair;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;

public class ShowMAPElite {
    private final JFrame window;
    private JPanel panel;
    private JScrollPane scroll;
    private Pair<Pair<Float, Float>, Pair<Float, Float>> minMax; // xMin, xMax; yMin, yMax
    private int xRange;
    private int yRange;
    private float minFitness = Float.MAX_VALUE;
    private float maxFitness = Float.MIN_VALUE;
    private float absoluteFitness = 10f;

    private String xLabel;
    private String yLabel;

    private float[][] fitness;
    private int[][] id;

    private final HashMap<Pair<Float, Float>, Pair<Integer, Float>> elite;

    public ShowMAPElite(HashMap<Pair<Float, Float>, Pair<Integer, Float>> elite, String name) {
        window = new JFrame();
        window.setTitle(name);
        window.setSize(1400, 700);
        window.setResizable(true);
        window.setLocationRelativeTo(null);
        window.setLayout(new BorderLayout());

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBackground(Color.CYAN);

        this.elite = elite;

        xLabel = name.split(" Vs ")[0];
        yLabel = name.split(" Vs ")[1];
    }

    public void show() {
        window.setVisible(true);
    }

    public void hide() {
        window.setVisible(false);
    }

    public void prepare(CreateOffspring co) {
        minMax(elite);

        Color[] colours = MapColours.getColourStops(MapColours.Colours.Viridis);

        int squareSize = 40;
        Dimension gridSize = new Dimension(squareSize, squareSize);

        JPanel grid = new JPanel();
        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
        grid.setBackground(Color.WHITE);
        grid.setBorder(BorderFactory.createLineBorder(Color.black));

        //System.out.println(xRange + " " + yRange);

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
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setBackground(Color.WHITE);

            String labelName = String.valueOf(i - 1 + minMax.a.a.intValue());
            if (xRange == 1 || i == xRange / 2) {
                labelName = xLabel + " " + labelName;
            }

            JPanel label = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            label.add(new JLabel(labelName));
            label.setMinimumSize(gridSize);
            label.setMaximumSize(gridSize);
            label.setBackground(Color.WHITE);
            row.add(label);
            for (int j = 0; j < yRange; j++) {
                counter++;

                int myID = id[i-1][j];
                float fit = fitness[i-1][j];

                JButton button = new JButton(String.valueOf(myID));
                button.setFont(button.getFont().deriveFont(Font.BOLD, 4));
                button.setForeground(Color.WHITE);
                button.setMaximumSize(gridSize);
                button.setMinimumSize(gridSize);

                button.setBackground(getColour(fit, colours));
                row.add(button);

                if (fit > 0f) {

                    Pair<Quest, GraphBoard> quest = co.getQuestByID(myID, true);
                    HashMap<String, Float> scores = co.getScoresByID(myID, true);
                    if (quest != null) {
                        Quest q = quest.a;
                        GraphBoard b = quest.b;

                        ShowMAPElite parent = this;

                        button.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                //System.out.println("ID: " + myID);
                                //System.out.println("Fitness: " + fit);
                                for (String[] monster : q.getMonsters()) {
                                    //System.out.println(Arrays.toString(monster));
                                }
                                for (BoardNode node : b.getBoardNodes()) {
                                    //System.out.println(node.getComponentName());
                                }
                                ShowMap map = null;
                                try {
                                    map = new ShowMap(co, q, b, myID, scores, parent, null);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
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
        JPanel yAxis = new JPanel();
        yAxis.setLayout(new BoxLayout(yAxis, BoxLayout.X_AXIS));
        yAxis.setBackground(Color.WHITE);
        JLabel blank = new JLabel("");
        blank.setBackground(Color.WHITE);
        blank.setForeground(Color.WHITE);
        blank.setPreferredSize(gridSize);
        blank.setMaximumSize(gridSize);
        blank.setMinimumSize(gridSize);
        yAxis.add(blank);
        for (int i = minMax.b.a.intValue(); i <= minMax.b.b.intValue(); i++) {
            JPanel axisContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
            axisContainer.setBackground(Color.WHITE);
            JLabel axisLabel = new JLabel(String.valueOf(i));
            axisContainer.setPreferredSize(gridSize);
            axisContainer.setMinimumSize(gridSize);
            axisContainer.setMaximumSize(gridSize);
            axisContainer.add(axisLabel);
            yAxis.add(axisContainer);
        }
        grid.add(yAxis);

        JPanel yAxisNameContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        yAxisNameContainer.setBackground(Color.WHITE);
        JLabel yAxisName = new JLabel(yLabel);
        yAxisName.setBackground(Color.WHITE);
        yAxisNameContainer.add(yAxisName);
        grid.add(yAxisNameContainer);


        panel.add(grid);
        scroll = new JScrollPane(panel);
        window.add(scroll, BorderLayout.CENTER);

        Border blackline = BorderFactory.createLineBorder(Color.black);

        JPanel legendContainer = new JPanel();
        legendContainer.setBackground(Color.WHITE);
        legendContainer.setLayout(new BoxLayout(legendContainer, BoxLayout.Y_AXIS));
        legendContainer.setMaximumSize(new Dimension(window.getWidth(), 60));
        TitledBorder legendBorder = new TitledBorder(blackline, "Fitness Legend");
        legendBorder.setTitleJustification(TitledBorder.CENTER);
        legendContainer.setBorder(legendBorder);
        JPanel legendBox = new JPanel(new FlowLayout(FlowLayout.CENTER));
        legendBox.setPreferredSize(new Dimension(1000, 30));
        legendBox.setMaximumSize(new Dimension(1000, 30));
        legendBox.setBackground(Color.WHITE);

        int columns = 100;
        JPanel legend = new JPanel(new GridLayout(1, columns, 0, 0));
        legend.setPreferredSize(new Dimension(1000, 15));
        legend.setMaximumSize(new Dimension(1000, 15));
        legend.setBackground(Color.WHITE);
        legend.setBorder(blackline);
        for (int i = 0; i < columns; i++) {
            JPanel legendPiece = new JPanel();
            legendPiece.setBackground(interpolate((float) i / columns, colours));
            legend.add(legendPiece);
        }
        legendBox.add(legend);
        legendContainer.add(legendBox);

        JPanel legendLabelContainer = new JPanel(new FlowLayout());
        legendLabelContainer.setPreferredSize(new Dimension(1020, 30));
        legendLabelContainer.setMaximumSize(new Dimension(1020, 30));
        legendLabelContainer.setBackground(Color.WHITE);
        JPanel legendLabelContainer2 = new JPanel(new GridLayout(1, 0, 5, 5));
        legendLabelContainer2.setBackground(Color.WHITE);
        legendLabelContainer2.setPreferredSize(new Dimension(1020, 30));
        legendLabelContainer2.setMaximumSize(new Dimension(1020, 30));

        JPanel labelContainerStart = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelContainerStart.add(new JLabel(Float.toString(absoluteFitness / 2)));
        labelContainerStart.setBackground(Color.WHITE);
        legendLabelContainer2.add(labelContainerStart);

        int labelCount = 5;
        for (int i = 1; i < labelCount; i++) {
            JPanel labelContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
            labelContainer.add(new JLabel(Float.toString(labelCount + i * (absoluteFitness / (2 * labelCount)))));
            labelContainer.setBackground(Color.WHITE);
            //labelContainer.setBorder(blackline);
            legendLabelContainer2.add(labelContainer);
        }

        JPanel labelContainerEnd = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        labelContainerEnd.add(new JLabel(Float.toString(absoluteFitness)));
        labelContainerEnd.setBackground(Color.WHITE);
        legendLabelContainer2.add(labelContainerEnd);

        /*legendLabelContainer2.add(new JLabel(Integer.toString((int) (absoluteFitness / 2))));
        legendLabelContainer2.add(new JLabel(Float.toString(5 * (absoluteFitness / 8))));
        legendLabelContainer2.add(new JLabel(Float.toString(3 * (absoluteFitness / 4))));
        legendLabelContainer2.add(new JLabel(Float.toString(7 * (absoluteFitness / 8))));
        legendLabelContainer2.add(new JLabel(Integer.toString((int) absoluteFitness)));*/
        legendLabelContainer.add(legendLabelContainer2);
        legendContainer.add(legendLabelContainer);

        window.add(legendContainer, BorderLayout.PAGE_END);

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
            if (fitness > absoluteFitness)
                absoluteFitness = fitness;

        }

        //System.out.println("X Range: " + x.a + "-" + x.b);
        //System.out.println("Y Range: " + y.a + "-" + y.b);

        minMax = new Pair<>(x, y);

        // Ranges are inclusive, so 1 larger
        xRange = (int) (x.b - x.a + 1);
        yRange = (int) (y.b - y.a + 1);
    }

    private Color getColour(double fitness, Color[] colours) {
        if (fitness <= 0f)
            return Color.BLACK;
        if (fitness < minFitness)
            return colours[0];

        if (maxFitness == minFitness)
            return colours[colours.length-1];

        return interpolate((fitness - (absoluteFitness / 2)) / (absoluteFitness), colours);
    }

    private Color interpolate(double t, Color[] colours) {

        if (t < 0.25) {
            return blend(colours[0], colours[1], t / 0.25);
        } else if (t < 0.5) {
            return blend(colours[1], colours[2], (t - 0.25) / 0.25);
        } else if (t < 0.75) {
            return blend(colours[2], colours[3], (t - 0.5) / 0.25);
        } else {
            return blend(colours[3], colours[4], (t - 0.75) / 0.25);
        }
    }

    private Color blend(Color x, Color y, double t) {
        int r = Math.min(255, Math.max(0, (int)((x.getRed() * (1 - t)) + (y.getRed() * t))));
        int g = Math.min(255, Math.max(0, (int)((x.getGreen() * (1 - t)) + (y.getGreen() * t))));
        int b = Math.min(255, Math.max(0, (int)((x.getBlue() * (1 - t)) + (y.getBlue() * t))));
        return new Color(r, g, b);
    }
}

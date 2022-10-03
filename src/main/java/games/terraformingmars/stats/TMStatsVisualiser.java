package games.terraformingmars.stats;

import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
import games.terraformingmars.TMGameState;
import games.terraformingmars.gui.BarPlot;
import games.terraformingmars.gui.DotPlot;
import gui.TiledImage;
import utilities.ImageIO;
import utilities.SummaryLogger;
import utilities.TAGStatSummary;
import utilities.TAGStringStatSummary;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TMStatsVisualiser extends JFrame {
    SummaryLogger[] playerLoggers;
    SummaryLogger playerAggregateLogger;
    SummaryLogger gameLogger;

    public static int fontSize = 16;
    public static Font defaultFont = new Font("Prototype", Font.BOLD, fontSize);
    public static Font defaultFontSmall = new Font("Prototype", Font.BOLD, fontSize-5);
    public static Font defaultFontLarge = new Font("Prototype", Font.BOLD, fontSize+5);
    public static Color fontColor = Color.white;
    static Color bgColor = Color.black;
    static Color grayColor = Color.gray;
    static Color lightGrayColor = Color.lightGray;
    static Color darkGrayColor = Color.darkGray;

    public TMStatsVisualiser(List<IGameListener> listeners) {
        for (IGameListener list: listeners) {
            if (list instanceof TMGameListener) {
                this.gameLogger = (SummaryLogger) ((TMGameListener) list).logger;
            } else if (list instanceof TMPlayerListener) {
                this.playerLoggers = (SummaryLogger[]) ((TMPlayerListener) list).logger;
                this.playerAggregateLogger = (SummaryLogger) ((TMPlayerListener) list).aggregate;
            }
        }

        BufferedImage bg = (BufferedImage) ImageIO.GetInstance().getImage("data/terraformingmars/images/stars.jpg");
        TexturePaint space = new TexturePaint(bg, new Rectangle2D.Float(0,0, bg.getWidth(), bg.getHeight()));
        TiledImage backgroundImage = new TiledImage(space);
        setContentPane(backgroundImage);

        try {
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("data/terraformingmars/images/fonts/Prototype.ttf")));
        } catch (IOException | FontFormatException e) {
            //Handle exception
        }
        UIManager.put("Label.foreground", fontColor);
        UIManager.put("Label.font", defaultFont);

        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;

        // Generation average
        TAGStatSummary stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.GENERATION.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Generation:", stats.mean(), stats.sd())), c);
        c.gridy++;
        getContentPane().add(new DotPlot(stats.getElements().toArray(new Double[0]), 0, 30), c);

        // Global parameters
        c.gridy++;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy++;
        getContentPane().add(new JLabel("Generation global parameter was completed:"), c);
        c.gridy++;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.GP_OCEAN_COMPLETE_GEN.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Ocean:", stats.mean(), stats.sd())), c);
        c.gridy++;
        getContentPane().add(new DotPlot(stats.getElements().toArray(new Double[0]), 0, 30), c);
        c.gridy++;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.GP_OXYGEN_COMPLETE_GEN.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Oxygen:", stats.mean(), stats.sd())), c);
        c.gridy++;
        getContentPane().add(new DotPlot(stats.getElements().toArray(new Double[0]), 0, 30), c);
        c.gridy++;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.GP_TEMPERATURE_COMPLETE_GEN.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Temperature:", stats.mean(), stats.sd())), c);
        c.gridy++;
        getContentPane().add(new DotPlot(stats.getElements().toArray(new Double[0]), 0, 30), c);

        // Resource averages
        c.gridy++;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy++;
        getContentPane().add(new JLabel("Resources at the end:"), c);
        c.gridy++;
        double[] resources = new double[] {
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.MEGACREDIT.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.STEEL.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.TITANIUM.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.PLANT.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.ENERGY_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.HEAT.name()).mean(),
        };
        Image[] xTickImages = new Image[] {
                ImageIO.GetInstance().getImage("data/terraformingmars/images/megacredits/megacredit.png"),
                ImageIO.GetInstance().getImage("data/terraformingmars/images/resources/steel.png"),
                ImageIO.GetInstance().getImage("data/terraformingmars/images/resources/titanium.png"),
                ImageIO.GetInstance().getImage("data/terraformingmars/images/resources/plant.png"),
                ImageIO.GetInstance().getImage("data/terraformingmars/images/resources/power.png"),
                ImageIO.GetInstance().getImage("data/terraformingmars/images/resources/heat.png"),
        };
        BarPlot barPlot1 = new BarPlot(resources, "Amount", "Resource");
        barPlot1.setxTicks(new String[]{"MC", "Steel", "Titanium", "Plant", "Energy", "Heat"});
        barPlot1.setxTickImages(xTickImages);
        getContentPane().add(barPlot1, c);
        c.gridy++;
        getContentPane().add(new JLabel("Resource production at the end:"), c);
        c.gridy++;
        resources = new double[] {
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.MEGACREDIT_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.STEEL_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.TITANIUM_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.PLANT_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.ENERGY_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.HEAT_PROD.name()).mean(),
        };
        BarPlot barPlot2 = new BarPlot(resources, "Amount", "Resource");
        barPlot2.setxTicks(new String[]{"MC", "Steel", "Titanium", "Plant", "Energy", "Heat"});
        barPlot2.setxTickImages(xTickImages);
        getContentPane().add(barPlot2, c);

        // Map coverage
        c.gridx++;
        c.gridy = 0;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.MAP_COVERAGE.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g%% +/- %6.2g", "Map coverage:", stats.mean()*100, stats.sd()*100)), c);
        // TODO heatmap
        c.gridy++;

        // Score gap
        c.gridy++;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy++;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.POINT_DIFF.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Score gap:", stats.mean(), stats.sd())), c);

        // Card stats
        c.gridy++;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy++;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.CARDS.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards left in hand:", stats.mean(), stats.sd())), c);
        c.gridy++;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_CARDS_PLAYED.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards played:", stats.mean(), stats.sd())), c);
        c.gridy++;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_CARDS_PLAYED_AUTOMATED.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards played (automated):", stats.mean(), stats.sd())), c);
        c.gridy++;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_CARDS_PLAYED_ACTIVE.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards played (active):", stats.mean(), stats.sd())), c);
        c.gridy++;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_CARDS_PLAYED_EVENT.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards played (event):", stats.mean(), stats.sd())), c);

        // Detailed card win rate plot
        c.gridy++;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy++;
        getContentPane().add(new JLabel("Cards win rate:"), c);
        TAGStringStatSummary summary = playerAggregateLogger.summaryStringData().get(TMPlayerListener.TMPlayerAttributes.CARDS_PLAYED_WIN.name());
        TAGStringStatSummary summary2 = playerAggregateLogger.summaryStringData().get(TMPlayerListener.TMPlayerAttributes.CARDS_PLAYED.name());
        double[] winRate = new double[summary.getElements().size()-1];
        String[] xTicks = new String[summary.getElements().size()-1];
        int i = 0;
        for (String card: summary.getElements().keySet()) {
            if (card.equals("")) continue;
            winRate[i] = summary.getElements().get(card)*100.0 / summary2.getElements().get(card);
            xTicks[i] = card;
            i++;
        }
        c.gridy++;
        BarPlot barPlot3 = new BarPlot(winRate, "Win Rate", "Card");
        barPlot3.setxTicks(xTicks);
        barPlot3.setDrawYvalue(false);
        getContentPane().add(barPlot3, c);

        // Frame properties
        setSize(new Dimension(1200, 800));
        setPreferredSize(new Dimension(1200, 800));
        setMinimumSize(new Dimension(1200, 800));
        revalidate();
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//        setExtendedState(JFrame.MAXIMIZED_BOTH);
        repaint();
    }
}

package games.terraformingmars.stats;

import core.interfaces.IGameListener;
import evaluation.StatsVisualiser;
import games.terraformingmars.TMForwardModel;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.gui.*;
import gui.TiledImage;
import utilities.ImageIO;
import utilities.SummaryLogger;
import utilities.TAGStatSummary;
import utilities.TAGStringStatSummary;
import utilities.plotting.BarPlot;
import utilities.plotting.DotPlot;
import utilities.plotting.PiePlot;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TMStatsVisualiser extends StatsVisualiser {
    SummaryLogger[] playerLoggers;
    SummaryLogger playerAggregateLogger;
    SummaryLogger gameLogger;

    public static int fontSize = 16;
    public static Font defaultFont = new Font("Prototype", Font.BOLD, fontSize);
    public static Font defaultFontSmall = new Font("Prototype", Font.BOLD, fontSize-5);
    public static Color fontColor = Color.white;

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
        c.gridy = 1;
        DotPlot dotPlot1 = new DotPlot(stats.getElements().toArray(new Double[0]), 0, 30);
        setDefaultDotPlotProperties(dotPlot1);
        getContentPane().add(dotPlot1, c);

        // Global parameters
        c.gridy = 2;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy = 3;
        getContentPane().add(new JLabel("Generation global parameter was completed:"), c);
        c.gridy = 4;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.GP_OCEAN_COMPLETE_GEN.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Ocean:", stats.mean(), stats.sd())), c);
        c.gridy = 5;
        DotPlot dotPlot2 = new DotPlot(stats.getElements().toArray(new Double[0]), 0, 30);
        setDefaultDotPlotProperties(dotPlot2);
        getContentPane().add(dotPlot2, c);
        c.gridy = 6;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.GP_OXYGEN_COMPLETE_GEN.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Oxygen:", stats.mean(), stats.sd())), c);
        c.gridy = 7;
        DotPlot dotPlot3 = new DotPlot(stats.getElements().toArray(new Double[0]), 0, 30);
        setDefaultDotPlotProperties(dotPlot3);
        getContentPane().add(dotPlot3, c);
        c.gridy = 8;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.GP_TEMPERATURE_COMPLETE_GEN.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Temperature:", stats.mean(), stats.sd())), c);
        c.gridy = 9;
        DotPlot dotPlot4 = new DotPlot(stats.getElements().toArray(new Double[0]), 0, 30);
        setDefaultDotPlotProperties(dotPlot4);
        getContentPane().add(dotPlot4, c);

        // Resource averages
        c.gridy = 10;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy = 11;
        getContentPane().add(new JLabel("Resources at the end:"), c);
        c.gridy = 12;
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
        BarPlot barPlot1 = new BarPlot(resources, "Amount", "Resource", fontSize, defaultFontSmall, fontColor);
        barPlot1.setxTicks(new String[]{"MC", "Steel", "Titanium", "Plant", "Energy", "Heat"});
        barPlot1.setxTickImages(xTickImages);
        setDefaultBarPlotProperties(barPlot1);
        c.gridheight = 7;
        getContentPane().add(barPlot1, c);
        c.gridheight = 1;
        c.gridy = 19;
        getContentPane().add(new JLabel("Resource production at the end:"), c);
        c.gridy = 20;
        c.gridheight = 7;
        resources = new double[] {
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.MEGACREDIT_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.STEEL_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.TITANIUM_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.PLANT_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.ENERGY_PROD.name()).mean(),
                playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.HEAT_PROD.name()).mean(),
        };
        BarPlot barPlot2 = new BarPlot(resources, "Amount", "Resource", fontSize, defaultFontSmall, fontColor);
        barPlot2.setxTicks(new String[]{"MC", "Steel", "Titanium", "Plant", "Energy", "Heat"});
        barPlot2.setxTickImages(xTickImages);
        setDefaultBarPlotProperties(barPlot2);
        getContentPane().add(barPlot2, c);
        c.gridheight = 1;

        // Map coverage
        c.gridx = 1;
        c.gridy = 0;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.MAP_COVERAGE.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g%% +/- %6.2g", "Map coverage:", stats.mean()*100, stats.sd()*100)), c);

        // Heatmap (percentage of times the tile was placed out of all games played)
        c.gridy = 1;
        c.gridheight = 9;
        TMGameState gs = new TMGameState(new TMGameParameters(0), 2);
        TMForwardModel fm = new TMForwardModel();
        fm.setup(gs);
        TAGStringStatSummary stats2 = gameLogger.summaryStringData().get(TMGameListener.TMGameAttributes.MAP_TILES.name());
        TMBoardHeatMap view = new TMBoardHeatMap(gs, stats2, stats.n());
        getContentPane().add(view, c);
        c.gridheight = 1;

        // Score gap
        c.gridy = 10;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy = 11;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.POINT_DIFF.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Score gap:", stats.mean(), stats.sd())), c);

        // Card stats
        c.gridy = 12;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy = 13;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.CARDS.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards left in hand:", stats.mean(), stats.sd())), c);
        c.gridy = 14;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_CARDS_PLAYED.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards played:", stats.mean(), stats.sd())), c);
        c.gridy = 15;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_CARDS_PLAYED_AUTOMATED.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards played (automated):", stats.mean(), stats.sd())), c);
        c.gridy = 16;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_CARDS_PLAYED_ACTIVE.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards played (active):", stats.mean(), stats.sd())), c);
        c.gridy = 17;
        stats = playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_CARDS_PLAYED_EVENT.name());
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", "Cards played (event):", stats.mean(), stats.sd())), c);

        // Detailed card win rate plot
        c.gridy = 18;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), c);
        c.gridy = 19;
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
        c.gridy = 20;
        c.gridheight = 7;
        BarPlot barPlot3 = new BarPlot(winRate, "Win Rate", "Card", fontSize, defaultFontSmall, fontColor);
        barPlot3.setxTicks(xTicks);
        setDefaultBarPlotProperties(barPlot3);
        barPlot3.setOrderData(true);
        barPlot3.setDrawYvalue(false);
        JScrollPane pane = new JScrollPane(barPlot3);
        pane.setOpaque(false);
        pane.setBorder(null);
        pane.getViewport().setOpaque(false);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        pane.getHorizontalScrollBar().setOpaque(false);
        pane.getHorizontalScrollBar().setUnitIncrement(16);
        pane.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = Color.white;
                this.trackColor = Color.black;
            }
        });
        pane.setPreferredSize(new Dimension(320, 170));
        getContentPane().add(pane, c);
        c.gridheight = 1;

        // Points distribution
        c.gridx = 2;
        c.gridy = 0;
        stats = gameLogger.summary().get(TMGameListener.TMGameAttributes.MAP_COVERAGE.name());
        getContentPane().add(new JLabel("Points distribution:"), c);

        c.gridy = 1;
        c.gridheight = 9;
        double[] data = new double[] {
            playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_POINTS_TR.name()).mean(),
            playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_POINTS_MILESTONES.name()).mean(),
            playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_POINTS_AWARDS.name()).mean(),
            playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_POINTS_CARDS.name()).mean(),
            playerAggregateLogger.summary().get(TMPlayerListener.TMPlayerAttributes.N_POINTS_BOARD.name()).mean(),
        };
        String[] dataLabels = new String[] {
                "TR", "Milestones", "Awards", "Cards", "Board"
        };
        PiePlot pp = new PiePlot(data, dataLabels);
        setDefaultPiePlotProperties(pp);
        getContentPane().add(pp, c);
        c.gridheight = 1;

        super.setFrameProperties(new Dimension(1200, 800));
    }

    private void setDefaultBarPlotProperties(BarPlot barPlot) {
        barPlot.setUnitHeight(20);
        barPlot.setPadding(1);
        barPlot.setMaxWidth(300);
        barPlot.setMaxHeight(150);
        barPlot.setHoverOverWidth(100);
        barPlot.setHoverOverHeight(20);
        barPlot.setBarColor(new Color(174, 241, 124, 190));
        barPlot.setHoverOverColor(new Color(0,0,0,150));
        barPlot.setStretchY(true);
        barPlot.setReverseOrder(true);
        barPlot.setOrderData(false);
    }

    private void setDefaultDotPlotProperties(DotPlot dotPlot) {
        dotPlot.setDotColor(new Color(22, 230, 250, 40));
        dotPlot.setOutlineColor(fontColor);
        dotPlot.setMaxWidth(300);
        dotPlot.setMaxHeight(20);
        dotPlot.setPadding(2);
    }

    private void setDefaultPiePlotProperties(PiePlot piePlot) {
        piePlot.setMaxWidth(200);
        piePlot.setMaxHeight(200);
    }

}

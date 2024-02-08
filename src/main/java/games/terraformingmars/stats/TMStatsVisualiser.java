package games.terraformingmars.stats;

import evaluation.metrics.Event;
import evaluation.listeners.MetricsGameListener;
import evaluation.StatsVisualiser;
import games.terraformingmars.TMForwardModel;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.gui.*;
import utilities.ImageIO;
import evaluation.summarisers.TAGNumericStatSummary;
import evaluation.summarisers.TAGOccurrenceStatSummary;
import gui.plotting.PiePlot;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TMStatsVisualiser extends StatsVisualiser {

    public static Font defaultFont = new Font("Prototype", Font.BOLD, fontSize);
    public static Font defaultFontSmall = new Font("Prototype", Font.BOLD, fontSize-5);

    public TMStatsVisualiser(List<MetricsGameListener> listeners) {
        super(listeners);

        // Set background image
        BufferedImage bg = (BufferedImage) ImageIO.GetInstance().getImage("data/terraformingmars/images/stars.jpg");
        setBackgroundImage(bg);

        // Register font type
        try {
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("data/terraformingmars/images/fonts/Prototype.ttf")));
        } catch (IOException | FontFormatException e) {
            //Handle exception
        }
        UIManager.put("Label.font", defaultFont);


        // ------- column 0 --------
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;

        // Generation average
        visualiseMetricAsDotPlot("Generation", Event.GameEvent.GAME_OVER, 0, 30);

        // Spacing
        gridBagConstraints.gridy ++;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), gridBagConstraints);

        // Global parameters
        visualiseMetricAsDotPlot("ParameterComplete (OceanTiles)", Event.GameEvent.GAME_OVER, 0, 30);
        visualiseMetricAsDotPlot("ParameterComplete (Oxygen)", Event.GameEvent.GAME_OVER, 0, 30);
        visualiseMetricAsDotPlot("ParameterComplete (Temperature)", Event.GameEvent.GAME_OVER, 0, 30);

        // Spacing
        gridBagConstraints.gridy ++;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), gridBagConstraints);

        // Get all resource averages at the end of the game
        TMTypes.Resource[] playerBoardResources = TMTypes.Resource.getPlayerBoardResources();
        double[] resources = new double[playerBoardResources.length];
        double[] resourceProduction = new double[playerBoardResources.length];
        Image[] xTickImages = new Image[playerBoardResources.length];
        String[] xTickLabels = new String[playerBoardResources.length];
        for (int i = 0; i < playerBoardResources.length; i++) {
            resources[i] = ((TAGNumericStatSummary) getStats("PlayerResource (" + playerBoardResources[i].name() + "):All", Event.GameEvent.GAME_OVER)).mean();
            resourceProduction[i] = ((TAGNumericStatSummary) getStats("PlayerProduction (" + playerBoardResources[i].name() + "):All", Event.GameEvent.GAME_OVER)).mean();
            xTickImages[i] = ImageIO.GetInstance().getImage(playerBoardResources[i].getImagePath());
            xTickLabels[i] = playerBoardResources[i].name();
        }
        int resourcePlotHeight = 7;

        // Visualise resources
        gridBagConstraints.gridy ++;
        getContentPane().add(new JLabel("Resources at the end:"), gridBagConstraints);  // 12
        visualiseMetricAsBarPlot(resources, "Amount", "Resource", defaultFontSmall, xTickLabels, xTickImages, resourcePlotHeight);

        // Visualise production
        gridBagConstraints.gridy += resourcePlotHeight;
        getContentPane().add(new JLabel("Resource production at the end:"), gridBagConstraints);  // 19
        visualiseMetricAsBarPlot(resourceProduction, "Amount", "Resource", defaultFontSmall, xTickLabels, xTickImages, resourcePlotHeight);


        // ------- column 1 --------
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;

        // Map coverage
        TAGNumericStatSummary stats = visualiseNumericMetricAsLabel("MapCoverage", Event.GameEvent.GAME_OVER);
//        getContentPane().add(new JLabel(String.format("%30s  %8.3g%% +/- %6.2g", "Map coverage:", stats.mean()*100, stats.sd()*100)), gridBagConstraints);

        // Custom TM Heatmap (percentage of times the tile was placed out of all games played)
        //  - Construct heatmap view
        TMGameState gs = new TMGameState(new TMGameParameters(), 2);
        TMForwardModel fm = new TMForwardModel();
        fm.setup(gs);
        TAGOccurrenceStatSummary stats2 = (TAGOccurrenceStatSummary)getStats("MapTiles", Event.GameEvent.GAME_OVER);
        TMBoardHeatMap view = new TMBoardHeatMap(gs, stats2, stats.n());
        //  - Add heatmap to frame
        int heatMapHeight = 9;
        gridBagConstraints.gridy ++;
        gridBagConstraints.gridheight = heatMapHeight;
        getContentPane().add(view, gridBagConstraints);
        gridBagConstraints.gridheight = 1;

        // Spacing
        gridBagConstraints.gridy += heatMapHeight;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), gridBagConstraints);

        // Score gap
        visualiseNumericMetricAsLabel("PointDifference", Event.GameEvent.GAME_OVER, "Score gap");

        // Spacing
        gridBagConstraints.gridy ++;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), gridBagConstraints);

        // Card stats
//        visualiseNumericMetricAsLabel("PlayerCardsPlayed:All", Event.GameEvent.GAME_OVER, "Cards played");
        visualiseNumericMetricAsLabel("PlayerPlayedCardsPerType (Automated):All", Event.GameEvent.GAME_OVER, "Cards played (automated)");
        visualiseNumericMetricAsLabel("PlayerPlayedCardsPerType (Active):All", Event.GameEvent.GAME_OVER, "Cards played (active)");
        visualiseNumericMetricAsLabel("PlayerPlayedCardsPerType (Event):All", Event.GameEvent.GAME_OVER, "Cards played (event)");

        // Spacing
        gridBagConstraints.gridy ++;
        getContentPane().add(Box.createRigidArea(new Dimension(0, 20)), gridBagConstraints);

        // Detailed card win rate plot
        gridBagConstraints.gridy ++;
        getContentPane().add(new JLabel("Cards win rate:"), gridBagConstraints);
        TAGOccurrenceStatSummary summary = (TAGOccurrenceStatSummary) getStats("PlayerCardsPlayedWin:All", Event.GameEvent.GAME_OVER);
        TAGOccurrenceStatSummary summary2 = (TAGOccurrenceStatSummary) getStats("PlayerAllCardsPlayed:All", Event.GameEvent.GAME_OVER);
        visualiseComponentWinRate(summary, summary2, "Card", defaultFontSmall, 7);


        // --------- column 2 ---------
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;

        // Points distribution visualised as pie plot
        getContentPane().add(new JLabel("Points distribution:"), gridBagConstraints);
        gridBagConstraints.gridy ++;
        gridBagConstraints.gridheight = 9;
        double[] data = new double[] {
            ((TAGNumericStatSummary)getStats("PlayerPointsTR:All", Event.GameEvent.GAME_OVER)).mean(),
            ((TAGNumericStatSummary)getStats("PlayerPointsMilestones:All", Event.GameEvent.GAME_OVER)).mean(),
            ((TAGNumericStatSummary)getStats("PlayerPointsAwards:All", Event.GameEvent.GAME_OVER)).mean(),
            ((TAGNumericStatSummary)getStats("PlayerPointsCards:All", Event.GameEvent.GAME_OVER)).mean(),
            ((TAGNumericStatSummary)getStats("PlayerPointsBoard:All", Event.GameEvent.GAME_OVER)).mean(),
        };
        String[] dataLabels = new String[] {
                "TR", "Milestones", "Awards", "Cards", "Board"
        };
        PiePlot pp = new PiePlot(data, dataLabels);
        setDefaultPiePlotProperties(pp);
        getContentPane().add(pp, gridBagConstraints);
        gridBagConstraints.gridheight = 1;


        // Finally, set frame properties
        super.setFrameProperties(new Dimension(1200, 800));
    }
}

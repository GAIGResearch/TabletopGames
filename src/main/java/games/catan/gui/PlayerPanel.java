package games.catan.gui;

import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

class PlayerPanel extends JPanel {
    final int playerID;
    JLabel playerLabel;
    JLabel scoreLabel;
    JLabel victoryPointsLabel;
    JLabel diceRollLabel;
    JLabel knightCount;
    JLabel longestRoad;
    JLabel devCards;

    Map<CatanParameters.Resource, JLabel> resourceToLabelMap;

    PlayerPanel(CatanGUI gui, int playerID, String playerName) {
        this.playerID = playerID;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        playerLabel = new JLabel();
        scoreLabel = new JLabel();
        victoryPointsLabel = new JLabel();
        diceRollLabel = new JLabel();
        knightCount = new JLabel();
        longestRoad = new JLabel();
        resourceToLabelMap = new HashMap<>();
        for (CatanParameters.Resource r: CatanParameters.Resource.values()) {
            if (r == CatanParameters.Resource.WILD) continue;
            JLabel label = new JLabel();
            resourceToLabelMap.put(r, label);
            add(label);
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // Left-click
                        gui.addHighlight(r);
                        label.setForeground(Color.blue);
                        for (CatanParameters.Resource res: resourceToLabelMap.keySet()) {
                            if (!gui.resourceHighlights.contains(res))
                                resourceToLabelMap.get(res).setForeground(Color.black);
                        }
                    } else {
                        gui.clearHighlights();
                        for (CatanParameters.Resource res: resourceToLabelMap.keySet()) {
                            resourceToLabelMap.get(res).setForeground(Color.black);
                        }
                    }
                }
            });
        }
        devCards = new JLabel();
        add(playerLabel);
        playerLabel.setForeground(CatanConstants.PlayerColors[playerID]);
        add(scoreLabel);
        add(victoryPointsLabel);
        add(knightCount);
        add(longestRoad);
        add(devCards);

        playerLabel.setText("Player " + playerID + ": " + playerName);

    }

    void _update(CatanGameState gs) {
        scoreLabel.setText("Score: " + gs.getScores()[playerID]);
        knightCount.setText("Knights: " + gs.getKnights()[playerID] + (gs.getLargestArmyOwner() == playerID? " [LARGEST ARMY]" : ""));
        longestRoad.setText("Longest road: " + gs.getRoadLengths()[playerID] + (gs.getLongestRoadOwner() == playerID? " [LONGEST ROAD]" : ""));
        victoryPointsLabel.setText("VP: " + gs.getVictoryPoints()[playerID]);
        for (CatanParameters.Resource r: CatanParameters.Resource.values()) {
            if (r == CatanParameters.Resource.WILD) continue;
            resourceToLabelMap.get(r).setText(r.name() + " = " + gs.getPlayerResources(playerID).get(r));
        }
//        playerResources.setText("<html>Resources: " + gs.getPlayerResources(playerID).toString() + "</html>");
        devCards.setText("Dev. Cards: " +  gs.getPlayerDevCards(playerID).toString());
    }
}

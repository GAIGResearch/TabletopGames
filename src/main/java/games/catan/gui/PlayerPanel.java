package games.catan.gui;

import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import javax.swing.*;
import javax.swing.border.Border;
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
    Border highlightBorder = BorderFactory.createLineBorder(Color.blue, 2);

    PlayerPanel(CatanGUI gui, int playerID, String playerName) {
        this.playerID = playerID;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        playerLabel = new JLabel("Player " + playerID + ": " + playerName);
        playerLabel.setForeground(CatanConstants.PlayerColors[playerID]);
        playerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left-click
                    gui.addPlayerHighlight(playerID);
                    playerLabel.setBorder(highlightBorder);
                    for (int i = 0; i < gui.gs.getNPlayers(); i++) {
                        if (i != playerID) {
                            gui.playerPanels[i].playerLabel.setBorder(null);
                        }
                    }
                } else {
                    gui.clearHighlights();
                    for (int i = 0; i < gui.gs.getNPlayers(); i++) {
                        gui.playerPanels[i].playerLabel.setBorder(null);
                    }
                }
            }
        });
        scoreLabel = new JLabel();
        victoryPointsLabel = new JLabel();
        diceRollLabel = new JLabel();
        knightCount = new JLabel();
        longestRoad = new JLabel();
        devCards = new JLabel();
        resourceToLabelMap = new HashMap<>();

        add(playerLabel);
        add(Box.createRigidArea(new Dimension(0,5)));
        add(scoreLabel);
        add(victoryPointsLabel);
        add(knightCount);
        add(longestRoad);
        add(Box.createRigidArea(new Dimension(0,5)));

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
                        gui.addResourceHighlight(r);
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
        add(Box.createRigidArea(new Dimension(0,5)));
        add(devCards);

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

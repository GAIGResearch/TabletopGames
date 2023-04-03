package games.catan.gui;

import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import javax.swing.*;

class PlayerPanel extends JPanel {
    final int playerID;
    JLabel playerLabel;
    JLabel scoreLabel;
    JLabel victoryPointsLabel;
    JLabel diceRollLabel;
    JLabel knightCount;
    JLabel longestRoad;
    JLabel playerResources;
    JLabel devCards;

    PlayerPanel(int playerID, String playerName) {
        this.playerID = playerID;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        playerLabel = new JLabel();
        scoreLabel = new JLabel();
        victoryPointsLabel = new JLabel();
        diceRollLabel = new JLabel();
        knightCount = new JLabel();
        longestRoad = new JLabel();
        playerResources = new JLabel();
        devCards = new JLabel();
        add(playerLabel);
        playerLabel.setForeground(CatanConstants.PlayerColors[playerID]);
        add(scoreLabel);
        add(victoryPointsLabel);
        add(knightCount);
        add(longestRoad);
        add(playerResources);
        add(devCards);

        playerLabel.setText("Player " + playerID + ": " + playerName);
    }

    void _update(CatanGameState gs) {
        scoreLabel.setText("Score: " + gs.getScores()[playerID]);
        knightCount.setText("Knights: " + gs.getKnights()[playerID] + (gs.getLargestArmyOwner() == playerID? " [LARGEST ARMY]" : ""));
        longestRoad.setText("Longest road: " + gs.getRoadLengths()[playerID] + (gs.getLongestRoadOwner() == playerID? " [LONGEST ROAD]" : ""));
        victoryPointsLabel.setText("VP: " + gs.getVictoryPoints()[playerID]);
        String resText = "<html>Resources:<ul>";
        for (CatanParameters.Resource r: CatanParameters.Resource.values()) {
            if (r == CatanParameters.Resource.WILD) continue;
            resText += "<li>" + r.name() + " = " + gs.getPlayerResources(playerID).get(r) + "</li>";
        }
        resText += "</ul></html>";
        playerResources.setText(resText);
//        playerResources.setText("<html>Resources: " + gs.getPlayerResources(playerID).toString() + "</html>");
        devCards.setText("Dev. Cards: " +  gs.getPlayerDevCards(playerID).toString());
    }
}

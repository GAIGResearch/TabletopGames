package games.catan.gui;

import games.catan.CatanConstants;
import games.catan.CatanGameState;

import javax.swing.*;
import java.awt.*;

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
    JLabel playerColourLabel;

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
        playerColourLabel = new JLabel();
        add(playerLabel);
        playerLabel.setForeground(CatanConstants.PlayerColors[playerID]);
        add(scoreLabel);
        add(victoryPointsLabel);
        add(knightCount);
        add(longestRoad);
        add(playerResources);
        add(devCards);
        add(playerColourLabel);

        playerLabel.setText("Player " + playerID + ": " + playerName);
    }

    void _update(CatanGameState gs) {
        scoreLabel.setText("Score: " + gs.getScores()[playerID]);
        knightCount.setText("Knights: " + gs.getKnights()[playerID]);
        victoryPointsLabel.setText("VP: " + gs.getVictoryPoints()[playerID]);

        playerResources.setText("<html>Resources: ");
        playerResources.setText(playerResources.getText() + "<br/>" + playerID + " : " + gs.getPlayerResources(playerID).toString());
        playerResources.setText(playerResources.getText() + "</html>");

        devCards.setText("<html>Dev. Cards: ");
        devCards.setText(devCards.getText() + "<br/>" + playerID + " : " + gs.getPlayerDevCards(playerID).toString());
        devCards.setText(devCards.getText() + "<br/></html>");
    }
}

package games.wonders7.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import games.wonders7.Wonders7GameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Set;

public class Wonders7GUI extends AbstractGUIManager {
    JLabel ageLabel;
    JScrollPane[] playerViews;
    Border highlightActive = BorderFactory.createLineBorder(new Color(220, 27, 67), 3);
    Border[] playerViewBorders, playerViewBordersHighlight, playerViewBordersHighlightLose, playerViewBordersHighlightWin;

    public Wonders7GUI(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        if (game == null) return;

        parent.setLayout(new BoxLayout(parent, BoxLayout.Y_AXIS));
        // Game title
        parent.add(new JLabel("7 Wonders"));
        // Game info: current age
        ageLabel = new JLabel("Current age: 0");
        parent.add(ageLabel);
        // TODO: game info - hands rotating which way?

        // Player info. Arrange on 2 rows, first left-right, then right-left (circle)
        int nRows = 2;
        int nPlayers = game.getPlayers().size();
        int nCols = nPlayers/nRows;
        if (nPlayers%nRows != 0) nCols++;
        JPanel firstRow = new JPanel();
        firstRow.setLayout(new BoxLayout(firstRow, BoxLayout.X_AXIS));
        JPanel secondRow = new JPanel();
        secondRow.setLayout(new BoxLayout(secondRow, BoxLayout.X_AXIS));
        playerViews = new JScrollPane[nPlayers];
        playerViewBorders = new Border[nPlayers];
        playerViewBordersHighlight = new Border[nPlayers];
        playerViewBordersHighlightLose = new Border[nPlayers];
        playerViewBordersHighlightWin = new Border[nPlayers];
        for (int i = 0; i < nCols; i++) {
            PlayerView playerView = new PlayerView((Wonders7GameState) game.getGameState(), i);
            playerViews[i] = new JScrollPane(playerView);
            playerViews[i].setPreferredSize(new Dimension(300, 300));
            playerViews[i].setMaximumSize(new Dimension(300, 300));
            playerViews[i].setMinimumSize(new Dimension(300, 300));
            firstRow.add(playerViews[i]);

            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + game.getPlayers().get(i).toString() + "]",
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            TitledBorder title1 = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + game.getPlayers().get(i).toString() + "] (WIN)",
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            TitledBorder title2 = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + game.getPlayers().get(i).toString() + "] (LOSE)",
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            playerViewBorders[i] = title;
            playerViewBordersHighlight[i] = BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i]);
            playerViewBordersHighlightWin[i] = BorderFactory.createCompoundBorder(highlightActive, title1);
            playerViewBordersHighlightWin[i] = BorderFactory.createCompoundBorder(highlightActive, title2);
            playerViews[i].setBorder(title);
        }
        for (int i = nPlayers-1; i >= nCols; i--) {
            PlayerView playerView = new PlayerView((Wonders7GameState) game.getGameState(), i);
            playerViews[i] = new JScrollPane(playerView);
            playerViews[i].setPreferredSize(new Dimension(300, 300));
            secondRow.add(playerViews[i]);

            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + game.getPlayers().get(i).toString() + "]",
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            TitledBorder title1 = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + game.getPlayers().get(i).toString() + "] (WIN)",
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            TitledBorder title2 = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + game.getPlayers().get(i).toString() + "] (LOSE)",
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            playerViewBorders[i] = title;
            playerViewBordersHighlight[i] = BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i]);
            playerViewBordersHighlightWin[i] = BorderFactory.createCompoundBorder(highlightActive, title1);
            playerViewBordersHighlightWin[i] = BorderFactory.createCompoundBorder(highlightActive, title2);
            playerViews[i].setBorder(title);
        }
        parent.add(firstRow);
        parent.add(secondRow);

        // Action panel
        parent.add(createActionPanel(new IScreenHighlight[0], defaultDisplayWidth, defaultActionPanelHeight));
    }

    @Override
    public int getMaxActionSpace() {
        return 200;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.isNotTerminal()) {
            ageLabel.setText("Current age: " + ((Wonders7GameState)gameState).getCurrentAge());
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    playerViews[i].setBorder(playerViewBordersHighlight[i]);
                } else {
                    playerViews[i].setBorder(playerViewBorders[i]);
                }
            }
        } else {
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                // Highlight win/lose player
                if (gameState.getPlayerResults()[i] == CoreConstants.GameResult.WIN_GAME) {
                    playerViews[i].setBorder(playerViewBordersHighlightWin[i]);
                } else {
                    playerViews[i].setBorder(playerViewBordersHighlightLose[i]);
                }
            }
        }
    }
}

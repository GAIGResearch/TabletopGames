package games.puertorico.gui;

import core.*;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.PuertoRicoParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.OutlineLabel;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static games.puertorico.gui.PRGUIUtils.secondaryColor;
import static games.puertorico.gui.PRGUIUtils.titleColor;

public class PuertoRicoGUI extends AbstractGUIManager {
    JLabel roundCount;

    Border highlightActive = BorderFactory.createLineBorder(titleColor, 3);
    Border highlightLose = BorderFactory.createLineBorder(new Color(208, 67, 67), 3);
    Border highlightWin = BorderFactory.createLineBorder(new Color(130, 199, 49), 3);
    PlayerBoard[] playerBoards;
    Border[] playerViewBorders, playerViewBordersHighlightCurrent, playerViewBordersHighlightWin, playerViewBordersHighlightLose;

    public PuertoRicoGUI(GamePanel parent, Game game, ActionController ac, int human) {
        super(parent, game, ac, human);
        if (game == null) return;

        width = 1000;
        height = 700;
        PuertoRicoGameState gs = (PuertoRicoGameState) game.getGameState();
        PuertoRicoParameters params = (PuertoRicoParameters) game.getGameState().getGameParameters();

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(true);
        wrapper.setBackground(PRGUIUtils.backgroundColor);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setPreferredSize(new Dimension(width, height));

        parent.setLayout(new FlowLayout());
        parent.add(wrapper);

        /* ------------------------------------- */
        // Title
        OutlineLabel title = new OutlineLabel("Puerto Rico", JLabel.CENTER, 1);
        title.setFont(PRGUIUtils.titleFont);
        title.setOutlineColor(Color.black);
        title.setForeground(PRGUIUtils.titleColor);
        wrapper.add(title);

        /* ------------------------------------- */
        // Top panel, general game info
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));

        // Left: game state
        JPanel gameStateInfo = new JPanel();
        gameStateInfo.setOpaque(false);
        top.add(gameStateInfo);
        gameStateInfo.setLayout(new BoxLayout(gameStateInfo, BoxLayout.Y_AXIS));
        roundCount = new JLabel("Round: 0");
        roundCount.setForeground(secondaryColor);
        roundCount.setFont(PRGUIUtils.textFontBold);
        roundCount.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
        gameStateInfo.add(roundCount);
        gameStateInfo.add(new GSInfo(gs));
        gameStateInfo.add(new GSPlantations(gs));
        gameStateInfo.add(new GSShipsAndMarket(gs));

        // Middle: role panel + buildings available
//        JPanel middlePanel = new JPanel();
//        middlePanel.setOpaque(false);
//        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.X_AXIS));
        top.add(new RolePanel(gs));
        top.add(new GSBuildings(gs));
//        top.add(middlePanel);

        // Right: action history
        createActionHistoryPanel(300, 200, -1);
        top.add(historyContainer);

        /* ------------------------------------- */
        // Middle panel: player boards
        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.X_AXIS));
        playerViewBorders = new Border[gs.getNPlayers()];
        playerViewBordersHighlightCurrent = new Border[gs.getNPlayers()];
        playerViewBordersHighlightLose = new Border[gs.getNPlayers()];
        playerViewBordersHighlightWin = new Border[gs.getNPlayers()];
        playerBoards = new PlayerBoard[gs.getNPlayers()];
        // player boards
        // player ID, name in border
        for (int i = 0; i < gs.getNPlayers(); i++) {
            PlayerBoard pb = new PlayerBoard(gs, i);
            playerBoards[i] = pb;

            TitledBorder titleBorder = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + game.getPlayers().get(i).toString() + "]",
                    TitledBorder.CENTER, TitledBorder.ABOVE_TOP);
            TitledBorder titleBorderWin = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + game.getPlayers().get(i).toString() + "] (WIN)",
                    TitledBorder.CENTER, TitledBorder.ABOVE_TOP);
            TitledBorder titleBorderLose = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + game.getPlayers().get(i).toString() + "] (LOSE)",
                    TitledBorder.CENTER, TitledBorder.ABOVE_TOP);
            playerViewBorders[i] = titleBorder;
            playerViewBordersHighlightCurrent[i] = BorderFactory.createCompoundBorder(highlightActive, titleBorder);
            playerViewBordersHighlightLose[i] = BorderFactory.createCompoundBorder(highlightLose, titleBorderLose);
            playerViewBordersHighlightWin[i] = BorderFactory.createCompoundBorder(highlightWin, titleBorderWin);

            pb.setBorder(titleBorder);
            middle.add(pb);
            // TODO // Highlight current player (border)
            // TODO Highlight WIN/LOSE
        }

        /* ------------------------------------- */
        // Bottom panel: actions
        wrapper.add(top);
        wrapper.add(middle);
        wrapper.add(createActionPanel(new IScreenHighlight[0], width, 50, false, false, null));
    }

    @Override
    public int getMaxActionSpace() {
        return 100;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!gameState.isNotTerminal()) {
            roundCount.setText("Round: " + gameState.getRoundCounter() + " (GAME OVER)");

            for (int i = 0; i < gameState.getNPlayers(); i++) {
                // Highlight win/lose player
                if (gameState.getPlayerResults()[i] == CoreConstants.GameResult.WIN_GAME) {
                    playerBoards[i].setBorder(playerViewBordersHighlightWin[i]);
                } else {
                    playerBoards[i].setBorder(playerViewBordersHighlightLose[i]);
                }
            }
        } else {
            roundCount.setText("Round: " + gameState.getRoundCounter());

            for (int i = 0; i < gameState.getNPlayers(); i++) {
                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    playerBoards[i].setBorder(playerViewBordersHighlightCurrent[i]);
                } else {
                    playerBoards[i].setBorder(playerViewBorders[i]);
                }
            }
        }
    }
}

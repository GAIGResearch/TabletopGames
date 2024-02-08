package games.puertorico.gui;

import core.*;
import core.actions.AbstractAction;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.actions.*;
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
import java.util.*;
import java.util.List;

import static games.puertorico.gui.PRGUIUtils.*;

public class PuertoRicoGUI extends AbstractGUIManager {
    JLabel roundCount;

    Border highlightActive = BorderFactory.createLineBorder(titleColor, 3);
    Border highlightLose = BorderFactory.createLineBorder(new Color(208, 67, 67), 3);
    Border highlightWin = BorderFactory.createLineBorder(new Color(130, 199, 49), 3);

    GSShipsAndMarket shipsAndMarket;
    PlayerBoard[] playerBoards;
    Border[] playerViewBorders, playerViewBordersHighlightCurrent, playerViewBordersHighlightWin, playerViewBordersHighlightLose;

    public PuertoRicoGUI(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;

        width = 1000;
        height = 700;
        PuertoRicoGameState gs = (PuertoRicoGameState) game.getGameState();

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
        gameStateInfo.add(new GSInfo(this, gs));
        gameStateInfo.add(new GSPlantations(this, gs));
        shipsAndMarket = new GSShipsAndMarket(this, gs);
        gameStateInfo.add(shipsAndMarket);

        // Middle: role panel + buildings available
        RolePanel rolePanel = new RolePanel(this, gs);
        top.add(rolePanel);
        top.add(new GSBuildings(this, gs));

        // Right: action history
        createActionHistoryPanel(300, 200, new HashSet<>());
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
        List<IScreenHighlight> highlights = new ArrayList<>();
        for (int i = 0; i < gs.getNPlayers(); i++) {
            PlayerBoard pb = new PlayerBoard(this, gs, i);
            playerBoards[i] = pb;
            highlights.add(pb);

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
        }

        /* ------------------------------------- */
        highlights.add(shipsAndMarket);
        highlights.add(rolePanel);

        // Bottom panel: actions
        wrapper.add(top);
        wrapper.add(middle);
        wrapper.add(createActionPanel(highlights.toArray(new IScreenHighlight[0]), width, 50, false, false, null, null, null));

        // Action buttons / instructions styling
        for (ActionButton ab: actionButtons) {
            ab.setUI(buttonUI);
            ab.setBackground(Color.white);
        }
    }

    @Override
    public int getMaxActionSpace() {
        return 100;
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING && !(actionButtons == null)) {
            PuertoRicoGameState gs = (PuertoRicoGameState) gameState;
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            Set<String> markedActionInstructions = new HashSet<>();
            int nButtonsCreated = 0;
            for (int i = 0; i < actions.size() && i < maxActionSpace; i++) {
                if (actions.get(i) instanceof SelectRole) {
                    if (!markedActionInstructions.contains("SelectRole")) {
                        actionButtons[nButtonsCreated].setVisible(true);
                        actionButtons[nButtonsCreated].setEnabled(false);
                        actionButtons[nButtonsCreated].setButtonAction(null, "Select a role (click on the role name chosen in the role panel)");
                        markedActionInstructions.add("SelectRole");
                        nButtonsCreated++;
                    }
                } else if (actions.get(i) instanceof Build) {
                    if (!markedActionInstructions.contains("Build")) {
                        actionButtons[nButtonsCreated].setVisible(true);
                        actionButtons[nButtonsCreated].setEnabled(false);
                        actionButtons[nButtonsCreated].setButtonAction(null, "Select a building (click on the building tile in the market)");
                        markedActionInstructions.add("Build");
                        nButtonsCreated++;
                    }
                } else if (actions.get(i) instanceof DrawPlantation || actions.get(i) instanceof BuildQuarry) {
                    if (!markedActionInstructions.contains("DrawPlantation")) {
                        actionButtons[nButtonsCreated].setVisible(true);
                        actionButtons[nButtonsCreated].setEnabled(false);
                        String drawFromStack = "";
                        if (gs.hasActiveBuilding(gameState.getCurrentPlayer(), PuertoRicoConstants.BuildingType.HACIENDA)) {
                            drawFromStack = ", or the stack";
                        }
                        if (gameState.getCurrentPlayer() == gs.getRoleOwner()) {
                            actionButtons[nButtonsCreated].setButtonAction(null, "Select a visible plantation" + drawFromStack + ", or a quarry.");
                        } else {
                            actionButtons[nButtonsCreated].setButtonAction(null, "Select a visible plantation" + drawFromStack + ".");
                        }
                        markedActionInstructions.add("DrawPlantation");
                        nButtonsCreated++;
                    }
                } else if (actions.get(i) instanceof ShipCargo) {
                    if (((ShipCargo)actions.get(i)).shipNumber >= 10) {
                        // Shipping to private wharf
                        if (playerBoards[gs.getCurrentPlayer()].cropClicked != null) {
                            actionButtons[nButtonsCreated].setVisible(true);
                            actionButtons[nButtonsCreated].setEnabled(true);
                            actionButtons[nButtonsCreated].setButtonAction(actions.get(i), "Load " + playerBoards[gs.getCurrentPlayer()].cropClicked.toString() + " on your private wharf.");
                            nButtonsCreated++;
                        }
                    } else {
                        // needs ship + cargo
                        if (!markedActionInstructions.contains("ShipCargo")) {
                            actionButtons[nButtonsCreated].setVisible(true);
                            actionButtons[nButtonsCreated].setEnabled(false);
                            actionButtons[nButtonsCreated].setButtonAction(null, "Select a cargo to load from your player board, and a ship (or use button after selecting crop to load to private wharf, if available).");
                            markedActionInstructions.add("ShipCargo");
                            nButtonsCreated++;
                        }
                    }
                } else if (actions.get(i) instanceof DiscardGoodsExcept || actions.get(i) instanceof WarehouseStorage) {
                    String action = actions.get(i) instanceof DiscardGoodsExcept ? "keep" : "store";
                    if (playerBoards[gs.getCurrentPlayer()].cropClicked == null) {
                        // Instructions to pick a crop
                        if (!markedActionInstructions.contains("DiscardGoodsExcept")) {
                            actionButtons[nButtonsCreated].setVisible(true);
                            actionButtons[nButtonsCreated].setEnabled(false);
                            actionButtons[nButtonsCreated].setButtonAction(null, "You must select a crop on your player board to " + action + (action.equals("keep")? " (others not stored will be lost)" : "") + ".");
                            markedActionInstructions.add("DiscardGoodsExcept");
                            nButtonsCreated++;
                        }
                    } else {
                        // Button matching the crop
                        PuertoRicoConstants.Crop actionCrop = actions.get(i) instanceof DiscardGoodsExcept ? ((DiscardGoodsExcept)actions.get(i)).crop
                                : ((WarehouseStorage)actions.get(i)).storedCrop;
                        if (actionCrop != playerBoards[gs.getCurrentPlayer()].cropClicked) continue;

                        actionButtons[nButtonsCreated].setVisible(true);
                        actionButtons[nButtonsCreated].setEnabled(true);
                        actionButtons[nButtonsCreated].setButtonAction(actions.get(i),  capitalize(action) + " " + actionCrop.name());
                        nButtonsCreated++;
                    }
                } else if (actions.get(i) instanceof GainCrop) {
                    if (!markedActionInstructions.contains("GainCrop")) {
                        actionButtons[nButtonsCreated].setVisible(true);
                        actionButtons[nButtonsCreated].setEnabled(false);
                        actionButtons[nButtonsCreated].setButtonAction(null, "Select a crop on your player board to gain as Craftsman bonus.");
                        markedActionInstructions.add("GainCrop");
                        nButtonsCreated++;
                    }
                } else if (actions.get(i) instanceof OccupyBuilding || actions.get(i) instanceof OccupyPlantation) {
                    if (!markedActionInstructions.contains("OccupyBuilding")) {
                        actionButtons[nButtonsCreated].setVisible(true);
                        actionButtons[nButtonsCreated].setEnabled(false);
                        actionButtons[nButtonsCreated].setButtonAction(null, "Select a building or plantation on your player board with an available space to place a colonist.");
                        markedActionInstructions.add("OccupyBuilding");
                        nButtonsCreated++;
                    }
                } else if (actions.get(i) instanceof Sell) {
                    if (!markedActionInstructions.contains("Sell")) {
                        actionButtons[nButtonsCreated].setVisible(true);
                        actionButtons[nButtonsCreated].setEnabled(false);
                        actionButtons[nButtonsCreated].setButtonAction(null, "Select a crop on your player board to sell.");
                        markedActionInstructions.add("Sell");
                        nButtonsCreated++;
                    }
                } else {
                    actionButtons[nButtonsCreated].setVisible(true);
                    actionButtons[nButtonsCreated].setEnabled(true);
                    actionButtons[nButtonsCreated].setButtonAction(actions.get(i), gameState);
                    nButtonsCreated++;
                }
            }
            for (int i = nButtonsCreated; i < actionButtons.length; i++) {
                actionButtons[i].setVisible(false);
                actionButtons[i].setButtonAction(null, "");
            }
        }
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

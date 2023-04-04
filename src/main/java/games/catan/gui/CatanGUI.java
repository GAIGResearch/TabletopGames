package games.catan.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.actions.BuildRoad;
import games.catan.actions.DeepPlaceSettlementThenRoad;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.Pair;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CatanGUI extends AbstractGUIManager {
    CatanGameState gs;
    CatanBoardView boardView;
    PlayerPanel[] playerPanels;

    JPanel gameInfo;
    JLabel scoreLabel;
    JLabel victoryPointsLabel;
    JLabel diceRollLabel;
    JLabel knightCount;
    JLabel longestRoad;
    JLabel playerResources;
    JLabel devCards;
    JLabel playerColourLabel;

    JScrollPane actionScrollPane;

    boolean filterActions = true;

    public CatanGUI(GamePanel parent, Game game, ActionController ac, int humanId) {
        super(parent, game, ac, humanId);
        if (game == null) return;
        this.gs = (CatanGameState) game.getGameState();

        boardView = new CatanBoardView(gs);

        // Bottom area will show actions available
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], 400, defaultActionPanelHeight, false, false, this::scrollActionPanelToTop, this::highlightActionOnBoard, this::removeHighlightOnBoard);
        JPanel wrapper = new JPanel();
        wrapper.setBackground(Color.white);
        parent.setLayout(new FlowLayout());
        parent.add(wrapper);

        wrapper.setLayout(new BorderLayout());
        wrapper.setBackground(Color.white);
        wrapper.add(createGameStateInfoPanel(gs), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        // each player have their own panel
        playerPanels = new PlayerPanel[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            playerPanels[i] = new PlayerPanel(i, game.getPlayers().get(i).toString());
            playerPanels[i].setOpaque(false);
        }

        mainPanel.add(Box.createRigidArea(new Dimension(5,0)));

        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane0 = new JScrollPane(playerPanels[0]);
        scrollPane0.setOpaque(false);
        scrollPane0.getViewport().setOpaque(false);
        scrollPane0.setPreferredSize(new Dimension(400,200));
        leftPanel.add(scrollPane0);
        JScrollPane scrollPane1 = new JScrollPane(playerPanels[1]);
        scrollPane1.setOpaque(false);
        scrollPane1.getViewport().setOpaque(false);
        scrollPane1.setPreferredSize(new Dimension(400,200));
        leftPanel.add(scrollPane1);
        mainPanel.add(leftPanel);

        mainPanel.add(boardView);
        mainPanel.add(Box.createRigidArea(new Dimension(5,0)));

        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane2 = new JScrollPane(playerPanels[2]);
        scrollPane2.setOpaque(false);
        scrollPane2.getViewport().setOpaque(false);
        scrollPane2.setPreferredSize(new Dimension(400,200));
        rightPanel.add(scrollPane2);
        if (playerPanels.length > 3) {
            JScrollPane scrollPane3 = new JScrollPane(playerPanels[3]);
            scrollPane3.setOpaque(false);
            scrollPane3.getViewport().setOpaque(false);
            scrollPane3.setPreferredSize(new Dimension(400,200));
            rightPanel.add(scrollPane3);
        }
        mainPanel.add(rightPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(5,0)));

        wrapper.add(mainPanel, BorderLayout.CENTER);

//        JScrollPane pane2 = new JScrollPane(actionPanel);
//        pane2.setPreferredSize(new Dimension(700, 100));
//        pane2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//        parent.add(pane2, BorderLayout.SOUTH);
        wrapper.add(actionPanel, BorderLayout.SOUTH);

        wrapper.revalidate();
        wrapper.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        return 5000;
    }

    protected void highlightActionOnBoard(ActionButton button) {
        boardView.highlight(button.getButtonAction());
    }
    protected void removeHighlightOnBoard(ActionButton button) {
        boardView.clearHighlight();
    }
    protected void scrollActionPanelToTop(ActionButton button) {
        javax.swing.SwingUtilities.invokeLater(() -> actionScrollPane.getVerticalScrollBar().setValue(0));
    }

    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height, boolean boxLayout,
                                           boolean opaque,
                                           Consumer<ActionButton> onActionSelected,
                                           Consumer<ActionButton> onMouseEnter,
                                           Consumer<ActionButton> onMouseExit) {
        JPanel actionPanel = new JPanel();
//        JPanel actionPanel = new JPanel() {
//            @Override
//            public Dimension getMaximumSize() {
//                return new Dimension(width, super.getMaximumSize().height);
//            }
//
//            @Override
//            public Dimension getPreferredSize() {
//                return new Dimension(width, super.getPreferredSize().height);
//            }
//        };
        actionPanel.setPreferredSize(new Dimension(width, height*10));
        actionPanel.setOpaque(false);

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights, onActionSelected, onMouseEnter, onMouseExit);
            actionButtons[i] = ab;
            actionButtons[i].setVisible(false);
            actionPanel.add(actionButtons[i]);
        }
        for (ActionButton actionButton : actionButtons) {
            actionButton.informAllActionButtons(actionButtons);
        }

        actionScrollPane = new JScrollPane(actionPanel);
        actionScrollPane.setOpaque(false);
        actionScrollPane.getViewport().setOpaque(false);
        actionScrollPane.setPreferredSize(new Dimension(width, height));
        actionScrollPane.setMaximumSize(new Dimension(width, height));
        actionScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        actionScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        if (boxLayout) {
            actionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }
        return actionScrollPane;
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (filterActions) {
            if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING && !(actionButtons == null)) {
                List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState, gameState.getCoreGameParameters().actionSpace);
                int i = 0;
                boolean vertexNotification = false;
                boolean edgeNotification = false;
                for (AbstractAction aa : actions) {
                    if (aa instanceof DeepPlaceSettlementThenRoad) {
                        DeepPlaceSettlementThenRoad a = (DeepPlaceSettlementThenRoad) aa;
                        Pair<Vector2D, Integer> p = new Pair<>(new Vector2D(a.x, a.y), a.vertex);
                        if (!boardView.vertexHighlight.isEmpty()) {
                            if (boardView.vertexHighlight.contains(p)) {
                                actionButtons[i].setVisible(true);
                                actionButtons[i].setEnabled(true);
                                actionButtons[i].setButtonAction(a, gameState);
                                actionButtons[i].setBackground(Color.white);
                                i++;
                            }
                        } else if (!vertexNotification) {
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setEnabled(false);
                            actionButtons[i].setButtonAction(null, "Select vertex on map");
                            actionButtons[i].setBackground(Color.gray);
                            i++;
                            vertexNotification = true;
                        }
                    } else if (aa instanceof BuildRoad) {
                        BuildRoad a = (BuildRoad) aa;
                        Pair<Vector2D, Integer> p = new Pair<>(new Vector2D(a.x, a.y), a.edge);
                        if (!boardView.edgeHighlight.isEmpty()) {
                            if (boardView.edgeHighlight.contains(p)) {
                                actionButtons[i].setVisible(true);
                                actionButtons[i].setEnabled(true);
                                actionButtons[i].setButtonAction(a, gameState);
                                actionButtons[i].setBackground(Color.white);
                                i++;
                            }
                        } else if (!edgeNotification) {
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setEnabled(false);
                            actionButtons[i].setButtonAction(null, "Select edge on map");
                            actionButtons[i].setBackground(Color.gray);
                            i++;
                            edgeNotification = true;
                        }
                    } else {
                        actionButtons[i].setVisible(true);
                        actionButtons[i].setEnabled(true);
                        actionButtons[i].setButtonAction(aa, gameState);
                        actionButtons[i].setBackground(Color.white);
                        i++;
                    }
                }
                for (int j = i; j < actionButtons.length; j++) {
                    actionButtons[j].setVisible(false);
                    actionButtons[j].setButtonAction(null, "");
                }
            }
        }
        else super.updateActionButtons(player, gameState);
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        diceRollLabel.setText("Dice Roll: " + ((CatanGameState) gameState).getRollValue());
        knightCount.setText("Knights: " + gs.getLargestArmyOwner() + " with size " + gs.getLargestArmySize());
        longestRoad.setText("Longest Road: " + gs.getLongestRoadOwner() + " with length " + gs.getLongestRoadLength());
//        victoryPointsLabel.setText("VictoryPoints: " + Arrays.toString(gs.getVictoryPoints()));

        for (int i = 0; i < gameState.getNPlayers(); i++) {
            playerPanels[i]._update((CatanGameState) gameState);
        }

        parent.repaint();
    }


    protected JPanel createGameStateInfoPanel(AbstractGameState gameState) {
        gameInfo = new JPanel();
        gameInfo.setOpaque(false);
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>Catan</h1></html>"));

        updateGameStateInfo(gameState);

        playerColourLabel = new JLabel("Current Player Colour: Red");
        knightCount = new JLabel("Knights: " + Arrays.toString(gs.getKnights()));
        longestRoad = new JLabel("Longest Road: " + gs.getLongestRoadOwner() + " with length " + gs.getLongestRoadLength());
//        victoryPointsLabel = new JLabel("VictoryPoints: " + Arrays.toString(gs.getVictoryPoints()));
        diceRollLabel = new JLabel("Dice Roll: " + ((CatanGameState) gameState).getRollValue());

        gameInfo.add(gameStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(diceRollLabel);

        gameInfo.setPreferredSize(new Dimension(900, 150));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.add(gameInfo, BorderLayout.WEST);
//        wrapper.setLayout(new GridBagLayout());
        return wrapper;
    }

}


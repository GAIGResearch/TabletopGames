package games.catan.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.Counter;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.actions.build.BuildCity;
import games.catan.actions.build.BuildRoad;
import games.catan.actions.build.BuildSettlement;
import games.catan.actions.robber.MoveRobber;
import games.catan.actions.setup.DeepPlaceSettlementThenRoad;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class CatanGUI extends AbstractGUIManager {
    CatanGameState gs;
    CatanBoardView boardView;
    PlayerPanel[] playerPanels;

    JPanel gameInfo;
    JLabel diceRollLabel;
    JLabel knightCount;
    JLabel longestRoad;
    JLabel playerColourLabel;
    JLabel currentOffer;
    JLabel resourcePool;

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
                // TODO: trim what the set of actions currently require
                boolean vertexNotification = false;
                boolean edgeNotification = false;
                boolean tileNotification = false;
                for (AbstractAction aa : actions) {
                    Pair<Point, Integer> vertex = null;
                    Pair<Point, Integer> edge = null;
                    Point tile = null;
                    // TODO: resource highlight for players (Discard, Trade)
                    // TODO: highlight players (MoveRobberAndSteal, StealResource)
                    // TODO: combined filters (PlaceSettlementWithRoad)
                    if (aa instanceof DeepPlaceSettlementThenRoad) {
                        DeepPlaceSettlementThenRoad a = (DeepPlaceSettlementThenRoad) aa;
                        vertex = new Pair<>(new Point(a.x, a.y), a.vertex);
                    } else if (aa instanceof BuildRoad) {
                        BuildRoad a = (BuildRoad) aa;
                        edge = new Pair<>(new Point(a.x, a.y), a.edge);
                    } else if (aa instanceof BuildCity) {
                        BuildCity a = (BuildCity) aa;
                        vertex = new Pair<>(new Point(a.col, a.row), a.vertex);
                    } else if (aa instanceof BuildSettlement) {
                        BuildSettlement a = (BuildSettlement) aa;
                        vertex = new Pair<>(new Point(a.x, a.y), a.vertex);
                    } else if (aa instanceof MoveRobber) {
                        MoveRobber a = (MoveRobber) aa;
                        tile = new Point(a.x, a.y);
                    } else if (aa instanceof PlaceSettlementWithRoad) {
                        PlaceSettlementWithRoad a = (PlaceSettlementWithRoad) aa;
                        vertex = new Pair<>(new Point(a.x, a.y), a.vertex);
                        edge = new Pair<>(new Point(a.x, a.y), a.edge);
                    }

                    // Use vertex filter
                    if (vertex != null && !boardView.vertexHighlight.isEmpty()) {
                        if (boardView.vertexHighlight.contains(vertex)) {
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setEnabled(true);
                            actionButtons[i].setButtonAction(aa, gameState);
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
                    // Use edge filter
                    if (edge != null && !boardView.edgeHighlight.isEmpty()) {
                        if (boardView.edgeHighlight.contains(edge)) {
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setEnabled(true);
                            actionButtons[i].setButtonAction(aa, gameState);
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
                    // Use tile filter
                    if (tile != null && boardView.hexHighlight != null) {
                        if (boardView.hexHighlight.equals(tile)) {
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setEnabled(true);
                            actionButtons[i].setButtonAction(aa, gameState);
                            actionButtons[i].setBackground(Color.white);
                            i++;
                        }
                    } else if (!tileNotification) {
                        actionButtons[i].setVisible(true);
                        actionButtons[i].setEnabled(false);
                        actionButtons[i].setButtonAction(null, "Select tile on map");
                        actionButtons[i].setBackground(Color.gray);
                        i++;
                        tileNotification = true;
                    }
                    // Non-filtered action
                    if (vertex == null && edge == null && tile == null) {
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
        currentOffer.setText("Trade offered: " + (gs.getTradeOffer() != null? gs.getTradeOffer().getString(gameState) : "(none)"));
        resourcePool = new JLabel("Resource pool: " + resourcePrint(gs.getResourcePool(), (CatanParameters) gs.getGameParameters()));
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
        currentOffer = new JLabel("Trade offered: (none)");
        resourcePool = new JLabel("Resource pool: " + resourcePrint(gs.getResourcePool(), (CatanParameters) gs.getGameParameters()));

        gameInfo.add(gameStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(diceRollLabel);
        gameInfo.add(currentOffer);
        gameInfo.add(resourcePool);

        gameInfo.setPreferredSize(new Dimension(900, 170));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.add(gameInfo, BorderLayout.WEST);
//        wrapper.setLayout(new GridBagLayout());
        return wrapper;
    }

    private String resourcePrint(HashMap<CatanParameters.Resource, Counter> resourcePool, CatanParameters cp) {
        String s = "";
        for (CatanParameters.Resource res: resourcePool.keySet()) {
            if (res == CatanParameters.Resource.WILD) continue;
            if (gs.getCoreGameParameters().alwaysDisplayFullObservable) {
                s += res + "=" + resourcePool.get(res).getValue() + "/" + cp.n_resource_cards + ", ";
            } else {
                s += res + "=" + CatanParameters.ResourceAmount.translate(resourcePool.get(res).getValue(), cp).po + ", ";
            }
        }
        s += "]";
        return s.replace(", ]", "");
    }

}


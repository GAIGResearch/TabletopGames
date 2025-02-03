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
import games.catan.actions.discard.DiscardResources;
import games.catan.actions.robber.MoveRobber;
import games.catan.actions.robber.MoveRobberAndSteal;
import games.catan.actions.robber.StealResource;
import games.catan.actions.setup.DeepPlaceSettlementThenRoad;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import games.catan.actions.trade.DefaultTrade;
import games.catan.actions.trade.OfferPlayerTrade;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class CatanGUI extends AbstractGUIManager implements IScreenHighlight {
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

    JLabel actionFilterTooltips;

    JScrollPane actionScrollPane;

    boolean filterActions = true;

    List<CatanParameters.Resource> resourceHighlights = new ArrayList<>();
    int maxHighlights = 2;
    void addResourceHighlight(CatanParameters.Resource r) {
        if (resourceHighlights.size() == maxHighlights) resourceHighlights.remove(0);
        resourceHighlights.add(r);
    }
    Set<Integer> playerHighlight = new HashSet<>();
    void addPlayerHighlight(int p) {
        playerHighlight.clear();
        playerHighlight.add(p);
    }

    public CatanGUI(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        if (game == null) return;
        this.gs = (CatanGameState) game.getGameState();

        boardView = new CatanBoardView(gs);

        // Bottom area will show actions available
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{boardView, this}, 800, defaultActionPanelHeight, false, false, this::scrollActionPanelToTop, this::highlightActionOnBoard, null);
        actionFilterTooltips = new JLabel("Filtering: " + filterActions);
        JPanel bottomWrap = new JPanel();
        bottomWrap.setLayout(new BoxLayout(bottomWrap, BoxLayout.X_AXIS));
        bottomWrap.add(actionPanel);
        bottomWrap.add(Box.createRigidArea(new Dimension(10, 0)));
        bottomWrap.add(actionFilterTooltips);

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
            playerPanels[i] = new PlayerPanel(this, i, game.getPlayers().get(i).toString());
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
        wrapper.add(bottomWrap, BorderLayout.SOUTH);

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
        boardView.clearHighlights();
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

    public enum Filter {
        Vertex(Pair.class, "Select vertex on map"), // <Point, Integer>
        Edge(Pair.class, "Select edge on map"),  // <Point, Integer>
        Tile(Point.class, "Select hex tile on map"),
        Resource(CatanParameters.Resource[].class, "Select resource(s) in player panel"),
        Player(Integer.class, "Select target player name in player panel");
        final Class<?> type;
        final String tooltip;
        Collection<?> highlightRef;
        public Object ref;
        Filter(Class<?> type, String tooltip) {
            this.type = type;
            this.tooltip = tooltip;
        }
        boolean filter() {
            if (this == Resource) {
                boolean show = true;
                for (Object r: highlightRef) {
                    if (!contains((CatanParameters.Resource[]) ref, (CatanParameters.Resource) r)) {
                        show = false;
                        break;
                    }
                }
                return show;
            }
            else return highlightRef.contains(ref);
        }
    }
    Set<Filter> filtersAvailable = new HashSet<>();  // TODO: list these next to action panel with tooltip

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (filterActions) {
            Filter.Vertex.highlightRef = boardView.vertexHighlight;
            Filter.Edge.highlightRef = boardView.edgeHighlight;
            Filter.Tile.highlightRef = boardView.hexHighlight;
            Filter.Resource.highlightRef = resourceHighlights;
            Filter.Player.highlightRef = playerHighlight;

            if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING && !(actionButtons == null)) {
                List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState, gameState.getCoreGameParameters().actionSpace);
                int i = 0;
                // Trim what the set of actions currently require
                filtersAvailable.clear();
                for (AbstractAction aa : actions) {
                    if (aa instanceof DeepPlaceSettlementThenRoad) {
                        DeepPlaceSettlementThenRoad a = (DeepPlaceSettlementThenRoad) aa;
                        Filter f = Filter.Vertex;
                        f.ref = new Pair<>(new Point(a.x, a.y), a.vertex);
                        filtersAvailable.add(f);
                    } else if (aa instanceof BuildRoad) {
                        BuildRoad a = (BuildRoad) aa;
                        Filter f = Filter.Edge;
                        f.ref = new Pair<>(new Point(a.x, a.y), a.edge);
                        filtersAvailable.add(f);
                    } else if (aa instanceof BuildCity) {
                        BuildCity a = (BuildCity) aa;
                        Filter f = Filter.Vertex;
                        f.ref = new Pair<>(new Point(a.col, a.row), a.vertex);
                        filtersAvailable.add(f);
                    } else if (aa instanceof BuildSettlement) {
                        BuildSettlement a = (BuildSettlement) aa;
                        Filter f = Filter.Vertex;
                        f.ref = new Pair<>(new Point(a.x, a.y), a.vertex);
                        filtersAvailable.add(f);
                    } else if (aa instanceof MoveRobberAndSteal) {
                        MoveRobberAndSteal a = (MoveRobberAndSteal) aa;
                        Filter f1 = Filter.Player;
                        f1.ref = a.targetPlayer;
                        filtersAvailable.add(f1);
                        Filter f2 = Filter.Tile;
                        f2.ref = new Point(a.x, a.y);
                        filtersAvailable.add(f2);
                    } else if (aa instanceof MoveRobber) {
                        MoveRobber a = (MoveRobber) aa;
                        Filter f2 = Filter.Tile;
                        f2.ref = new Point(a.x, a.y);
                        filtersAvailable.add(f2);
                    } else if (aa instanceof PlaceSettlementWithRoad) {
                        PlaceSettlementWithRoad a = (PlaceSettlementWithRoad) aa;
                        Filter f = Filter.Vertex;
                        f.ref = new Pair<>(new Point(a.x, a.y), a.vertex);
                        filtersAvailable.add(f);
                        Filter f2 = Filter.Edge;
                        f2.ref = new Pair<>(new Point(a.x, a.y), a.edge);
                        filtersAvailable.add(f2);
                    } else if (aa instanceof DiscardResources) {
                        DiscardResources a = (DiscardResources) aa;
                        Filter f = Filter.Resource;
                        f.ref = a.resourcesToDiscard;
                        filtersAvailable.add(f);
                    } else if (aa instanceof DefaultTrade) {
                        DefaultTrade a = (DefaultTrade) aa;
                        Filter f = Filter.Resource;
                        f.ref = new CatanParameters.Resource[] {a.resourceOffer, a.resourceToGet};
                        filtersAvailable.add(f);
                    } else if (aa instanceof OfferPlayerTrade) {
                        OfferPlayerTrade a = (OfferPlayerTrade) aa;
                        Filter f = Filter.Resource;
                        f.ref = new CatanParameters.Resource[] {a.resourceOffered, a.resourceRequested};
                        filtersAvailable.add(f);
                        Filter f1 = Filter.Player;
                        f1.ref = a.otherPlayerID;
                        filtersAvailable.add(f1);
                    } else if (aa instanceof StealResource) {
                        StealResource a = (StealResource) aa;
                        Filter f1 = Filter.Player;
                        f1.ref = a.targetPlayerID;
                        filtersAvailable.add(f1);
                    } else {
                        // Non-filtered action
                        actionButtons[i].setVisible(true);
                        actionButtons[i].setEnabled(true);
                        actionButtons[i].setButtonAction(aa, gameState);
                        actionButtons[i].setBackground(Color.white);
                        actionButtons[i].setForeground(Color.BLACK);
                        i++;
                        continue;
                    }

                    // Apply filters, considering multiple ones together
                    boolean showAction = true;
                    for (Filter f: filtersAvailable) {
                        if (!f.highlightRef.isEmpty()) {
                            // Apply filter
                            if (!f.filter()) {
                                showAction = false;
                                break;
                            }
                        }
                    }
                    if (showAction) {
                        actionButtons[i].setVisible(true);
                        actionButtons[i].setEnabled(true);
                        actionButtons[i].setButtonAction(aa, gameState);
                        actionButtons[i].setBackground(Color.white);
                        actionButtons[i].setForeground(Color.BLACK);
                        i++;
                    }
                }

                for (Filter f: filtersAvailable) {
                    if (f.highlightRef.isEmpty()) {
                        // Tooltip
                        actionButtons[i].setVisible(true);
                        actionButtons[i].setEnabled(false);
                        actionButtons[i].setButtonAction(null, f.tooltip);
                        actionButtons[i].setBackground(Color.darkGray);
                        actionButtons[i].setForeground(Color.white);
                        i++;
                    }
                }

                // Disable the rest of the actions
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

        for (int i = 0; i < gameState.getNPlayers(); i++) {
            playerPanels[i]._update((CatanGameState) gameState);
        }

        String s = "<html><h3>Filtering actions? " + filterActions + "." + (filterActions? " Filters available:" : "") +"</h3><hr>";
        if (filterActions) {
            s += "<ul>";
            for (Filter f : filtersAvailable) {
                s += "<li>" + f.name() + ": " + f.tooltip + "</li>";
            }
            s += "</ul>";
        }
        s += "</html>";
        actionFilterTooltips.setText(s);

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
        return wrapper;
    }

    private String resourcePrint(Map<CatanParameters.Resource, Counter> resourcePool, CatanParameters cp) {
        StringBuilder s = new StringBuilder();
        for (CatanParameters.Resource res: resourcePool.keySet()) {
            if (res == CatanParameters.Resource.WILD) continue;
            if (gs.getCoreGameParameters().alwaysDisplayFullObservable) {
                s.append(res).append("=").append(resourcePool.get(res).getValue()).append("/").append(cp.n_resource_cards).append(", ");
            } else {
                s.append(res).append("=").append(CatanParameters.ResourceAmount.translate(resourcePool.get(res).getValue(), cp).po).append(", ");
            }
        }
        s.append("]");
        return s.toString().replace(", ]", "");
    }

    @Override
    public void clearHighlights() {
        resourceHighlights.clear();
        playerHighlight.clear();
    }

    static boolean contains(CatanParameters.Resource[] arr, CatanParameters.Resource r) {
        for (CatanParameters.Resource res: arr) if (res == r) return true;
        return false;
    }
}


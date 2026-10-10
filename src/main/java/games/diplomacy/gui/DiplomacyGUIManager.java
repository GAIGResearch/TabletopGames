package games.diplomacy.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.diplomacy.*;
import games.diplomacy.actions.*;
import gui.AbstractGUIManager;
import gui.IMovePlanner;
import gui.MapMove;
import gui.MapRegion;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * GUI for Diplomacy: the map (DiplomacyMapView) beside the powers (DiplomacyPowersView) and a list of the orders of
 * the last resolved phase, with the action buttons below, a Rules tab and a How to Play tab. The framework's history
 * panel is left out: it would show each order as it is given, while the other powers' orders are hidden until the
 * phase is resolved. A human player may also give an order by clicking on the map: on the unit being ordered to hold
 * it (or disband it), or on the province an order is aimed at (a move or retreat there, a support or convoy into it,
 * a build in it); when several orders are aimed at the province, a menu lists them.
 */
public class DiplomacyGUIManager extends AbstractGUIManager {

    static final int ACTION_HEIGHT = 130;
    static final Color CLICKABLE = new Color(240, 200, 0), ORDERING = new Color(30, 170, 60);

    DiplomacyMapView mapView;
    DiplomacyPowersView powersView;
    JTextArea lastOrders;
    // the state last shown (a copy), read by the click handler and the tooltips
    DiplomacyGameState shown;
    DiplomacyPlanner planner;

    public DiplomacyGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        DiplomacyGameState state = (DiplomacyGameState) gameState;
        DiplomacyParameters params = (DiplomacyParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        String[] agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            agentNames[i] = game.getPlayers().get(i).toString();
        }

        shown = state;
        planner = new DiplomacyPlanner((DiplomacyForwardModel) game.getForwardModel());
        mapView = new DiplomacyMapView(params.getMap(), p -> showHiddenInfo(shown, p));
        mapView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clicked(e);
            }
        });
        mapView.setToolTips(this::toolTip);
        powersView = new DiplomacyPowersView(agentNames);
        lastOrders = new JTextArea();
        lastOrders.setEditable(false);
        lastOrders.setFont(new Font("SansSerif", Font.PLAIN, 11));
        mapView.update(state);
        powersView.update(state);

        Dimension mapSize = mapView.getPreferredSize();
        Dimension powersSize = powersView.getPreferredSize();
        int rightWidth = powersSize.width;
        JScrollPane ordersPane = new JScrollPane(lastOrders);
        ordersPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        TitledBorder border = BorderFactory.createTitledBorder("Last orders (x = failed)");
        ordersPane.setBorder(border);
        Dimension ordersSize = new Dimension(rightWidth, mapSize.height - powersSize.height - 5);
        ordersPane.setPreferredSize(ordersSize);
        ordersPane.setMinimumSize(ordersSize);
        ordersPane.setMaximumSize(ordersSize);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(powersView);
        right.add(Box.createVerticalStrut(5));
        right.add(ordersPane);

        this.width = mapSize.width + rightWidth + 20;
        this.height = mapSize.height + 10;

        JTabbedPane tabs = new JTabbedPane();
        JPanel main = new JPanel(new BorderLayout());
        tabs.add("Game", main);
        RulesView.addTabs(tabs, GameType.Diplomacy, params, height + ACTION_HEIGHT);

        JPanel gameArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gameArea.add(mapView);
        gameArea.add(right);

        // a fleet at sea may be offered dozens of convoys and supports: the panel scrolls
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, ACTION_HEIGHT);
        main.add(gameArea, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + ACTION_HEIGHT + 50));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // the most actions at one decision are a unit's hold, moves, supports and, for a fleet at sea, convoys: the
        // most seen in 30 random games to 1912 was 100 (a fleet in the North Sea), and the panel scrolls; a plan (see
        // DiplomacyPlanner) offers the orders of all the power's units at once
        return 1500;
    }

    @Override
    public void update(AbstractPlayer player, AbstractGameState gameState, boolean showActions) {
        super.update(player, gameState, showActions);
        // after the actions are offered (or withdrawn) for this state
        showClickable();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        DiplomacyGameState state = (DiplomacyGameState) gameState;
        shown = state;
        mapView.update(state);
        powersView.update(state);
        StringBuilder text = new StringBuilder();
        for (int p = 0; p < state.getNPlayers(); p++) {
            boolean first = true;
            for (DiplomacyResult r : state.getLastResults())
                if (r.power() == p) {
                    if (first) {
                        if (!text.isEmpty()) text.append("\n");
                        text.append(state.getMap().powers().get(p)).append("\n");
                        first = false;
                    }
                    text.append("   ").append(r.order()).append(r.success() ? "" : "   x").append("\n");
                }
        }
        if (text.isEmpty())
            text.append("None yet");
        lastOrders.setText(text.toString());
        lastOrders.setCaretPosition(0);
    }

    /**
     * The province an order is aimed at: where a move or retreat goes, the unit a hold support or the province a move
     * support or convoy is for, where a unit is built; the unit's own province for a hold or disband. Null for
     * WaiveBuilds, which only a button gives.
     */
    static DiplomacyProvince target(DiplomacyOrder order) {
        if (order instanceof Move m) return m.to.province();
        if (order instanceof Retreat r) return r.to.province();
        if (order instanceof SupportHold s) return s.supported;
        if (order instanceof SupportMove s) return s.to;
        if (order instanceof Convoy c) return c.to;
        if (order instanceof Build b) return b.location.province();
        if (order instanceof WaiveBuilds) return null;
        return order.province();
    }

    /**
     * The offered orders aimed at the province, in the order offered.
     */
    private List<DiplomacyOrder> ordersAt(DiplomacyProvince province) {
        return clickable.matching(DiplomacyOrder.class, o -> province.equals(target(o)));
    }

    @Override
    public List<MapRegion> getMapRegions() {
        if (mapView == null) return List.of();
        List<MapRegion> regions = new ArrayList<>();
        for (DiplomacyProvince p : shown.getMap().provinces())
            regions.add(new MapRegion(p.name(), p.fullName(), mapView, mapView.provinceShape(p)));
        return regions;
    }

    /**
     * An order is chosen by pointing at the unit ordered (or where a unit is built) and then at the province the
     * order is aimed at (see target).
     */
    @Override
    public MapMove getMapMove(AbstractAction action) {
        if (!(action instanceof DiplomacyOrder o) || o.province() == null) return null;
        return new MapMove(o.province().name(), target(o).name());
    }

    @Override
    public IMovePlanner getPlanner() {
        return planner;
    }

    /**
     * A left click on a province gives the one offered order aimed at it, or opens a menu of them when there are
     * several.
     */
    private void clicked(MouseEvent e) {
        if (!clickable.isOffered() || shown == null || e.getButton() != MouseEvent.BUTTON1) return;
        DiplomacyProvince province = mapView.provinceAt(e.getPoint());
        if (province == null) return;
        List<DiplomacyOrder> orders = ordersAt(province);
        if (orders.size() == 1) {
            clickable.submit(orders.get(0));
        } else if (orders.size() > 1) {
            JPopupMenu menu = new JPopupMenu();
            for (DiplomacyOrder o : orders) {
                JMenuItem item = new JMenuItem(o.getString(shown));
                item.addActionListener(a -> {
                    clickable.submit(o);
                    showClickable();
                });
                menu.add(item);
            }
            menu.show(mapView, e.getX(), e.getY());
        }
        showClickable();
    }

    /**
     * Outlines the unit being ordered in green and every province a click may give an order for in yellow; nothing
     * when no human player is to act.
     */
    private void showClickable() {
        if (mapView == null) return;
        Map<DiplomacyProvince, Color> outlines = new HashMap<>();
        Set<DiplomacyProvince> units = new HashSet<>();
        for (DiplomacyOrder o : clickable.matching(DiplomacyOrder.class, o -> o instanceof Hold || o instanceof Retreat))
            units.add(o.province());
        if (units.size() > 1) {
            // the orders for several units at once (a plan, see DiplomacyPlanner): the units, not every province
            // some order is aimed at
            units.forEach(p -> outlines.put(p, ORDERING));
        } else if (clickable.isOffered()) {
            for (DiplomacyOrder o : clickable.matching(DiplomacyOrder.class, o -> true)) {
                DiplomacyProvince target = target(o);
                if (target != null)
                    outlines.put(target, CLICKABLE);
            }
            for (DiplomacyOrder o : clickable.matching(DiplomacyOrder.class, o -> o instanceof Hold || o instanceof Retreat))
                outlines.put(o.province(), ORDERING);
        }
        mapView.setOutlines(outlines);
    }

    /**
     * The province under the mouse: its name, supply centre owner and unit, and when a human player is to act, the
     * orders a click there would give, with what the player can see that bears on them.
     */
    private String toolTip(MouseEvent e) {
        if (shown == null) return null;
        DiplomacyProvince p = mapView.provinceAt(e.getPoint());
        if (p == null) return null;
        StringBuilder text = new StringBuilder("<html><b>").append(p.fullName()).append("</b> (").append(p.name()).append(")");
        if (p.type() != DiplomacyProvince.Type.SEA) {
            int owner = shown.getOwner(p);
            text.append("<br>").append(p.supplyCentre() ? "Supply centre, " : "")
                    .append(owner < 0 ? "uncontrolled" : "controlled by " + shown.getMap().powers().get(owner));
            if (p.home() >= 0) text.append(" (home of ").append(shown.getMap().powers().get(p.home())).append(")");
        }
        DiplomacyUnit u = shown.getUnit(p);
        text.append("<br>").append(u == null ? "Empty" : describe(u));
        DiplomacyUnit d = shown.getDislodged(p);
        if (d != null) text.append("<br>Dislodged: ").append(describe(d));
        if (shown.isStandoff(p) && shown.getPhase().isRetreats())
            text.append("<br>Left empty by a standoff: no retreat here");
        if (clickable.isOffered()) {
            List<DiplomacyOrder> orders = ordersAt(p);
            // the orders of several units at once, in a plan (see DiplomacyPlanner), are chosen unit by unit
            boolean planning = clickable.matching(DiplomacyOrder.class, o -> o instanceof Hold || o instanceof Retreat)
                    .stream().map(DiplomacyOrder::province).distinct().count() > 1;
            if (orders.isEmpty()) {
                if (!planning) text.append("<br><i>No order for the unit being ordered is aimed here</i>");
            } else if (planning) {
                text.append("<br>Orders that could be aimed here:");
            } else {
                text.append(orders.size() == 1 ? "<br>Click to order:" : "<br>Click to choose one of:");
                for (DiplomacyOrder o : orders)
                    text.append("<br>&nbsp;&nbsp;").append(o.getString(shown)).append(consequence(o));
            }
        }
        return text.append("</html>").toString();
    }

    private String describe(DiplomacyUnit u) {
        return shown.getMap().powers().get(u.owner()) + " " + (u.isFleet() ? "fleet" : "army")
                + (u.coast().isEmpty() ? "" : " (" + u.coast() + ")");
    }

    /**
     * What the player can see that bears on the order: who is in the province a move goes to, and whose unit a
     * support or convoy helps.
     */
    private String consequence(DiplomacyOrder o) {
        int me = clickable.player();
        DiplomacyUnit helped = null;
        if (o instanceof Move m) {
            DiplomacyUnit there = shown.getUnit(m.to.province());
            if (there == null) return " - empty now";
            return there.owner() == me ? " - your unit is there, and must move away" : " - attacks the " + describe(there);
        }
        if (o instanceof SupportMove s) helped = shown.getUnit(s.from);
        if (o instanceof SupportHold s) helped = shown.getUnit(s.supported);
        if (o instanceof Convoy c) helped = shown.getUnit(c.from);
        if (helped == null) return "";
        return helped.owner() == me ? " - helps your own unit" : " - helps the " + describe(helped);
    }
}

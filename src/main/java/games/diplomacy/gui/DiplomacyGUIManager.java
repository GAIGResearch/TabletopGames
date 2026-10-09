package games.diplomacy.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.diplomacy.*;
import games.diplomacy.actions.*;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
        }

        shown = state;
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
        tabs.add("Rules", new RulesView(rulesHtml(params), height + ACTION_HEIGHT));
        tabs.add("How to Play", new RulesView(HOW_TO_PLAY, height + ACTION_HEIGHT));

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
        // most seen in 30 random games to 1912 was 100 (a fleet in the North Sea), and the panel scrolls
        return 400;
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
        if (clickable.isOffered()) {
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
            if (orders.isEmpty()) {
                text.append("<br><i>No order for the unit being ordered is aimed here</i>");
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

    static final String HOW_TO_PLAY = "<h2>How to play</h2>"
            + "<p>In each phase you give one order at a time, for the unit whose province is outlined in green. "
            + "Every province where a click gives an order is outlined in yellow.</p>"
            + "<ul><li><b>Hold</b>: click the unit being ordered.</li>"
            + "<li><b>Move</b>: click the province to move to.</li>"
            + "<li><b>Support</b>: click the province the support goes into. For a support to hold, that is the "
            + "province of the unit supported.</li>"
            + "<li><b>Convoy</b>: click the province the army is to be convoyed to.</li>"
            + "<li><b>Retreat</b>: click the province to retreat to, or the dislodged unit's own province to "
            + "disband it.</li>"
            + "<li><b>Build</b> and <b>disband</b>: click the home supply centre to build in, or the unit to "
            + "disband.</li></ul>"
            + "<p>When one order is aimed at the province you click, it is given at once. When several are (a move "
            + "and supports into the same province, a choice of coasts, a move by land or via convoy), a menu lists "
            + "them: choose one, or click elsewhere to close it.</p>"
            + "<p>Hover over any province for its name, who controls it, the unit in it, and the orders a "
            + "click there would give, with who they would attack or help.</p>"
            + "<p>The action buttons below the map offer the same orders. <b>Waive builds</b> is only on the "
            + "buttons.</p>";

    static String rulesHtml(DiplomacyParameters params) {
        DiplomacyMap map = params.getMap();
        return "<h2>Diplomacy</h2>"
                + "<p><a href='#year'>The year</a> - <a href='#orders'>Orders</a> - <a href='#resolution'>Resolution</a>"
                + " - <a href='#retreats'>Retreats</a> - <a href='#adjustments'>Builds and disbands</a>"
                + " - <a href='#interface'>Interface</a></p>"
                + "<p>Seven powers fight for the supply centres of Europe. A power that controls " + map.victoryCentres()
                + " supply centres after a Fall turn wins. Otherwise the game ends after the Fall turn of "
                + params.lastYear + ", and the power with the most supply centres wins (powers with the same number "
                + "share the win). There is no negotiation in this implementation.</p>"
                + "<a name='year'></a><h3>The year</h3>"
                + "<p>Each year has a Spring turn and a Fall turn. Each turn has an orders phase and, if any unit was "
                + "dislodged, a retreat phase. After the Fall turn every supply centre with a unit in it comes under "
                + "that unit's power, and the powers then build or disband units to match their supply centres. (The "
                + "other land provinces change hands at the same time, but only the supply centres count.)</p>"
                + "<p>In each phase the powers give their orders one at a time, one order per unit. The orders stay "
                + "hidden until every power has given them; then all are carried out together.</p>"
                + "<a name='orders'></a><h3>Orders</h3>"
                + "<ul><li><b>Hold</b>: the unit stays.</li>"
                + "<li><b>Move</b>: an army moves to an adjacent land or coastal province, and a fleet to an adjacent "
                + "sea or coastal province along the coast. A fleet entering Spain, St. Petersburg or Bulgaria names "
                + "the coast.</li>"
                + "<li><b>Support</b> (S): the unit adds 1 to the strength of a unit holding, or moving, in a province "
                + "it could move to itself.</li>"
                + "<li><b>Convoy</b> (C): a fleet at sea carries an army across the water. A chain of fleets can carry "
                + "it over several seas. A move marked <i>via convoy</i> goes by sea even where it could go by land. "
                + "An army is also convoyed when a fleet of its own power convoys it.</li></ul>"
                + "<a name='resolution'></a><h3>Resolution</h3>"
                + "<ul><li>Every unit has strength 1, plus 1 for each support that is not cut.</li>"
                + "<li>A move succeeds if it is stronger than the unit holding there and than every other move into "
                + "the same province. Moves of equal strength into one province all fail (a standoff).</li>"
                + "<li>A unit that is beaten by a move into its province is dislodged.</li>"
                + "<li>A support is cut if its unit is attacked from any province except the one it supports into, "
                + "or if its unit is dislodged.</li>"
                + "<li>A power cannot dislodge its own unit, and its supports do not count against its own unit. "
                + "An attack by a power on its own unit does not cut support.</li>"
                + "<li>Two units cannot swap places unless one of them is convoyed.</li>"
                + "<li>A convoy fails only if every route it could take has a fleet dislodged.</li>"
                + (params.paradoxRule == DiplomacyParadoxRule.SZYKMAN
                ? "<li>In a convoy paradox the convoying fleets hold (the Szykman rule).</li>"
                : "<li>A convoyed army does not cut the support of an attack on one of its convoying fleets, unless "
                + "it has another route (rules 21 and 22 of the 2000 rulebook). In any other convoy paradox the "
                + "convoying fleets hold (the Szykman rule).</li>")
                + "</ul>"
                + "<a name='retreats'></a><h3>Retreats</h3>"
                + "<p>A dislodged unit retreats to an adjacent province it could move to, that is empty, that is not "
                + "the province its attacker came from (unless the attacker was convoyed) and that was not left empty "
                + "by a standoff. Two units retreating to one province are both disbanded. A unit with no retreat is "
                + "disbanded at once, and any unit may be disbanded instead of retreating.</p>"
                + "<a name='adjustments'></a><h3>Builds and disbands</h3>"
                + "<p>A power with more supply centres than units may build one unit for each, in its home supply "
                + "centres that it still controls and that are empty. An army may be built in any of them, a fleet "
                + "only on a coast. <b>Waive builds</b> gives up the rest. A power with more units than supply "
                + "centres disbands the units of its choice.</p>"
                + "<a name='interface'></a><h3>Interface</h3>"
                + "<ul><li>Each land province is shaded in the colour of the power controlling it, and left buff if "
                + "nobody does. A star marks a supply centre. The grey hatched land is impassable.</li>"
                + "<li>A cannon in a power's colour is an army, and a ship is a fleet. A red-bordered unit beside "
                + "its province has been dislodged and must retreat.</li>"
                + "<li>The last orders carried out are drawn on the map: black arrows for moves that succeeded, red "
                + "dashed arrows ending in a cross for moves that failed, green dotted lines for supports, blue for "
                + "convoys and orange arrows for retreats. A dashed ring marks a unit built, and a red cross a unit "
                + "disbanded. Purple shows the orders given so far this phase by the "
                + "powers you may see (your own, when you play).</li>"
                + "<li>On the right are the powers, with their supply centres and units, and the list of last "
                + "orders.</li>"
                + "<li>The action buttons are the orders for the unit being ordered now. You may also click on "
                + "the map: see How to Play.</li></ul>";
    }
}

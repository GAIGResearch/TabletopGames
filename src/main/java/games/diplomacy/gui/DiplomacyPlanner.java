package games.diplomacy.gui;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.diplomacy.*;
import games.diplomacy.actions.*;
import gui.IMovePlanner;

import java.util.*;

/**
 * Plans a power's orders for a phase: the orders for its units in any order (changing any of them), or its builds or
 * disbands, sent together. The forward model asks for the orders unit by unit, in its own order; each is answered
 * with the planned order for that unit, or with a hold (in a retreat phase, a disband) for a unit given none, as the
 * rules have it. Unused builds are waived.
 */
public class DiplomacyPlanner implements IMovePlanner {

    private final DiplomacyForwardModel fm;

    public DiplomacyPlanner(DiplomacyForwardModel fm) {
        this.fm = fm;
    }

    @Override
    public boolean plans(AbstractGameState state, int player) {
        DiplomacyGameState s = (DiplomacyGameState) state;
        // from the power's first decision of the phase
        return s.isNotTerminal() && s.getCurrentPlayer() == player && s.getOrders(player).isEmpty();
    }

    /**
     * In an orders or retreat phase, the orders for each of the power's units to order (those it has given an order
     * too, to change it); in the adjustment phase, the builds or disbands the forward model offers next.
     */
    @Override
    public List<AbstractAction> options(AbstractGameState planned, int player) {
        DiplomacyGameState s = (DiplomacyGameState) planned;
        if (s.getPhase() == DiplomacyPhase.ADJUSTMENTS)
            return s.hasOrdersToGive(player) ? fm.computeAvailableActions(s) : List.of();
        List<AbstractAction> options = new ArrayList<>();
        for (DiplomacyProvince p : units(s, player))
            options.addAll(fm.ordersFor(s, p));
        return options;
    }

    /**
     * The provinces of the power's units to order this phase (its dislodged units in a retreat phase), in index
     * order.
     */
    static List<DiplomacyProvince> units(DiplomacyGameState s, int power) {
        List<DiplomacyProvince> list = new ArrayList<>();
        for (DiplomacyProvince p : s.getMap().provinces()) {
            DiplomacyUnit u = s.getPhase().isRetreats() ? s.getDislodged(p) : s.getUnit(p);
            if (u != null && u.owner() == power)
                list.add(p);
        }
        return list;
    }

    /**
     * Records the order, without passing the turn on as the forward model would.
     */
    @Override
    public void apply(AbstractGameState planned, AbstractAction action) {
        action.execute(planned);
    }

    /**
     * An order replaces the one for the same unit (or a build in the same province).
     */
    @Override
    public boolean replaces(AbstractAction planned, AbstractAction chosen) {
        return planned instanceof DiplomacyOrder a && chosen instanceof DiplomacyOrder b
                && a.province() != null && a.province().equals(b.province());
    }

    @Override
    public AbstractAction fallback(AbstractGameState state, List<AbstractAction> available) {
        for (AbstractAction a : available)
            if (a instanceof Hold || a instanceof WaiveBuilds)
                return a;
        // a dislodged unit given no retreat is disbanded; a disband the power must make is left to it
        if (((DiplomacyGameState) state).getPhase().isRetreats())
            for (AbstractAction a : available)
                if (a instanceof Disband)
                    return a;
        return null;
    }

    @Override
    public List<String> warnings(AbstractGameState planned, int player) {
        DiplomacyGameState s = (DiplomacyGameState) planned;
        List<String> warnings = new ArrayList<>();
        List<DiplomacyOrder> orders = s.getOrders(player);
        Map<DiplomacyProvince, DiplomacyOrder> byUnit = new HashMap<>();
        for (DiplomacyOrder o : orders)
            byUnit.put(o.province(), o);
        if (s.getPhase() == DiplomacyPhase.ADJUSTMENTS) {
            int adjustment = s.adjustment(player);
            if (adjustment < 0 && orders.size() < -adjustment)
                warnings.add(plural(-adjustment - orders.size(), "more unit") + " to disband.");
            else if (adjustment > 0 && s.hasOrdersToGive(player))
                warnings.add(plural(adjustment - orders.size(), "build") + " not used will be given up.");
            return warnings;
        }
        List<String> unordered = new ArrayList<>();
        for (DiplomacyProvince p : units(s, player))
            if (!byUnit.containsKey(p))
                unordered.add(p.fullName());
        if (!unordered.isEmpty())
            warnings.add((s.getPhase().isRetreats() ? "No retreat, so disbanded: " : "No order, so holding: ")
                    + String.join(", ", unordered) + ".");
        // two of the power's own units ordered into the same province stand each other off
        Map<DiplomacyProvince, List<DiplomacyProvince>> into = new LinkedHashMap<>();
        for (DiplomacyOrder o : orders) {
            DiplomacyLocation to = o instanceof Move m ? m.to : o instanceof Retreat r ? r.to : null;
            if (to != null)
                into.computeIfAbsent(to.province(), k -> new ArrayList<>()).add(o.province());
        }
        into.forEach((to, from) -> {
            if (from.size() > 1)
                warnings.add(names(from) + " are all ordered into " + to.fullName()
                        + (s.getPhase().isRetreats() ? ", so all are disbanded." : ", so none of them gets there."));
        });
        for (DiplomacyOrder o : orders) {
            if (o instanceof SupportMove sm && isOwn(s, sm.from, player) && !moves(byUnit.get(sm.from), sm.to, null))
                warnings.add(sm.unit.fullName() + " supports a move from " + sm.from.fullName() + " to "
                        + sm.to.fullName() + " that is not ordered.");
            if (o instanceof SupportHold sh && isOwn(s, sh.supported, player) && byUnit.get(sh.supported) instanceof Move)
                warnings.add(sh.unit.fullName() + " supports " + sh.supported.fullName()
                        + " holding, but it is ordered to move.");
            if (o instanceof Convoy c && isOwn(s, c.from, player) && !moves(byUnit.get(c.from), c.to, true))
                warnings.add(c.unit.fullName() + " convoys " + c.from.fullName() + " to " + c.to.fullName()
                        + ", but that army is not ordered there by convoy.");
        }
        return warnings;
    }

    private static boolean isOwn(DiplomacyGameState s, DiplomacyProvince p, int player) {
        DiplomacyUnit u = s.getUnit(p);
        return u != null && u.owner() == player;
    }

    /**
     * Whether the order is a move into the province (by convoy or not, if viaConvoy is null).
     */
    private static boolean moves(DiplomacyOrder order, DiplomacyProvince to, Boolean viaConvoy) {
        return order instanceof Move m && m.to.province().equals(to) && (viaConvoy == null || m.viaConvoy == viaConvoy);
    }

    private static String names(List<DiplomacyProvince> provinces) {
        List<String> names = provinces.stream().map(DiplomacyProvince::fullName).toList();
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }

    private static String plural(int n, String what) {
        return n + " " + what + (n == 1 ? "" : "s");
    }

    @Override
    public String sendLabel() {
        return "Send orders";
    }
}

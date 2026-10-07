package games.diplomacy;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import games.diplomacy.actions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The powers give their orders in turn, one order per action, rather than simultaneously (a joint action over seven
 * powers would be far too large). The orders stay hidden from the other powers until all are given, and the phase is
 * then resolved together.
 */
public class DiplomacyForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DiplomacyGameState state = (DiplomacyGameState) firstState;
        DiplomacyMap map = state.getMap();
        if (state.getNPlayers() != map.nPowers())
            throw new IllegalArgumentException("Diplomacy on " + map.fileName + " is for " + map.nPowers()
                    + " players, not " + state.getNPlayers());
        int n = map.nProvinces();
        state.units = new DiplomacyUnit[n];
        state.owner = new int[n];
        Arrays.fill(state.owner, -1);
        for (int power = 0; power < map.nPowers(); power++) {
            for (Map.Entry<DiplomacyLocation, DiplomacyUnit.Type> e : map.startingUnits(power).entrySet())
                state.units[e.getKey().province().index()] =
                        new DiplomacyUnit(e.getValue(), power, e.getKey().coast());
            for (DiplomacyProvince home : map.homeCentres(power))
                state.owner[home.index()] = power;
        }
        state.year = map.startYear();
        state.orders = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++)
            state.orders.add(new ArrayList<>());
        state.dislodged = new DiplomacyUnit[n];
        state.dislodgedFrom = new int[n];
        Arrays.fill(state.dislodgedFrom, -1);
        state.standoff = new boolean[n];
        state.lastResults = new ArrayList<>();
        state.setGamePhase(DiplomacyPhase.SPRING_ORDERS);
        state.setFirstPlayer(0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DiplomacyGameState state = (DiplomacyGameState) gameState;
        int power = state.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();
        if (state.getPhase() == DiplomacyPhase.ADJUSTMENTS) {
            // the units it may disband, or the builds it may make and giving up the rest
            if (state.adjustment(power) < 0) {
                List<DiplomacyOrder> given = state.getOrders(power);
                for (DiplomacyProvince p : state.unitProvinces(power))
                    if (!given.contains(new Disband(p)))
                        actions.add(new Disband(p));
            } else {
                for (DiplomacyProvince p : state.freeHomeCentres(power)) {
                    actions.add(new Build(new DiplomacyLocation(p), DiplomacyUnit.Type.ARMY));
                    if (p.type() == DiplomacyProvince.Type.COAST) {
                        if (p.hasCoasts())
                            for (String coast : p.coasts())
                                actions.add(new Build(new DiplomacyLocation(p, coast), DiplomacyUnit.Type.FLEET));
                        else
                            actions.add(new Build(new DiplomacyLocation(p), DiplomacyUnit.Type.FLEET));
                    }
                }
                actions.add(new WaiveBuilds());
            }
            return actions;
        }
        List<DiplomacyProvince> toOrder = state.unitsToOrder(power);
        if (toOrder.isEmpty())
            return actions;
        // the orders for the power's next unit
        DiplomacyProvince unit = toOrder.get(0);
        if (state.getPhase().isRetreats()) {
            for (DiplomacyLocation to : state.retreats(unit))
                actions.add(new Retreat(unit, to));
            actions.add(new Disband(unit));
            return actions;
        }
        actions.add(new Hold(unit));
        List<DiplomacyLocation> moves = state.ordinaryMoves(unit, state.getUnit(unit));
        for (DiplomacyLocation to : moves)
            actions.add(new Move(unit, to));
        for (DiplomacyProvince to : state.convoyRoutes(unit).keySet())
            actions.add(new Move(unit, new DiplomacyLocation(to), true));
        addSupports(state, unit, moves, actions);
        if (unit.type() == DiplomacyProvince.Type.SEA)
            addConvoys(state, unit, actions);
        return actions;
    }

    /**
     * The convoys the fleet in the (sea) province may make: each army on a coast to each destination it could be
     * convoyed to along a chain of fleets that includes this one (only the power's own armies if ownUnitsOnly).
     */
    private void addConvoys(DiplomacyGameState state, DiplomacyProvince fleet, List<AbstractAction> actions) {
        int power = state.getUnit(fleet).owner();
        for (DiplomacyProvince from : state.getMap().provinces())
            if (state.getUnit(from) != null && mayHelp(state, power, from))
                for (Map.Entry<DiplomacyProvince, Set<DiplomacyProvince>> route : state.convoyRoutes(from).entrySet())
                    if (route.getValue().contains(fleet))
                        actions.add(new Convoy(fleet, from, route.getKey()));
    }

    /**
     * The supports the unit in the province may give: to any other unit in a province it could move to (to hold),
     * or able to move there itself (to move there); only to the power's own units if ownUnitsOnly. Support for a
     * foreign unit's attack on one of the power's own units is legal but cannot dislodge it, so is not offered.
     */
    private void addSupports(DiplomacyGameState state, DiplomacyProvince unit, List<DiplomacyLocation> moves,
                             List<AbstractAction> actions) {
        int power = state.getUnit(unit).owner();
        List<DiplomacyProvince> reach = moves.stream().map(DiplomacyLocation::province).distinct().toList();
        for (DiplomacyProvince target : reach)
            if (state.getUnit(target) != null && mayHelp(state, power, target))
                actions.add(new SupportHold(unit, target));
        for (DiplomacyProvince from : state.getMap().provinces())
            if (!from.equals(unit) && state.getUnit(from) != null && mayHelp(state, power, from))
                for (DiplomacyProvince target : reach)
                    if (!target.equals(from) && state.canReach(from, target)
                            && !attacksOwnUnit(state, power, from, target))
                        actions.add(new SupportMove(unit, from, target));
    }

    /**
     * Whether the power is offered orders that help the unit in the province.
     */
    private boolean mayHelp(DiplomacyGameState state, int power, DiplomacyProvince province) {
        return !((DiplomacyParameters) state.getGameParameters()).ownUnitsOnly
                || state.getUnit(province).owner() == power;
    }

    private boolean attacksOwnUnit(DiplomacyGameState state, int power, DiplomacyProvince from,
                                   DiplomacyProvince target) {
        DiplomacyUnit defender = state.getUnit(target);
        return defender != null && defender.owner() == power && state.getUnit(from).owner() != power;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        DiplomacyGameState state = (DiplomacyGameState) currentState;
        // the current power keeps the turn until it has given all its orders, then it passes to the next power with
        // orders to give; when there is none, the phase is resolved
        int current = state.getCurrentPlayer();
        if (state.hasOrdersToGive(current))
            return;
        for (int i = 1; i < state.getNPlayers(); i++) {
            int p = (current + i) % state.getNPlayers();
            if (state.hasOrdersToGive(p)) {
                endPlayerTurn(state, p);
                return;
            }
        }
        resolvePhase(state);
    }

    /**
     * Carries out the orders of the current phase, and moves on to the next phase.
     */
    private void resolvePhase(DiplomacyGameState state) {
        DiplomacyPhase phase = state.getPhase();
        List<DiplomacyResult> results = new ArrayList<>();
        DiplomacyPhase next;
        switch (phase) {
            case SPRING_ORDERS, FALL_ORDERS -> {
                resolveOrders(state, results);
                next = phase.spring ? DiplomacyPhase.SPRING_RETREATS : DiplomacyPhase.FALL_RETREATS;
            }
            case SPRING_RETREATS -> {
                resolveRetreats(state, results);
                next = DiplomacyPhase.FALL_ORDERS;
            }
            case FALL_RETREATS -> {
                resolveRetreats(state, results);
                next = DiplomacyPhase.ADJUSTMENTS;
            }
            default -> {
                resolveAdjustments(state, results);
                next = DiplomacyPhase.SPRING_ORDERS;
            }
        }
        // a phase in which nobody gave an order leaves the last results (of the phase before it) to be seen
        if (!results.isEmpty())
            state.lastResults = results;
        for (List<DiplomacyOrder> list : state.orders)
            list.clear();
        enterPhase(state, next);
    }

    private void enterPhase(DiplomacyGameState state, DiplomacyPhase phase) {
        // the end of the Fall turn: supply centres change hands, and the game may end
        if (phase == DiplomacyPhase.ADJUSTMENTS) {
            updateOwnership(state);
            if (gameOver(state)) {
                endGame(state);
                return;
            }
        }
        if (phase == DiplomacyPhase.SPRING_ORDERS)
            state.year++;
        state.setGamePhase(phase);
        if (phase.isRetreats()) {
            // a dislodged unit with nowhere to retreat is disbanded at once
            for (DiplomacyProvince p : state.getMap().provinces())
                if (state.dislodged[p.index()] != null && state.retreats(p).isEmpty())
                    state.dislodged[p.index()] = null;
        }
        // the lowest-numbered power with orders to give starts; a phase in which nobody has any is resolved at once
        for (int p = 0; p < state.getNPlayers(); p++)
            if (state.hasOrdersToGive(p)) {
                endRound(state, p);
                return;
            }
        resolvePhase(state);
    }

    /**
     * Carries out the orders of an orders phase, setting aside each unit dislodged to retreat.
     */
    private void resolveOrders(DiplomacyGameState state, List<DiplomacyResult> results) {
        DiplomacyAdjudicator adjudicator = new DiplomacyAdjudicator(state);
        for (int p = 0; p < state.getNPlayers(); p++)
            for (DiplomacyOrder o : state.getOrders(p))
                results.add(new DiplomacyResult(p, o, adjudicator.succeeds(o.province())));
        Arrays.fill(state.dislodged, null);
        Arrays.fill(state.dislodgedFrom, -1);
        Arrays.fill(state.standoff, false);
        for (DiplomacyProvince p : state.getMap().provinces()) {
            int attacker = adjudicator.dislodgedBy(p);
            if (attacker >= 0) {
                state.dislodged[p.index()] = state.units[p.index()];
                // a unit dislodged by a convoyed army may retreat to where the army came from
                if (!adjudicator.isConvoyed(state.getMap().provinces().get(attacker)))
                    state.dislodgedFrom[p.index()] = attacker;
            }
        }
        DiplomacyUnit[] after = state.units.clone();
        for (DiplomacyResult r : results)
            if (r.success() && r.order() instanceof Move m)
                after[m.unit.index()] = null;
        for (DiplomacyResult r : results)
            if (r.success() && r.order() instanceof Move m) {
                DiplomacyUnit u = state.units[m.unit.index()];
                after[m.to.province().index()] = new DiplomacyUnit(u.type(), u.owner(), m.to.coast());
            }
        // a province left empty after a failed move into it was the scene of a standoff - unless that move lost a
        // head-to-head battle against the unit that left it, or was a convoy that never arrived
        for (DiplomacyResult r : results)
            if (!r.success() && r.order() instanceof Move m && adjudicator.reachesDestination(m.unit)) {
                int dest = m.to.province().index();
                boolean lostHeadToHead = state.dislodgedFrom[m.unit.index()] == dest;
                if (after[dest] == null && !lostHeadToHead)
                    state.standoff[dest] = true;
            }
        state.units = after;
    }

    private void resolveRetreats(DiplomacyGameState state, List<DiplomacyResult> results) {
        // units retreating to the same province are all disbanded
        int[] arrivals = new int[state.getMap().nProvinces()];
        for (int p = 0; p < state.getNPlayers(); p++)
            for (DiplomacyOrder o : state.getOrders(p))
                if (o instanceof Retreat r)
                    arrivals[r.to.province().index()]++;
        for (int p = 0; p < state.getNPlayers(); p++)
            for (DiplomacyOrder o : state.getOrders(p)) {
                boolean success = true;
                if (o instanceof Retreat r) {
                    success = arrivals[r.to.province().index()] == 1;
                    if (success) {
                        DiplomacyUnit u = state.dislodged[r.unit.index()];
                        state.units[r.to.province().index()] = new DiplomacyUnit(u.type(), u.owner(), r.to.coast());
                    }
                }
                results.add(new DiplomacyResult(p, o, success));
            }
        // dislodgedFrom and standoff describe the last orders phase, and are cleared when the next is resolved
        Arrays.fill(state.dislodged, null);
    }

    private void resolveAdjustments(DiplomacyGameState state, List<DiplomacyResult> results) {
        for (int p = 0; p < state.getNPlayers(); p++)
            for (DiplomacyOrder o : state.getOrders(p)) {
                if (o instanceof Disband d)
                    state.units[d.unit.index()] = null;
                else if (o instanceof Build b)
                    state.units[b.location.province().index()] = new DiplomacyUnit(b.type, p, b.location.coast());
                results.add(new DiplomacyResult(p, o, true));
            }
    }

    /**
     * At the end of a Fall turn, each supply centre with a unit in it comes under the control of the unit's power.
     */
    private void updateOwnership(DiplomacyGameState state) {
        for (DiplomacyProvince p : state.getMap().supplyCentres()) {
            DiplomacyUnit u = state.getUnit(p);
            if (u != null)
                state.owner[p.index()] = u.owner();
        }
    }

    /**
     * A power controls enough supply centres to win, or the last year (DiplomacyParameters.lastYear) has been played.
     */
    private boolean gameOver(DiplomacyGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++)
            if (state.nCentres(p) >= state.getMap().victoryCentres())
                return true;
        return state.getYear() >= ((DiplomacyParameters) state.getGameParameters()).lastYear;
    }
}

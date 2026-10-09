package games.diplomacy;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.diplomacy.actions.*;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * Factories, arrange helpers and order parsing for the Diplomacy tests.
 * <p>
 * Orders are written as in the rulebook, with the map's abbreviations:
 * "A Ber-Sil", "F Con-Bul/ec", "F StP/sc-Bot" (moves), "A Pru H" or "A Pru Holds" (hold), "Build A Par",
 * "Build F StP/nc", "Disband Ber", "Waive France" (WaiveBuilds by that power); supports "A Gas S A Mar-Bur"
 * (SupportMove, a coast in the destination is dropped) and "F Den S F Bal" (SupportHold); retreats "A Mun R Boh",
 * "F Gas R Spa/nc" (the unit dislodged from the first province); convoys "F Nth C A Lon-Nwy"; a move marked via
 * convoy "A Lon-Bel via convoy" (a plain "A Lon-Bel" is a move not via convoy). In a retreat phase "Disband Ber" is
 * the unit dislodged from Ber.
 */
class DiplomacyTestUtils {

    static final int AUSTRIA = 0, ENGLAND = 1, FRANCE = 2, GERMANY = 3, ITALY = 4, RUSSIA = 5, TURKEY = 6;
    static final int N_POWERS = 7;
    static final String[] POWER_NAMES = {"Austria", "England", "France", "Germany", "Italy", "Russia", "Turkey"};

    private DiplomacyTestUtils() {
    }

    /** A game with 7 random players, and default parameters when params is null. */
    static Game newGame(long seed, DiplomacyParameters params) {
        Game game = GameType.Diplomacy.createGameInstance(N_POWERS, seed,
                params == null ? new DiplomacyParameters() : params);
        List<AbstractPlayer> players = IntStream.range(0, N_POWERS)
                .mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList();
        game.reset(players);
        return game;
    }

    static Game newGame(long seed) {
        return newGame(seed, null);
    }

    /** A directly instantiated state, set up (Spring 1901, starting units), with default parameters if null. */
    static DiplomacyGameState newState(DiplomacyParameters params) {
        DiplomacyGameState state = new DiplomacyGameState(params == null ? new DiplomacyParameters() : params, N_POWERS);
        new DiplomacyForwardModel().setup(state);
        return state;
    }

    static DiplomacyGameState newState() {
        return newState(null);
    }

    /** Default parameters, except that orders helping other powers' units are offered (ownUnitsOnly false). */
    static DiplomacyParameters helpingAnyUnit() {
        DiplomacyParameters params = new DiplomacyParameters();
        params.setParameterValue("ownUnitsOnly", false);
        return params;
    }

    static DiplomacyProvince prov(DiplomacyGameState state, String name) {
        return state.getMap().province(name);
    }

    static DiplomacyLocation loc(DiplomacyGameState state, String name) {
        return state.getMap().location(name);
    }

    static DiplomacyUnit army(int power) {
        return new DiplomacyUnit(DiplomacyUnit.Type.ARMY, power);
    }

    static DiplomacyUnit fleet(int power) {
        return new DiplomacyUnit(DiplomacyUnit.Type.FLEET, power);
    }

    static DiplomacyUnit fleet(int power, String coast) {
        return new DiplomacyUnit(DiplomacyUnit.Type.FLEET, power, coast);
    }

    /** Removes every unit from the board. Supply centre ownership is left as it was. */
    static void clearBoard(DiplomacyGameState state) {
        for (DiplomacyProvince p : state.getMap().provinces())
            state.setUnit(p, null);
    }

    /** Puts units of the power on the board, e.g. place(state, GERMANY, "A Ber", "F Kie", "F StP/nc"). */
    static void place(DiplomacyGameState state, int power, String... units) {
        for (String u : units) {
            String[] parts = u.split(" ");
            DiplomacyLocation l = loc(state, parts[1]);
            DiplomacyUnit.Type type = switch (parts[0]) {
                case "A" -> DiplomacyUnit.Type.ARMY;
                case "F" -> DiplomacyUnit.Type.FLEET;
                default -> throw new IllegalArgumentException("Bad unit " + u);
            };
            state.setUnit(l.province(), new DiplomacyUnit(type, power, l.coast()));
        }
    }

    /** Gives control of the supply centres to the power (-1 for nobody). */
    static void own(DiplomacyGameState state, int power, String... centres) {
        for (String c : centres)
            state.setOwner(prov(state, c), power);
    }

    /**
     * Records a unit of the power as dislodged, as an orders phase would. dislodge(state, GERMANY, "A Kie", "Hol")
     * records the army in Kie as dislodged by a unit that came from Hol; attackerOrigin is null for no origin.
     */
    static void dislodge(DiplomacyGameState state, int power, String unit, String attackerOrigin) {
        String[] parts = unit.split(" ");
        DiplomacyLocation l = loc(state, parts[1]);
        DiplomacyUnit.Type type = parts[0].equals("F") ? DiplomacyUnit.Type.FLEET : DiplomacyUnit.Type.ARMY;
        state.setDislodged(l.province(), new DiplomacyUnit(type, power, l.coast()),
                attackerOrigin == null ? null : prov(state, attackerOrigin));
    }

    /** The legal actions now, as a set (after checking there are no duplicates). */
    static Set<AbstractAction> legalSet(DiplomacyGameState state, DiplomacyForwardModel fm) {
        List<AbstractAction> list = fm.computeAvailableActions(state);
        Set<AbstractAction> set = new HashSet<>(list);
        assertEquals("duplicate actions in " + list, list.size(), set.size());
        return set;
    }

    /** The legal actions now of the given class (e.g. Convoy.class), as a set. */
    static Set<AbstractAction> legalSetOf(DiplomacyGameState state, DiplomacyForwardModel fm,
                                          Class<? extends DiplomacyOrder> type) {
        Set<AbstractAction> set = new HashSet<>();
        for (AbstractAction a : legalSet(state, fm))
            if (type.isInstance(a)) set.add(a);
        return set;
    }

    /** The orders written, as a set of actions (to compare with legalSet). */
    static Set<AbstractAction> orderSet(DiplomacyGameState state, String... orders) {
        Set<AbstractAction> set = new HashSet<>();
        for (String o : orders)
            set.add(order(state, o));
        assertEquals("duplicate orders written", orders.length, set.size());
        return set;
    }

    /** Starts the orders phase (SPRING_ORDERS or FALL_ORDERS) with the lowest-numbered power that has a unit. */
    static void startOrders(DiplomacyGameState state, DiplomacyPhase phase) {
        for (int p = 0; p < N_POWERS; p++)
            if (state.nUnits(p) > 0) {
                startPhase(state, phase, p);
                return;
            }
        fail("arrangement: no units on the board");
    }

    /**
     * The orders given, followed by a Hold for every unit on the board whose province is not the first province
     * written in one of them (for a whole orders phase in which most units hold).
     */
    static String[] andHoldTheRest(DiplomacyGameState state, String... orders) {
        Set<DiplomacyProvince> ordered = new HashSet<>();
        for (String o : orders)
            ordered.add(order(state, o).province());
        List<String> all = new ArrayList<>(Arrays.asList(orders));
        for (DiplomacyProvince p : state.getMap().provinces()) {
            DiplomacyUnit u = state.getUnit(p);
            if (u != null && !ordered.contains(p))
                all.add(u.type().letter + " " + p.name() + " H");
        }
        return all.toArray(new String[0]);
    }

    /** The number of dislodged units recorded (awaiting retreat). */
    static int totalDislodged(DiplomacyGameState state) {
        int n = 0;
        for (DiplomacyProvince p : state.getMap().provinces())
            if (state.getDislodged(p) != null) n++;
        return n;
    }

    /** Starts the given phase with the power to act first. */
    static void startPhase(DiplomacyGameState state, DiplomacyPhase phase, int firstPower) {
        state.setGamePhase(phase);
        state.setTurnOwner(firstPower);
    }

    /**
     * An order written with this suffix is legal but not offered by computeAvailableActions (support for a foreign
     * unit's attack on the supporter's own unit); play checks it is not offered, and gives it anyway.
     */
    static final String NOT_OFFERED = "(not offered)";

    /** The order written in the text (see the class Javadoc). Needs only the map, not the units. */
    static DiplomacyOrder order(DiplomacyGameState state, String text) {
        String[] parts = text.replace(NOT_OFFERED, "").trim().split(" +");
        switch (parts[0]) {
            case "Waive":
                return new WaiveBuilds();
            case "Build":
                return new Build(loc(state, parts[2]), parts[1].equals("F") ? DiplomacyUnit.Type.FLEET : DiplomacyUnit.Type.ARMY);
            case "Disband":
                return new Disband(prov(state, parts[1]));
            case "A":
            case "F":
                if (parts.length == 3 && (parts[2].equals("H") || parts[2].equals("Holds")))
                    return new Hold(loc(state, parts[1]).province());
                if (parts.length == 5 && parts[2].equals("S")) {
                    DiplomacyProvince unit = loc(state, parts[1]).province();
                    if (parts[4].contains("-")) {
                        String[] ends = parts[4].split("-");
                        return new SupportMove(unit, loc(state, ends[0]).province(), loc(state, ends[1]).province());
                    }
                    return new SupportHold(unit, loc(state, parts[4]).province());
                }
                if (parts.length == 5 && parts[2].equals("C")) {
                    String[] ends = parts[4].split("-");
                    return new Convoy(loc(state, parts[1]).province(), loc(state, ends[0]).province(),
                            loc(state, ends[1]).province());
                }
                if (parts.length == 4 && parts[2].equals("via") && parts[3].equals("convoy")) {
                    String[] ends = parts[1].split("-");
                    return new Move(loc(state, ends[0]).province(), loc(state, ends[1]), true);
                }
                if (parts.length == 4 && parts[2].equals("R"))
                    return new Retreat(loc(state, parts[1]).province(), loc(state, parts[3]));
                if (parts.length == 2 && parts[1].contains("-")) {
                    String[] ends = parts[1].split("-");
                    return new Move(loc(state, ends[0]).province(), loc(state, ends[1]));
                }
            default:
                throw new IllegalArgumentException("Cannot parse order " + text);
        }
    }

    /** The power giving the order written. */
    static int powerOf(DiplomacyGameState state, String text) {
        String[] parts = text.replace(NOT_OFFERED, "").trim().split(" +");
        switch (parts[0]) {
            case "Waive": {
                int p = Arrays.asList(POWER_NAMES).indexOf(parts[1]);
                if (p < 0) throw new IllegalArgumentException("Unknown power in " + text);
                return p;
            }
            case "Build":
                return loc(state, parts[2]).province().home();
            case "Disband": {
                DiplomacyProvince p = prov(state, parts[1]);
                DiplomacyUnit u = state.getPhase().isRetreats() ? state.getDislodged(p) : state.getUnit(p);
                assertNotNull("arrangement: no unit to disband in " + text, u);
                return u.owner();
            }
            default: {
                String where = parts[1].split("-")[0];
                DiplomacyProvince p = loc(state, where).province();
                DiplomacyUnit u = parts.length == 4 && parts[2].equals("R") ? state.getDislodged(p) : state.getUnit(p);
                assertNotNull("arrangement: no unit for " + text, u);
                assertEquals("arrangement: wrong unit type for " + text, parts[0], u.type().letter);
                return u.owner();
            }
        }
    }

    /**
     * Gives the orders through fm.next, each when its power has the turn, checking that each is a legal action (or
     * for one marked NOT_OFFERED, that it is not).
     * Fails if the power with the turn has no order left in the list, or if the game ends or the phase changes
     * before every order is given. The orders may be a whole phase (the last one then resolves it) or only part.
     */
    static void play(DiplomacyGameState state, DiplomacyForwardModel fm, String... orders) {
        record Owned(int power, DiplomacyOrder order, String text) {
        }
        List<Owned> remaining = new ArrayList<>();
        for (String t : orders)
            remaining.add(new Owned(powerOf(state, t), order(state, t), t));
        DiplomacyPhase phase = state.getPhase();
        int year = state.getYear();
        while (!remaining.isEmpty()) {
            List<String> left = remaining.stream().map(Owned::text).toList();
            assertTrue("game ended with orders still to give: " + left, state.isNotTerminal());
            assertEquals("phase changed with orders still to give: " + left, phase, state.getPhase());
            assertEquals("year changed with orders still to give: " + left, year, state.getYear());
            int current = state.getCurrentPlayer();
            // In an orders phase units are ordered in province order, so the power gives its remaining order for
            // the lowest-index province. In adjustments it gives its first remaining order in the list.
            Owned next = null;
            for (Owned o : remaining) {
                if (o.power() != current) continue;
                if (next == null || (phase != DiplomacyPhase.ADJUSTMENTS
                        && o.order().province().index() < next.order().province().index()))
                    next = o;
            }
            if (next == null)
                fail(POWER_NAMES[current] + " has the turn in " + phase + " " + year
                        + " but the test gives it no more orders; left: " + left);
            List<AbstractAction> legal = fm.computeAvailableActions(state);
            if (next.text().contains(NOT_OFFERED))
                assertFalse(POWER_NAMES[current] + " is offered " + next.text() + "; legal: " + legal,
                        legal.contains(next.order()));
            else
                assertTrue(POWER_NAMES[current] + " may not give " + next.text() + "; legal: " + legal,
                        legal.contains(next.order()));
            fm.next(state, next.order());
            remaining.remove(next);
        }
    }

    /** Gives the rest of the orders phase through play: every unit not yet ordered this phase holds. */
    static void holdAll(DiplomacyGameState state, DiplomacyForwardModel fm) {
        List<String> holds = new ArrayList<>();
        for (DiplomacyProvince p : state.getMap().provinces()) {
            DiplomacyUnit u = state.getUnit(p);
            if (u != null && state.getOrders(u.owner()).stream().noneMatch(o -> p.equals(o.province())))
                holds.add(u.type().letter + " " + p.name() + " H");
        }
        play(state, fm, holds.toArray(new String[0]));
    }

    /**
     * Plays a whole phase through play, each order written with its expected outcome after a colon -
     * "A Ber-Pru: ok", "A Pru-Ber: fails" - and asserts that getLastResults is exactly those outcomes.
     */
    static void playExpecting(DiplomacyGameState state, DiplomacyForwardModel fm, String... ordersWithOutcome) {
        String[] orders = new String[ordersWithOutcome.length];
        Set<DiplomacyResult> expected = new HashSet<>();
        for (int i = 0; i < orders.length; i++) {
            String[] parts = ordersWithOutcome[i].split(":");
            String outcome = parts[1].trim();
            if (!outcome.equals("ok") && !outcome.equals("fails"))
                throw new IllegalArgumentException("outcome must be ok or fails: " + ordersWithOutcome[i]);
            orders[i] = parts[0].trim();
            expected.add(new DiplomacyResult(powerOf(state, orders[i]), order(state, orders[i]), outcome.equals("ok")));
        }
        play(state, fm, orders);
        assertEquals(expected, lastResults(state));
    }

    /** The result of an order as it should appear in getLastResults. */
    static DiplomacyResult result(DiplomacyGameState state, int power, String order, boolean success) {
        return new DiplomacyResult(power, order(state, order), success);
    }

    /** The last resolved phase's results as a set, after checking the list has no duplicates. */
    static Set<DiplomacyResult> lastResults(DiplomacyGameState state) {
        Set<DiplomacyResult> set = new HashSet<>(state.getLastResults());
        assertEquals("duplicate results in " + state.getLastResults(), state.getLastResults().size(), set.size());
        return set;
    }

    static int totalUnits(DiplomacyGameState state) {
        int n = 0;
        for (DiplomacyProvince p : state.getMap().provinces())
            if (state.getUnit(p) != null) n++;
        return n;
    }

    /** Asserts that no power has an order recorded. */
    static void assertNoOrders(DiplomacyGameState state) {
        for (int p = 0; p < N_POWERS; p++)
            assertEquals("orders of " + POWER_NAMES[p], List.of(), state.getOrders(p));
    }
}

package games.diplomacy;

import games.diplomacy.actions.Convoy;
import games.diplomacy.actions.DiplomacyOrder;
import games.diplomacy.actions.Move;
import games.diplomacy.actions.SupportHold;
import games.diplomacy.actions.SupportMove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Decides which orders of an orders phase succeed and which units are dislodged, by Kruijswijk's algorithm ("The Math
 * of Adjudication", DATC section 5). One is made for each resolution.
 */
class DiplomacyAdjudicator {

    private enum Status {UNRESOLVED, GUESSING, RESOLVED}

    private final DiplomacyGameState state;
    private final int nProvinces;
    // by the province of the unit ordered
    private final DiplomacyOrder[] order;
    private final int[] power;
    private final Status[] status;
    private final boolean[] resolution;
    private final List<Integer> dependencies = new ArrayList<>();
    // convoying fleets that hold instead, by the Szykman rule for convoy paradoxes
    private final boolean[] holding;
    private final DiplomacyParadoxRule paradoxRule;

    DiplomacyAdjudicator(DiplomacyGameState state) {
        this.state = state;
        nProvinces = state.getMap().nProvinces();
        order = new DiplomacyOrder[nProvinces];
        power = new int[nProvinces];
        status = new Status[nProvinces];
        resolution = new boolean[nProvinces];
        holding = new boolean[nProvinces];
        paradoxRule = ((DiplomacyParameters) state.getGameParameters()).paradoxRule;
        for (int p = 0; p < state.getNPlayers(); p++)
            for (DiplomacyOrder o : state.getOrders(p)) {
                order[o.province().index()] = o;
                power[o.province().index()] = p;
            }
        Arrays.fill(status, Status.UNRESOLVED);
    }

    /**
     * Whether the order given to the unit in the province succeeds.
     */
    boolean succeeds(DiplomacyProvince province) {
        return resolve(province.index());
    }

    private boolean resolve(int nr) {
        // an order's success is decided from the strengths of the orders that bear on it, recursively; where the
        // decisions depend on each other in a cycle, both guesses are tried, and the backup rule decides if both (or
        // neither) are consistent
        if (status[nr] == Status.RESOLVED)
            return resolution[nr];
        if (status[nr] == Status.GUESSING) {
            if (!dependencies.contains(nr))
                dependencies.add(nr);
            return resolution[nr];
        }
        int oldSize = dependencies.size();
        resolution[nr] = false;
        status[nr] = Status.GUESSING;
        boolean first = adjudicate(nr);
        if (dependencies.size() == oldSize) {
            // no guess was needed (or it was resolved in the meantime)
            if (status[nr] != Status.RESOLVED) {
                resolution[nr] = first;
                status[nr] = Status.RESOLVED;
            }
            return first;
        }
        if (dependencies.get(oldSize) != nr) {
            // part of a cycle headed by another order: leave the guess for it to settle
            dependencies.add(nr);
            resolution[nr] = first;
            return first;
        }
        // nr heads a cycle: try the other guess
        clearDependencies(oldSize);
        resolution[nr] = true;
        status[nr] = Status.GUESSING;
        boolean second = adjudicate(nr);
        if (first == second) {
            clearDependencies(oldSize);
            resolution[nr] = first;
            status[nr] = Status.RESOLVED;
            return first;
        }
        backupRule(oldSize);
        return resolve(nr);
    }

    private void clearDependencies(int oldSize) {
        while (dependencies.size() > oldSize)
            status[dependencies.remove(dependencies.size() - 1)] = Status.UNRESOLVED;
    }

    /**
     * Settles the cycle of orders from oldSize in the dependency list, for which both guesses (or neither) are
     * consistent.
     */
    private void backupRule(int oldSize) {
        // a cycle with a convoy order in it is a convoy paradox: by the Szykman rule its convoying fleets hold (carry
        // no army), and the cycle is resolved again; otherwise it is circular movement, and every move succeeds
        List<Integer> cycle = new ArrayList<>(dependencies.subList(oldSize, dependencies.size()));
        boolean paradox = cycle.stream().anyMatch(nr -> order[nr] instanceof Convoy);
        clearDependencies(oldSize);
        for (int nr : cycle) {
            if (paradox) {
                if (order[nr] instanceof Convoy)
                    holding[nr] = true;
            } else {
                resolution[nr] = true;
                status[nr] = Status.RESOLVED;
            }
        }
    }

    private boolean adjudicate(int nr) {
        if (order[nr] instanceof Move move)
            return adjudicateMove(nr, move);
        if (order[nr] instanceof SupportHold || order[nr] instanceof SupportMove)
            return supportGiven(nr);
        // a unit not moving succeeds unless it is dislodged
        return !dislodged(nr);
    }

    /**
     * Whether the support matches the order of the unit it supports, and is not cut.
     */
    private boolean supportGiven(int nr) {
        // a hold support needs a unit not moving; a move support needs a unit making that move
        int target;
        if (order[nr] instanceof SupportHold s) {
            target = s.supported.index();
            if (order[target] == null || order[target] instanceof Move)
                return false;
        } else {
            SupportMove s = (SupportMove) order[nr];
            target = s.to.index();
            if (!(order[s.from.index()] instanceof Move m) || !m.to.province().equals(s.to))
                return false;
        }
        // cut by an attack from another power, from any province but the one the support is given into
        for (int other = 0; other < nProvinces; other++)
            if (order[other] instanceof Move m && m.to.province().index() == nr && power[other] != power[nr]
                    && other != target && cutsSupport(other, m, nr))
                return false;
        // dislodgement cuts it too - except by a convoyed army that the support's attack on one of its fleets
        // would stop (the 2000 rule 21 takes precedence over dislodgement)
        for (int other = 0; other < nProvinces; other++)
            if (order[other] instanceof Move m && m.to.province().index() == nr && !rule21(other, m, nr)
                    && resolve(other))
                return false;
        return true;
    }

    /**
     * Whether the move from attacker into the province of the supporting unit nr would cut its support.
     */
    private boolean cutsSupport(int attacker, Move move, int nr) {
        // a convoyed army must have a successful route, and rule 21 applies to it
        if (!convoyed(attacker))
            return true;
        return !rule21(attacker, move, nr) && path(attacker, move, -1);
    }

    /**
     * Rules 21 and 22 of the 2000 rulebook (only with DiplomacyParameters.paradoxRule RULEBOOK_2000): a convoyed
     * army does not cut the support of a unit (nr) supporting an attack on one of the fleets convoying it, unless it
     * has a successful route without that fleet.
     */
    private boolean rule21(int attacker, Move move, int nr) {
        return paradoxRule == DiplomacyParadoxRule.RULEBOOK_2000 && convoyed(attacker)
                && order[nr] instanceof SupportMove s && order[s.to.index()] instanceof Convoy c
                && c.from.index() == attacker && c.to.equals(move.to.province())
                && !path(attacker, move, s.to.index());
    }

    /**
     * Whether the move from nr is carried by convoy: an army's move marked via convoy, or one that a fleet of the
     * army's own power is ordered to convoy.
     */
    private boolean convoyed(int nr) {
        if (!(order[nr] instanceof Move m) || m.viaConvoy)
            return order[nr] instanceof Move;
        for (int f = 0; f < nProvinces; f++)
            if (order[f] instanceof Convoy c && power[f] == power[nr] && c.from.index() == nr
                    && c.to.equals(m.to.province()))
                return true;
        return false;
    }

    /**
     * Whether the move from nr can reach its destination without the fleet in the province excluded (-1 for none).
     */
    private boolean path(int nr, Move move, int excluded) {
        if (!convoyed(nr))
            return true;
        // a chain of fleets ordered to convoy exactly this move, none of them dislodged, from the army to the
        // destination
        DiplomacyMap map = state.getMap();
        DiplomacyProvince dest = move.to.province();
        List<DiplomacyProvince> frontier = new ArrayList<>(List.of(map.provinces().get(nr)));
        boolean[] seen = new boolean[nProvinces];
        while (!frontier.isEmpty()) {
            DiplomacyProvince p = frontier.remove(frontier.size() - 1);
            for (DiplomacyProvince sea : map.adjacent(p)) {
                int s = sea.index();
                if (seen[s] || s == excluded || holding[s] || !(order[s] instanceof Convoy c) || c.from.index() != nr
                        || !c.to.equals(dest) || !resolve(s))
                    continue;
                if (map.adjacent(sea).contains(dest))
                    return true;
                seen[s] = true;
                frontier.add(sea);
            }
        }
        return false;
    }

    /**
     * Whether the move from the province (if it is one) is carried by convoy.
     */
    boolean isConvoyed(DiplomacyProvince province) {
        return convoyed(province.index());
    }

    /**
     * Whether the move from the province reaches its destination province (to arrive or to be bounced there): false
     * for a convoyed army with no successful route.
     */
    boolean reachesDestination(DiplomacyProvince province) {
        return order[province.index()] instanceof Move m && path(province.index(), m, -1);
    }

    /**
     * The supports given to the move from nr (or, if it is not moving, to the unit in nr holding), leaving out those
     * of the power excluded (-1 for none).
     */
    private int supports(int nr, int excluded) {
        int n = 0;
        for (int other = 0; other < nProvinces; other++) {
            boolean supporting = order[nr] instanceof Move m
                    ? order[other] instanceof SupportMove sm && sm.from.index() == nr && sm.to.equals(m.to.province())
                    : order[other] instanceof SupportHold sh && sh.supported.index() == nr;
            if (supporting && power[other] != excluded && resolve(other))
                n++;
        }
        return n;
    }

    private boolean adjudicateMove(int nr, Move move) {
        int dest = move.to.province().index();
        if (!path(nr, move, -1))
            return false;
        int attack = attackStrength(nr, move);
        int opponent = headToHead(nr, move);
        if (opponent >= 0) {
            if (attack <= defendStrength(opponent))
                return false;
        } else if (attack <= holdStrength(dest)) {
            return false;
        }
        for (int other = 0; other < nProvinces; other++)
            if (other != nr && order[other] instanceof Move m && m.to.province().index() == dest
                    && attack <= preventStrength(other, m))
                return false;
        return true;
    }

    /**
     * Whether the unit in the province (not moving away successfully) is dislodged by a move into it.
     */
    private boolean dislodged(int nr) {
        for (int other = 0; other < nProvinces; other++)
            if (order[other] instanceof Move m && m.to.province().index() == nr && resolve(other))
                return true;
        return false;
    }

    /**
     * The province of a unit moving to nr's province while nr moves to its, neither by convoy, or -1.
     */
    private int headToHead(int nr, Move move) {
        int dest = move.to.province().index();
        if (order[dest] instanceof Move back && back.to.province().index() == nr && !convoyed(nr) && !convoyed(dest))
            return dest;
        return -1;
    }

    private int holdStrength(int province) {
        if (order[province] == null)
            return 0;
        if (order[province] instanceof Move)
            return resolve(province) ? 0 : 1;
        return 1 + supports(province, -1);
    }

    private int attackStrength(int nr, Move move) {
        if (!path(nr, move, -1))
            return 0;
        int dest = move.to.province().index();
        boolean destVacated = order[dest] == null
                || (order[dest] instanceof Move && headToHead(nr, move) < 0 && resolve(dest));
        if (destVacated)
            return 1 + supports(nr, -1);
        // a power cannot dislodge its own unit, nor help another power dislodge it
        if (power[dest] == power[nr])
            return 0;
        return 1 + supports(nr, power[dest]);
    }

    private int defendStrength(int nr) {
        return 1 + supports(nr, -1);
    }

    private int preventStrength(int nr, Move move) {
        if (!path(nr, move, -1))
            return 0;
        int opponent = headToHead(nr, move);
        // a unit that lost a head-to-head battle has no effect on the province its attacker came from
        if (opponent >= 0 && resolve(opponent))
            return 0;
        return 1 + supports(nr, -1);
    }

    /**
     * The province the unit that dislodged the unit in nr came from, or -1 if it is not dislodged.
     */
    int dislodgedBy(DiplomacyProvince province) {
        int nr = province.index();
        if (order[nr] == null || (order[nr] instanceof Move && resolve(nr)))
            return -1;
        for (int other = 0; other < nProvinces; other++)
            if (order[other] instanceof Move m && m.to.province().index() == nr && resolve(other))
                return other;
        return -1;
    }
}

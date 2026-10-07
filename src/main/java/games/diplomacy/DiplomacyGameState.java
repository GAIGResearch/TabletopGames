package games.diplomacy;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import games.GameType;
import games.diplomacy.actions.Build;
import games.diplomacy.actions.DiplomacyOrder;
import games.diplomacy.actions.WaiveBuilds;

import java.util.*;

/**
 * <p>State tracked (the board itself is fixed, in DiplomacyParameters.getMap()); the arrays are indexed by
 * DiplomacyProvince.index():</p>
 * <ul>
 *     <li>units - the unit in each province, or null</li>
 *     <li>owner - the power controlling each supply centre, or -1 (always -1 for a province that is not one)</li>
 *     <li>year - the game year (the map's start year first)</li>
 *     <li>orders - the orders each power has given in the current phase, in the order given</li>
 *     <li>dislodged - each unit dislodged in the last orders phase, by the province it was dislodged from, until
 *     it retreats or is disbanded</li>
 *     <li>dislodgedFrom - for each unit dislodged in the last orders phase, the province its attacker came from; -1
 *     if none, or if the attacker was convoyed</li>
 *     <li>standoff - the provinces left vacant by a standoff in the last orders phase</li>
 *     <li>lastResults - every power's orders in the last resolved phase, with their outcome; empty before the
 *     first</li>
 * </ul>
 * The phase of the year is the game phase, a DiplomacyPhase. Each player is one power, in the map's order.
 */
public class DiplomacyGameState extends AbstractGameState {

    DiplomacyUnit[] units;
    int[] owner;
    int year;
    List<List<DiplomacyOrder>> orders;
    DiplomacyUnit[] dislodged;
    int[] dislodgedFrom;
    boolean[] standoff;
    List<DiplomacyResult> lastResults;

    public DiplomacyGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Diplomacy;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>();
    }

    public DiplomacyMap getMap() {
        return ((DiplomacyParameters) gameParameters).getMap();
    }

    public DiplomacyUnit getUnit(DiplomacyProvince province) {
        return units[province.index()];
    }

    public int getOwner(DiplomacyProvince province) {
        return owner[province.index()];
    }

    public int getYear() {
        return year;
    }

    public DiplomacyPhase getPhase() {
        return (DiplomacyPhase) getGamePhase();
    }

    /**
     * The provinces holding a unit of the power, in index order.
     */
    public List<DiplomacyProvince> unitProvinces(int power) {
        List<DiplomacyProvince> list = new ArrayList<>();
        for (DiplomacyProvince p : getMap().provinces())
            if (units[p.index()] != null && units[p.index()].owner() == power)
                list.add(p);
        return list;
    }

    public int nUnits(int power) {
        return unitProvinces(power).size();
    }

    public int nCentres(int power) {
        int n = 0;
        for (int o : owner)
            if (o == power)
                n++;
        return n;
    }

    public List<DiplomacyOrder> getOrders(int power) {
        return Collections.unmodifiableList(orders.get(power));
    }

    public void addOrder(int power, DiplomacyOrder order) {
        orders.get(power).add(order);
    }

    public DiplomacyUnit getDislodged(DiplomacyProvince province) {
        return dislodged[province.index()];
    }

    public DiplomacyProvince getDislodgedFrom(DiplomacyProvince province) {
        int from = dislodgedFrom[province.index()];
        return from < 0 ? null : getMap().provinces().get(from);
    }

    public boolean isStandoff(DiplomacyProvince province) {
        return standoff[province.index()];
    }

    /**
     * The locations the unit could reach from the province by an ordinary move (no convoy).
     */
    public List<DiplomacyLocation> ordinaryMoves(DiplomacyProvince province, DiplomacyUnit unit) {
        if (unit.isFleet())
            return getMap().fleetMoves(new DiplomacyLocation(province, unit.coast()));
        return getMap().armyMoves(province).stream().map(DiplomacyLocation::new).toList();
    }

    /**
     * Whether the unit in the province could move to the target province: by an ordinary move (on any coast), or
     * for an army by convoy (see convoyRoutes).
     */
    public boolean canReach(DiplomacyProvince province, DiplomacyProvince target) {
        DiplomacyUnit unit = units[province.index()];
        if (unit == null)
            return false;
        return ordinaryMoves(province, unit).stream().anyMatch(l -> l.province().equals(target))
                || convoyRoutes(province).containsKey(target);
    }

    /**
     * The coastal provinces the army in the province could be convoyed to by the fleets now at sea (only its own
     * power's fleets if DiplomacyParameters.ownUnitsOnly), each with the sea provinces of the fleets that could take
     * part. Empty for a fleet, or an army not on a coast.
     */
    public Map<DiplomacyProvince, Set<DiplomacyProvince>> convoyRoutes(DiplomacyProvince province) {
        Map<DiplomacyProvince, Set<DiplomacyProvince>> routes = new HashMap<>();
        DiplomacyUnit unit = units[province.index()];
        if (unit == null || unit.isFleet() || province.type() != DiplomacyProvince.Type.COAST)
            return routes;
        // every chain of fleets in adjacent sea provinces that starts next to the army and uses no fleet twice
        int fleetOwner = ((DiplomacyParameters) gameParameters).ownUnitsOnly ? unit.owner() : -1;
        for (DiplomacyProvince sea : convoyingNeighbours(province, null, fleetOwner))
            extendRoute(province, new ArrayList<>(List.of(sea)), routes, fleetOwner);
        return routes;
    }

    private void extendRoute(DiplomacyProvince from, List<DiplomacyProvince> chain,
                             Map<DiplomacyProvince, Set<DiplomacyProvince>> routes, int fleetOwner) {
        DiplomacyProvince last = chain.get(chain.size() - 1);
        for (DiplomacyLocation l : getMap().fleetMoves(new DiplomacyLocation(last))) {
            DiplomacyProvince p = l.province();
            if (p.type() == DiplomacyProvince.Type.COAST && !p.equals(from))
                routes.computeIfAbsent(p, k -> new HashSet<>()).addAll(chain);
        }
        for (DiplomacyProvince next : convoyingNeighbours(last, chain, fleetOwner)) {
            chain.add(next);
            extendRoute(from, chain, routes, fleetOwner);
            chain.remove(chain.size() - 1);
        }
    }

    /**
     * The sea provinces next to the province holding a fleet (of fleetOwner, or anyone's if -1), other than those
     * already in the chain (null for none).
     */
    private List<DiplomacyProvince> convoyingNeighbours(DiplomacyProvince province, List<DiplomacyProvince> chain,
                                                        int fleetOwner) {
        List<DiplomacyProvince> list = new ArrayList<>();
        for (DiplomacyProvince p : getMap().adjacent(province))
            if (p.type() == DiplomacyProvince.Type.SEA && units[p.index()] != null && units[p.index()].isFleet()
                    && (fleetOwner == -1 || units[p.index()].owner() == fleetOwner)
                    && (chain == null || !chain.contains(p)))
                list.add(p);
        return list;
    }

    /**
     * Where the unit dislodged from the province may retreat.
     */
    public List<DiplomacyLocation> retreats(DiplomacyProvince province) {
        DiplomacyUnit unit = dislodged[province.index()];
        if (unit == null)
            return List.of();
        List<DiplomacyLocation> list = new ArrayList<>();
        for (DiplomacyLocation l : ordinaryMoves(province, unit)) {
            int p = l.province().index();
            if (units[p] == null && p != dislodgedFrom[province.index()] && !standoff[p])
                list.add(l);
        }
        return list;
    }

    /**
     * For testing only: records the unit as dislodged from the province by a unit that came from attackerOrigin
     * (null for none), as the resolution of an orders phase does.
     */
    public void setDislodged(DiplomacyProvince province, DiplomacyUnit unit, DiplomacyProvince attackerOrigin) {
        dislodged[province.index()] = unit;
        dislodgedFrom[province.index()] = attackerOrigin == null ? -1 : attackerOrigin.index();
    }

    /**
     * For testing only: marks the province as left vacant by a standoff in the last orders phase (or not).
     */
    public void setStandoff(DiplomacyProvince province, boolean value) {
        standoff[province.index()] = value;
    }

    public List<DiplomacyResult> getLastResults() {
        return Collections.unmodifiableList(lastResults);
    }

    /**
     * The power's units still to be ordered in the current phase, in the order they are to be ordered; empty in the
     * adjustment phase.
     */
    public List<DiplomacyProvince> unitsToOrder(int power) {
        DiplomacyPhase phase = getPhase();
        if (phase == DiplomacyPhase.ADJUSTMENTS)
            return List.of();
        Set<DiplomacyProvince> ordered = new HashSet<>();
        for (DiplomacyOrder o : orders.get(power))
            ordered.add(o.province());
        // in an orders phase its units, in a retreat phase its dislodged units, in province index order
        DiplomacyUnit[] toOrder = phase.isOrders() ? units : dislodged;
        List<DiplomacyProvince> list = new ArrayList<>();
        for (DiplomacyProvince p : getMap().provinces())
            if (toOrder[p.index()] != null && toOrder[p.index()].owner() == power && !ordered.contains(p))
                list.add(p);
        return list;
    }

    /**
     * Whether the power still has a decision to make in the current phase.
     */
    public boolean hasOrdersToGive(int power) {
        if (getPhase() != DiplomacyPhase.ADJUSTMENTS)
            return !unitsToOrder(power).isEmpty();
        int adjustment = adjustment(power);
        List<DiplomacyOrder> given = orders.get(power);
        if (adjustment < 0)
            return given.size() < -adjustment;
        if (adjustment > 0)
            return given.size() < adjustment && given.stream().noneMatch(o -> o instanceof WaiveBuilds)
                    && !freeHomeCentres(power).isEmpty();
        return false;
    }

    /**
     * The power's supply centres less its units: the builds it is entitled to if positive, the units it must
     * disband if negative.
     */
    public int adjustment(int power) {
        return nCentres(power) - nUnits(power);
    }

    /**
     * The power's home supply centres where it may build now, in index order.
     */
    public List<DiplomacyProvince> freeHomeCentres(int power) {
        Set<DiplomacyProvince> built = new HashSet<>();
        for (DiplomacyOrder o : orders.get(power))
            if (o instanceof Build)
                built.add(o.province());
        List<DiplomacyProvince> list = new ArrayList<>();
        for (DiplomacyProvince p : getMap().homeCentres(power))
            if (owner[p.index()] == power && units[p.index()] == null && !built.contains(p))
                list.add(p);
        return list;
    }

    /**
     * For testing only: puts the unit (or null for none) in the province.
     */
    public void setUnit(DiplomacyProvince province, DiplomacyUnit unit) {
        units[province.index()] = unit;
    }

    /**
     * For testing only: gives control of the supply centre to the power (-1 for nobody).
     */
    public void setOwner(DiplomacyProvince province, int power) {
        owner[province.index()] = power;
    }

    @Override
    public void redeterminise(int playerId) {
        // The other powers' orders for this phase are hidden: wipe them, so that in the copy they give them again.
        // Modelling what they ordered is the responsibility of the deciding agent.
        for (int p = 0; p < getNPlayers(); p++)
            if (p != playerId)
                orders.get(p).clear();
        // The observer acts first in the copy if it still has orders to give; the wiped powers then follow it in turn
        if (hasOrdersToGive(playerId))
            setTurnOwner(playerId);
    }

    @Override
    protected DiplomacyGameState _copy(int playerId) {
        DiplomacyGameState copy = new DiplomacyGameState(gameParameters, getNPlayers());
        // units are immutable records, so the arrays can be copied shallowly
        copy.units = units.clone();
        copy.owner = owner.clone();
        copy.year = year;
        copy.orders = new ArrayList<>();
        for (List<DiplomacyOrder> list : orders)
            copy.orders.add(new ArrayList<>(list));
        copy.dislodged = dislodged.clone();
        copy.dislodgedFrom = dislodgedFrom.clone();
        copy.standoff = standoff.clone();
        copy.lastResults = new ArrayList<>(lastResults);
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal())
            return Math.min(1.0, nCentres(playerId) / (double) getMap().victoryCentres());
        return getPlayerResults()[playerId].value;
    }

    /**
     * The number of supply centres the power controls.
     */
    @Override
    public double getGameScore(int playerId) {
        return nCentres(playerId);
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof DiplomacyGameState other)) return false;
        return year == other.year && Arrays.equals(units, other.units) && Arrays.equals(owner, other.owner)
                && orders.equals(other.orders) && Arrays.equals(dislodged, other.dislodged)
                && Arrays.equals(dislodgedFrom, other.dislodgedFrom) && Arrays.equals(standoff, other.standoff)
                && lastResults.equals(other.lastResults);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), year, orders, lastResults);
        result = 31 * result + Arrays.hashCode(units);
        result = 31 * result + Arrays.hashCode(owner);
        result = 31 * result + Arrays.hashCode(dislodged);
        result = 31 * result + Arrays.hashCode(dislodgedFrom);
        result = 31 * result + Arrays.hashCode(standoff);
        return result;
    }
}

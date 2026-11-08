package games.risk.components;

import core.components.Card;
import games.risk.RiskContinent;
import games.risk.RiskGameState;
import games.risk.RiskTerritory;

import java.util.List;
import java.util.Objects;

/**
 * A Secret Mission card, of one of three kinds:
 * <ul>
 *     <li>conquer the given continents (and, with anotherContinent, one more of the player's choice);</li>
 *     <li>occupy nTerritories territories, each with at least minArmies armies;</li>
 *     <li>destroy the player target, or occupy the map's backupTerritories territories when that cannot be done
 *     (see isBackup).</li>
 * </ul>
 * The conquest and occupy missions come from the map file (RiskMap.missions()). There is a destroy mission for
 * each colour, whether or not it is in the game.
 */
public class RiskMission extends Card {

    public enum Kind {CONQUER, OCCUPY, DESTROY}

    public final Kind kind;
    /** CONQUER: the continents named on the card */
    public final List<RiskContinent> continents;
    /** CONQUER: one more continent of the player's choice is needed */
    public final boolean anotherContinent;
    /** OCCUPY */
    public final int nTerritories;
    public final int minArmies;
    /** DESTROY: the player id */
    public final int target;

    private RiskMission(String name, Kind kind, List<RiskContinent> continents, boolean anotherContinent,
                        int nTerritories, int minArmies, int target) {
        super(name);
        this.kind = kind;
        this.continents = List.copyOf(continents);
        this.anotherContinent = anotherContinent;
        this.nTerritories = nTerritories;
        this.minArmies = minArmies;
        this.target = target;
    }

    public static RiskMission conquer(List<RiskContinent> continents, boolean anotherContinent) {
        StringBuilder name = new StringBuilder("Conquer ");
        for (int i = 0; i < continents.size(); i++)
            name.append(i == 0 ? "" : " and ").append(continents.get(i).name());
        if (anotherContinent)
            name.append(", and a 3rd continent");
        return new RiskMission(name.toString(), Kind.CONQUER, continents, anotherContinent, 0, 0, -1);
    }

    public static RiskMission occupy(int nTerritories, int minArmies) {
        String name = "Occupy " + nTerritories + " territories"
                + (minArmies > 1 ? ", each with " + minArmies + " armies" : "");
        return new RiskMission(name, Kind.OCCUPY, List.of(), false, nTerritories, minArmies, -1);
    }

    public static RiskMission destroy(int target) {
        return new RiskMission("Destroy player " + target, Kind.DESTROY, List.of(), false, 0, 0, target);
    }

    /**
     * Whether the holder has completed this mission in the given state.
     */
    public boolean isComplete(RiskGameState state, int holder) {
        return switch (kind) {
            case CONQUER -> {
                for (RiskContinent c : continents)
                    if (!state.holdsContinent(holder, c))
                        yield false;
                if (!anotherContinent)
                    yield true;
                for (RiskContinent c : state.getMap().continents())
                    if (!continents.contains(c) && state.holdsContinent(holder, c))
                        yield true;
                yield false;
            }
            case OCCUPY -> occupies(state, holder, nTerritories, minArmies);
            case DESTROY -> isBackup(state, holder)
                    ? occupies(state, holder, state.getMap().backupTerritories(), 1)
                    : state.getEliminatedBy(target) == holder;
        };
    }

    /**
     * Whether this is a destroy mission that cannot be completed - its target is the holder, is not in the game, or
     * has been eliminated by someone else - and so is to occupy the map's backupTerritories territories instead.
     */
    public boolean isBackup(RiskGameState state, int holder) {
        return kind == Kind.DESTROY && (target == holder || target >= state.getNPlayers()
                || (state.isEliminated(target) && state.getEliminatedBy(target) != holder));
    }

    private static boolean occupies(RiskGameState state, int holder, int nTerritories, int minArmies) {
        int n = 0;
        for (RiskTerritory t : state.getTerritories(holder))
            if (state.getArmies(t) >= minArmies)
                n++;
        return n >= nTerritories;
    }

    @Override
    public RiskMission copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RiskMission other && other.kind == kind && other.continents.equals(continents)
                && other.anotherContinent == anotherContinent && other.nTerritories == nTerritories
                && other.minArmies == minArmies && other.target == target;
    }

    @Override
    public int hashCode() {
        // the ordinal, so the hash is the same from one run to the next
        return Objects.hash(kind.ordinal(), continents, anotherContinent, nTerritories, minArmies, target) + 731227;
    }

    @Override
    public String toString() {
        return getComponentName();
    }
}

package games.risk.components;

import core.components.Card;
import games.risk.RiskTerritory;

import java.util.Objects;

/**
 * A RISK card: a territory with an Infantry, Cavalry or Artillery symbol, or a wild card (no territory, all three
 * symbols). Two wild cards are equal.
 */
public class RiskCard extends Card {

    public enum Symbol {
        INFANTRY, CAVALRY, ARTILLERY, WILD
    }

    /** null for a wild card */
    public final RiskTerritory territory;
    public final Symbol symbol;

    public RiskCard(RiskTerritory territory) {
        super(territory.name());
        this.territory = territory;
        this.symbol = territory.symbol();
    }

    /**
     * A wild card.
     */
    public RiskCard() {
        super("Wild");
        this.territory = null;
        this.symbol = Symbol.WILD;
    }

    public boolean isWild() {
        return symbol == Symbol.WILD;
    }

    @Override
    public RiskCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RiskCard other && Objects.equals(other.territory, territory) && other.symbol == symbol;
    }

    @Override
    public int hashCode() {
        // the ordinal, so the hash is the same from one run to the next
        return Objects.hash(territory, symbol.ordinal()) + 48271;
    }

    @Override
    public String toString() {
        return isWild() ? "Wild" : territory.name() + " (" + symbol + ")";
    }
}

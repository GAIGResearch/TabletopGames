package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.risk.RiskGameState;
import games.risk.RiskParameters;
import games.risk.RiskTerritory;

import java.util.Objects;

/**
 * Attack again and again (allowBlitz): the current player attacks to from from with the most dice allowed, roll
 * after roll, until to is captured or from is down to 1 army. The defender always rolls the most dice allowed. On a
 * capture the usual move-in choice follows, with the dice of the last roll as the minimum.
 */
public class Blitz extends AbstractAction {

    public final RiskTerritory from;
    public final RiskTerritory to;

    public Blitz(RiskTerritory from, RiskTerritory to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        while (state.getArmies(from) > 1 && state.getArmies(to) > 0) {
            int nDice = Math.min(params.maxAttackDice, state.getArmies(from) - 1);
            Attack.rollAndCapture(state, player, from, to, nDice, Attack.maxDefendDice(state, to));
        }
        return true;
    }

    @Override
    public Blitz copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Blitz other && other.from.equals(from) && other.to.equals(to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to) + 731223;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Blitz(" + from + ", " + to + ")";
    }
}

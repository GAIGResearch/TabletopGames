package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.risk.RiskGameState;
import games.risk.RiskTerritory;

import java.util.Objects;

/**
 * The defender's choice to roll nDefendDice dice against an attack from from on to with nAttackDice dice
 * (defenderChoosesDice). The dice are rolled, and the attacker - the owner of from - captures to if it is emptied.
 */
public class DefendWith extends AbstractAction {

    public final RiskTerritory from;
    public final RiskTerritory to;
    public final int nAttackDice;
    public final int nDefendDice;

    public DefendWith(RiskTerritory from, RiskTerritory to, int nAttackDice, int nDefendDice) {
        this.from = from;
        this.to = to;
        this.nAttackDice = nAttackDice;
        this.nDefendDice = nDefendDice;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        Attack.rollAndCapture(state, state.getOwner(from), from, to, nAttackDice, nDefendDice);
        return true;
    }

    @Override
    public DefendWith copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DefendWith other && other.from.equals(from) && other.to.equals(to)
                && other.nAttackDice == nAttackDice && other.nDefendDice == nDefendDice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, nAttackDice, nDefendDice) + 731225;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "DefendWith(" + from + ", " + to + ", " + nAttackDice + " v " + nDefendDice + ")";
    }
}

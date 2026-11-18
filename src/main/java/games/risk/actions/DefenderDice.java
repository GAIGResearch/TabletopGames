package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import games.risk.RiskGameState;
import games.risk.RiskTerritory;

import java.util.ArrayList;
import java.util.List;

/**
 * The defender's decision after an Attack when defenderChoosesDice is set: how many dice to roll, from 1 up to
 * min(maxDefendDice, armies on to). Offers DefendWith(from, to, nAttackDice, n).
 */
public class DefenderDice extends OneShotExtendedAction {

    public final RiskTerritory from;
    public final RiskTerritory to;
    public final int nAttackDice;

    // OneShotExtendedAction's equals and hashCode compare the name, so it must show every field
    public DefenderDice(int defender, RiskTerritory from, RiskTerritory to, int nAttackDice) {
        super("Defend " + to.name() + " from " + from.name() + " (" + nAttackDice + " dice)", defender,
                gs -> choices(gs, from, to, nAttackDice));
        this.from = from;
        this.to = to;
        this.nAttackDice = nAttackDice;
    }

    private static List<AbstractAction> choices(AbstractGameState gs, RiskTerritory from, RiskTerritory to,
                                                int nAttackDice) {
        RiskGameState state = (RiskGameState) gs;
        List<AbstractAction> actions = new ArrayList<>();
        for (int n = 1; n <= Attack.maxDefendDice(state, to); n++)
            actions.add(new DefendWith(from, to, nAttackDice, n));
        return actions;
    }

    @Override
    public DefenderDice copy() {
        DefenderDice retValue = new DefenderDice(player, from, to, nAttackDice);
        retValue.executed = executed;
        return retValue;
    }
}

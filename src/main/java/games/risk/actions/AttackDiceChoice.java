package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import games.risk.RiskGameState;
import games.risk.RiskParameters;
import games.risk.RiskTerritory;

import java.util.ArrayList;
import java.util.List;

/**
 * The follow-on choice after ChooseAttack: an Attack with 1 up to min(maxAttackDice, armies on from - 1) dice, or a
 * Blitz (allowBlitz).
 */
public class AttackDiceChoice extends OneShotExtendedAction {

    public final RiskTerritory from;
    public final RiskTerritory to;

    // OneShotExtendedAction's equals and hashCode compare the name, so it must show every field
    public AttackDiceChoice(int player, RiskTerritory from, RiskTerritory to) {
        super("Attack " + to.name() + " from " + from.name(), player, gs -> choices(gs, from, to));
        this.from = from;
        this.to = to;
    }

    private static List<AbstractAction> choices(AbstractGameState gs, RiskTerritory from, RiskTerritory to) {
        RiskGameState state = (RiskGameState) gs;
        RiskParameters params = (RiskParameters) state.getGameParameters();
        List<AbstractAction> actions = new ArrayList<>();
        int maxDice = Math.min(params.maxAttackDice, state.getArmies(from) - 1);
        for (int n = 1; n <= maxDice; n++)
            actions.add(new Attack(from, to, n));
        if (params.allowBlitz)
            actions.add(new Blitz(from, to));
        return actions;
    }

    @Override
    public AttackDiceChoice copy() {
        AttackDiceChoice retValue = new AttackDiceChoice(player, from, to);
        retValue.executed = executed;
        return retValue;
    }
}

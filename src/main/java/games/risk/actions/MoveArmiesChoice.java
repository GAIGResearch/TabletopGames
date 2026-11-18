package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import games.risk.RiskGameState;
import games.risk.RiskParameters;
import games.risk.RiskTerritory;
import games.risk.RiskUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The follow-on choice of how many armies to move from one territory to another, from min (the dice rolled after a
 * capture, or 1 when fortifying) up to the most allowed. With more numbers than maxMoveChoices, only the least, the
 * most and evenly spaced numbers between them are offered.
 */
public class MoveArmiesChoice extends OneShotExtendedAction {

    public final RiskTerritory from;
    public final RiskTerritory to;
    public final int min;

    // OneShotExtendedAction's equals and hashCode compare the name, so it must show every field
    public MoveArmiesChoice(int player, RiskTerritory from, RiskTerritory to, int min) {
        super("Move armies " + from.name() + " -> " + to.name() + " (min " + min + ")", player,
                gs -> choices(gs, from, to, min));
        this.from = from;
        this.to = to;
        this.min = min;
    }

    private static List<AbstractAction> choices(AbstractGameState gs, RiskTerritory from, RiskTerritory to, int min) {
        RiskGameState state = (RiskGameState) gs;
        RiskParameters params = (RiskParameters) state.getGameParameters();
        // all but one of the armies on from, and no more than to has room for
        int max = Math.min(state.getArmies(from) - 1, state.getRoom(to));
        List<AbstractAction> actions = new ArrayList<>();
        for (int n : RiskUtils.spread(min, max, params.maxMoveChoices))
            actions.add(new MoveArmies(from, to, n));
        return actions;
    }

    @Override
    public MoveArmiesChoice copy() {
        MoveArmiesChoice retValue = new MoveArmiesChoice(player, from, to, min);
        retValue.executed = executed;
        return retValue;
    }
}

package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import games.risk.RiskGameState;
import games.risk.RiskTerritory;

import java.util.ArrayList;
import java.util.List;

/**
 * The follow-on choice of how many armies to move from one territory to another. After a capture it is any number
 * from min (the dice rolled) up to the most allowed; when fortifying (halfOrAll) it is only the most allowed or half
 * of it, rounded down.
 */
public class MoveArmiesChoice extends OneShotExtendedAction {

    public final RiskTerritory from;
    public final RiskTerritory to;
    public final int min;
    public final boolean halfOrAll;

    public MoveArmiesChoice(int player, RiskTerritory from, RiskTerritory to, int min) {
        this(player, from, to, min, false);
    }

    // OneShotExtendedAction's equals and hashCode compare the name, so it must show every field
    public MoveArmiesChoice(int player, RiskTerritory from, RiskTerritory to, int min, boolean halfOrAll) {
        super("Move armies " + from.name() + " -> " + to.name() + (halfOrAll ? " (half or all)" : " (min " + min + ")"),
                player, gs -> choices(gs, from, to, min, halfOrAll));
        this.from = from;
        this.to = to;
        this.min = min;
        this.halfOrAll = halfOrAll;
    }

    private static List<AbstractAction> choices(AbstractGameState gs, RiskTerritory from, RiskTerritory to, int min,
                                                boolean halfOrAll) {
        RiskGameState state = (RiskGameState) gs;
        List<AbstractAction> actions = new ArrayList<>();
        // all but one of the armies on from, and no more than to has room for
        int max = Math.min(state.getArmies(from) - 1, state.getRoom(to));
        if (halfOrAll) {
            // half is left out when it rounds down below min (moving 1 of a possible 1)
            if (max / 2 >= min)
                actions.add(new MoveArmies(from, to, max / 2));
            actions.add(new MoveArmies(from, to, max));
            return actions;
        }
        for (int n = min; n <= max; n++)
            actions.add(new MoveArmies(from, to, n));
        return actions;
    }

    @Override
    public MoveArmiesChoice copy() {
        MoveArmiesChoice retValue = new MoveArmiesChoice(player, from, to, min, halfOrAll);
        retValue.executed = executed;
        return retValue;
    }
}

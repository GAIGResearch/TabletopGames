package games.hareandtortoise.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import games.hareandtortoise.HareAndTortoiseGameState;
import games.hareandtortoise.HareAndTortoiseParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Player chooses to either draw or discard carrots.
 */
public class DrawOrDiscardCarrots extends OneShotExtendedAction {

    public DrawOrDiscardCarrots(int player) {
        super("Draw or discard carrots", player, DrawOrDiscardCarrots::choices);
    }

    private static List<AbstractAction> choices(AbstractGameState gs) {
        HareAndTortoiseGameState state = (HareAndTortoiseGameState) gs;
        List<AbstractAction> actions = new ArrayList<>();
        actions.add(new ChewCarrot(true));
        // a player can discard only carrots they hold
        if (state.getCarrots(state.getCurrentPlayer())
                >= ((HareAndTortoiseParameters) state.getGameParameters()).carrotsPerChew)
            actions.add(new ChewCarrot(false));
        return actions;
    }

    @Override
    public DrawOrDiscardCarrots copy() {
        DrawOrDiscardCarrots retValue = new DrawOrDiscardCarrots(player);
        retValue.executed = executed;
        return retValue;
    }
}

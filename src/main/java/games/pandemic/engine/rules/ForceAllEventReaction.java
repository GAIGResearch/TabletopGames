package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.IAction;
import games.pandemic.PandemicGameState;

import java.util.List;

public class ForceAllEventReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        int nPlayers = gs.getNPlayers();

        for (int i = 0; i < nPlayers; i++) {
            List<IAction> acts = pgs.getEventActions(i);
            pgs.addReactivePlayer(i, acts);
        }
        return false;
    }
}

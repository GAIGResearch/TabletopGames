package games.pandemic.rules.rules;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.rules.Node;
import core.rules.nodetypes.RuleNode;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicTurnOrder;

import static games.pandemic.PandemicGameState.PandemicGamePhase.DiscardReaction;
import static core.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class ForceDiscardReaction extends RuleNode {

    public ForceDiscardReaction(){
        super();
    }

    /**
     * Copy constructor
     * @param forceDiscardReaction - Node to be copied
     */
    public ForceDiscardReaction(ForceDiscardReaction forceDiscardReaction){
        super(forceDiscardReaction);
    }

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        // player needs to discard cards (doing 1 at a time)
        ((PandemicTurnOrder)pgs.getTurnOrder()).addCurrentPlayerReaction(gs);
        pgs.setGamePhase(DiscardReaction);
        return false;
    }

    @Override
    protected Node _copy() {
        return new ForceDiscardReaction(this);
    }
}

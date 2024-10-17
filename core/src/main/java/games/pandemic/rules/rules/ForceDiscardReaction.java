package games.pandemic.rules.rules;

import core.AbstractGameStateWithTurnOrder;
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
    protected boolean run(AbstractGameStateWithTurnOrder gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        // player needs to discard cards (doing 1 at a time)
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (((Deck<Card>)pgs.getComponent(playerHandHash, i)).isOverCapacity()) {
                ((PandemicTurnOrder) pgs.getTurnOrder()).addReactivePlayer(i);
                break;
            }
        }
        pgs.setGamePhase(DiscardReaction);
        return false;
    }

    @Override
    protected Node _copy() {
        return new ForceDiscardReaction(this);
    }
}

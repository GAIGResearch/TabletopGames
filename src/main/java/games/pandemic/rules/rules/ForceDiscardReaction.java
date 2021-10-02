package games.pandemic.rules.rules;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.rules.nodetypes.RuleNode;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicTurnOrder;

import static games.pandemic.PandemicGameState.PandemicGamePhase.DiscardReaction;
import static core.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class ForceDiscardReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        // player needs to discard N cards
        Deck<Card> playerDeck = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);
        int nDiscards = playerDeck.getSize() - playerDeck.getCapacity();
        for (int i = 0; i < nDiscards; i++) {
            ((PandemicTurnOrder)pgs.getTurnOrder()).addCurrentPlayerReaction(gs);
        }
        pgs.setGamePhase(DiscardReaction);
        return false;
    }
}

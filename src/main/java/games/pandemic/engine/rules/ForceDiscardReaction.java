package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicTurnOrder;

import java.util.ArrayList;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;
import static games.pandemic.PandemicGameState.GamePhase.DiscardReaction;
import static utilities.CoreConstants.playerHandHash;

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

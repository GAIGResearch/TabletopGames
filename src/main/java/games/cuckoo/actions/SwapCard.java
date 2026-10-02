package games.cuckoo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.cuckoo.CuckooGameState;
import games.cuckoo.CuckooKnowledge;

import static games.cuckoo.CuckooUtils.isKing;

/**
 * Try to exchange the card held with the left-hand neighbour's (the next player still in the game), who refuses if
 * they hold a King. The dealer instead exchanges it for the top card of the draw deck, unless that card is a King.
 */
public class SwapCard extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        CuckooGameState state = (CuckooGameState) gs;
        int player = state.getCurrentPlayer();
        CuckooKnowledge knowledge = state.getKnowledge();
        if (player == state.getDealer()) {
            if (!isKing(state.getDrawDeck().peek())) {
                exchange(state.getPlayerCards().get(player), state.getDrawDeck());
                knowledge.replaced(player);
            }
            return true;
        }
        int neighbour = state.nextPlayerInGame(player);
        if (isKing(state.getPlayerCard(neighbour))) {
            knowledge.revealed(neighbour);
        } else {
            exchange(state.getPlayerCards().get(player), state.getPlayerCards().get(neighbour));
            knowledge.exchanged(player, neighbour);
        }
        return true;
    }

    /**
     * Exchanges the single card in the hand for the top card of the other deck.
     */
    private void exchange(Deck<FrenchCard> hand, Deck<FrenchCard> other) {
        FrenchCard mine = hand.draw();
        hand.add(other.draw());
        other.add(mine);
    }

    @Override
    public SwapCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SwapCard;
    }

    @Override
    public int hashCode() {
        return 481907;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Swap card";
    }
}

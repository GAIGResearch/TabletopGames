package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.toads.*;
import games.toads.components.ToadCard;

public class ForceOpponentDiscard extends AbstractAction {

    public final ToadConstants.ToadCardType type;

    public ForceOpponentDiscard(ToadConstants.ToadCardType type) {
        this.type = type;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Find the card in the opponent's hand, remove it, put it at the bottom of their deck, and draw a new card (if they have one)
        ToadGameState state = (ToadGameState) gs;
        int opponent = 1 - state.getCurrentPlayer();
        Deck<ToadCard> opponentHand = state.getPlayerHand(opponent).copy();
        for (ToadCard card : opponentHand) {
            if (card.type == type) {
                PartialObservableDeck<ToadCard> opponentDeck = state.getPlayerDeck(opponent);
                state.getPlayerHand(opponent).remove(card);  // remove card
                opponentDeck.addToBottom(card); // put at bottom of deck
                opponentDeck.setVisibilityOfComponent(opponentDeck.getSize() - 1, 0, true);
                opponentDeck.setVisibilityOfComponent(opponentDeck.getSize() - 1, 1, true);
                state.getPlayerHand(opponent).add(state.getPlayerDeck(opponent).draw()); // draw a new card
                break;
            }
        }
        return true;
    }

    @Override
    public ForceOpponentDiscard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ForceOpponentDiscard && ((ForceOpponentDiscard) obj).type == type;
    }

    @Override
    public int hashCode() {
        return type.ordinal() - 3031134;
    }


    @Override
    public String toString() {
        return "Force opponent to discard " + type;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

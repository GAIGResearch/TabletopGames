package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.toads.ToadConstants;
import games.toads.ToadGameState;
import games.toads.components.ToadCard;

/**
 * A Siege Cannon's guess of a card in the opponent's hand, by its ToadCardType.guessGroup. NONE_OF_THESE is no guess.
 */
public class GuessCard extends AbstractAction {

    public final ToadConstants.ToadCardType type;

    public GuessCard(ToadConstants.ToadCardType type) {
        this.type = type;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ToadGameState state = (ToadGameState) gs;
        int guesser = state.getCurrentPlayer();
        PartialObservableDeck<ToadCard> opponentHand = state.getPlayerHand(1 - guesser);
        // the first matching card in the opponent's hand becomes visible to the guesser
        for (int i = 0; i < opponentHand.getSize(); i++) {
            if (opponentHand.get(i).type.guessGroup() == type) {
                opponentHand.setVisibilityOfComponent(i, guesser, true);
                break;
            }
        }
        return true;
    }

    @Override
    public GuessCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GuessCard other && other.type == type;
    }

    @Override
    public int hashCode() {
        return type.ordinal() + 574213;
    }

    @Override
    public String toString() {
        return type == ToadConstants.ToadCardType.NONE_OF_THESE ? "No guess" : "Guess " + type.guessName();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

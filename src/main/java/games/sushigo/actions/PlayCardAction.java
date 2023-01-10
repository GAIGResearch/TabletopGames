package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import java.util.Objects;

public class PlayCardAction extends AbstractAction {
    final int playerId;
    public final SGCard.SGCardType cardType;

    public PlayCardAction(int playerId, SGCard.SGCardType cardType)
    {
        this.playerId = playerId;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SGGameState sggs = (SGGameState) gs;
        Deck<SGCard> hand =  sggs.getPlayerHands().get(playerId);
        int cardIndex = -1;
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).type == cardType) {
                cardIndex = i;
                break;
            }
        }
        if (cardIndex == -1)
            throw new AssertionError("No card found in hand of type " + cardType);
        if(sggs.getPlayerChopSticksActivated(playerId) && sggs.getPlayerExtraTurns(playerId) == 0)
        {
            sggs.setPlayerExtraCardPick(cardIndex, playerId);
        }
        else sggs.setPlayerCardPick(cardIndex, playerId);

        return true;
    }

    @Override
    public AbstractAction copy() {
        // immutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof PlayCardAction) {
            // deliberately not including the cardIndex, as these actions are equivalent in play
            PlayCardAction other = (PlayCardAction) obj;
            return other.playerId == playerId && other.cardType == cardType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // deliberately not including the cardIndex
        return Objects.hash(playerId, cardType) - 12692;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + cardType;
    }


    @Override
    public String toString() {
        return "Play " + cardType;
    }
}

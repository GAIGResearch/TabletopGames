package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

public class PlayCard extends DrawCard {
    final int playerID;

    public PlayCard(int fromIndex, int playerID) {
        this.fromIndex = fromIndex;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;
        setCardNotVisible(llgs);
        PartialObservableDeck<LoveLetterCard> from = llgs.getPlayerHandCards().get(playerID);
        deckFrom = from.getComponentID();
        Deck<LoveLetterCard> to = llgs.getPlayerDiscardCards().get(playerID);
        deckTo = to.getComponentID();
        LoveLetterCard card = from.pick(fromIndex);
        if (card != null) {
            cardId = card.getComponentID();
            if (to.add(card, toIndex)) {
                executed = true;
                return true;
            }
        }
        return false;
    }

    protected void setCardNotVisible(LoveLetterGameState llgs) {
        // Set this card to not be visible by other players
        PartialObservableDeck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            if (i != playerID) {
                playerDeck.setVisibilityOfComponent(fromIndex, playerID, false);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCard)) return false;
        if (!super.equals(o)) return false;
        PlayCard playCard = (PlayCard) o;
        return playerID == playCard.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID);
    }
}

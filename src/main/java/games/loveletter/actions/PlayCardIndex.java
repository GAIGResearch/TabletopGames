package games.loveletter.actions;

import core.AbstractGameState;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

public class PlayCardIndex extends PlayCard {
    final int cardIdx;

    public PlayCardIndex(LoveLetterCard.CardType cardType, int cardIdx, int playerID, int targetPlayer, LoveLetterCard.CardType targetCardType, LoveLetterCard.CardType forcedCountessCardType, boolean canExecuteEffect, boolean discard) {
        super(cardType, playerID, targetPlayer, targetCardType, forcedCountessCardType, canExecuteEffect, discard);
        this.cardIdx = cardIdx;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;

        if (discard) {
            PartialObservableDeck<LoveLetterCard> from = llgs.getPlayerHandCards().get(playerID);
            Deck<LoveLetterCard> to = llgs.getPlayerDiscardCards().get(playerID);
            LoveLetterCard card = from.get(cardIdx);
            if (card != null) {
                // Discard card
                from.remove(card);
                to.add(card);
            } else {
                throw new AssertionError("No card in hand matching the required type");
            }
        }

        // Execute card effect
        if (canExecuteEffect) // If not, just discard the card, it's ok
            return _execute(llgs);

        return true;
    }

    @Override
    public PlayCardIndex copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCardIndex)) return false;
        if (!super.equals(o)) return false;
        PlayCardIndex that = (PlayCardIndex) o;
        return cardIdx == that.cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIdx);
    }

    public int getCardIdx() {
        return cardIdx;
    }
}

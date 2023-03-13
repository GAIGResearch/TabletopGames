package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

public abstract class PlayCardIndex extends AbstractAction {
    protected final int playerID;
    final int targetPlayer;
    protected final LoveLetterCard.CardType cardType;
    final LoveLetterCard.CardType targetCardType;
    final LoveLetterCard.CardType forcedCountessCardType;
    final boolean canExecuteEffect;
    final boolean discard;

    public PlayCardIndex(LoveLetterCard.CardType cardType, int playerID, int targetPlayer, LoveLetterCard.CardType targetCardType, LoveLetterCard.CardType forcedCountessCardType, boolean canExecuteEffect, boolean discard) {
        this.cardType = cardType;
        this.playerID = playerID;
        this.targetPlayer = targetPlayer;
        this.targetCardType = targetCardType;
        this.forcedCountessCardType = forcedCountessCardType;
        this.canExecuteEffect = canExecuteEffect;
        this.discard = discard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;

        if (discard) {
            PartialObservableDeck<LoveLetterCard> from = llgs.getPlayerHandCards().get(playerID);
            Deck<LoveLetterCard> to = llgs.getPlayerDiscardCards().get(playerID);
            LoveLetterCard card = null;
            // Find card by type
            for (LoveLetterCard c : from.getComponents()) {
                if (c.cardType == cardType) {
                    card = c;
                    break;
                }
            }
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

    protected abstract boolean _execute(LoveLetterGameState llgs);
    protected abstract String _toString();

    @Override
    public String toString() {
        if (!canExecuteEffect) return cardType + " - no effect";
        else return _toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCardIndex)) return false;
        PlayCardIndex playCard = (PlayCardIndex) o;
        return playerID == playCard.playerID && targetPlayer == playCard.targetPlayer && canExecuteEffect == playCard.canExecuteEffect && discard == playCard.discard && cardType == playCard.cardType && targetCardType == playCard.targetCardType && forcedCountessCardType == playCard.forcedCountessCardType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, targetPlayer, cardType, targetCardType, forcedCountessCardType, canExecuteEffect, discard);
    }

    public int getPlayerID() {
        return playerID;
    }

    public int getTargetPlayer() {
        return targetPlayer;
    }

    public LoveLetterCard.CardType getCardType() {
        return cardType;
    }

    public LoveLetterCard.CardType getForcedCountessCardType() {
        return forcedCountessCardType;
    }

    public LoveLetterCard.CardType getTargetCardType() {
        return targetCardType;
    }

    public boolean canExecuteEffect() {
        return canExecuteEffect;
    }
}

package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

public abstract class PlayCard extends AbstractAction {
    final int playerID, targetPlayer;
    final LoveLetterCard.CardType cardType, targetCardType, forcedCountessCardType;
    final boolean canExecuteEffect;

    public PlayCard(LoveLetterCard.CardType cardType, int playerID, int targetPlayer, LoveLetterCard.CardType targetCardType, LoveLetterCard.CardType forcedCountessCardType, boolean canExecuteEffect) {
        this.cardType = cardType;
        this.playerID = playerID;
        this.targetPlayer = targetPlayer;
        this.targetCardType = targetCardType;
        this.forcedCountessCardType = forcedCountessCardType;
        this.canExecuteEffect = canExecuteEffect;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;

        PartialObservableDeck<LoveLetterCard> from = llgs.getPlayerHandCards().get(playerID);
        Deck<LoveLetterCard> to = llgs.getPlayerDiscardCards().get(playerID);
        LoveLetterCard card = null;
        // Find card by type
        for (LoveLetterCard c: from.getComponents()) {
            if (c.cardType == cardType) {
                card = c;
                break;
            }
        }
        if (card != null) {
            // Discard card
            from.remove(card);
            to.add(card);
            // Execute card effect
            if (canExecuteEffect) // If not, just discard the card, it's ok
                return _execute(llgs);
            return true;
        }
        throw new AssertionError("No card in hand matching the required type");
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
        if (!(o instanceof PlayCard)) return false;
        PlayCard playCard = (PlayCard) o;
        return playerID == playCard.playerID && targetPlayer == playCard.targetPlayer && canExecuteEffect == playCard.canExecuteEffect && cardType == playCard.cardType && targetCardType == playCard.targetCardType && forcedCountessCardType == playCard.forcedCountessCardType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, targetPlayer, cardType, targetCardType, forcedCountessCardType, canExecuteEffect);
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

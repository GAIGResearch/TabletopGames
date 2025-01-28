package games.loveletter.actions;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;

/**
 * The guard allows to attempt guessing another player's card. If the guess is correct, the targeted opponent
 * is removed from the game.
 */
public class GuardAction extends PlayCard implements IPrintable {

    public GuardAction(int cardIdx, int playerID, int opponentID, CardType cardtype, boolean canExecuteEffect, boolean discard) {
        super(CardType.Guard, cardIdx, playerID, opponentID, cardtype, null, canExecuteEffect, discard);
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        if (targetCardType == null) return false;

        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);

        // guess the opponent's card and remove the opponent from play if the guess was correct
        LoveLetterCard card = opponentDeck.peek();
        if (card.cardType == this.targetCardType) {
            llgs.killPlayer(playerID, targetPlayer, cardType);
            if (llgs.getCoreGameParameters().recordEventHistory) {
                llgs.recordHistory("Guard guess correct!");
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GuardAction && super.equals(o);
    }

    @Override
    public GuardAction copy() {
        return this;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return "Guard: guess p" + targetPlayer + " has " + targetCardType.name();
    }
}

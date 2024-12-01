package games.loveletter.actions;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;

/**
 * The Priest allows a player to see another player's hand cards.
 * This has no effect in case the game is fully observable.
 */
public class PriestAction extends PlayCard implements IPrintable {

    public PriestAction(int cardIdx, int playerID, int opponentID, boolean canExecuteEffect, boolean discard) {
        super(CardType.Priest, cardIdx, playerID, opponentID, null, null, canExecuteEffect, discard);
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        PartialObservableDeck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);

        // Set all cards to be visible by the current player
        for (int i = 0; i < opponentDeck.getComponents().size(); i++)
            opponentDeck.setVisibilityOfComponent(i, playerID, true);

        targetCardType = opponentDeck.get(0).cardType;
        if (llgs.getCoreGameParameters().recordEventHistory) {
            llgs.recordHistory("Priest sees " + targetCardType);
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof PriestAction;
    }

    @Override
    public PriestAction copy() {
        PriestAction copy = new PriestAction(cardIdx, playerID, targetPlayer, canExecuteEffect, discard);
        copy.targetCardType = targetCardType;
        return copy;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Priest: see p" + targetPlayer;
    }
}

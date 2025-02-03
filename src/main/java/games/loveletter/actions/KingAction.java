package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants.VisibilityMode;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;


/**
 * The King lets two players swap their hand cards.
 */
public class KingAction extends PlayCard implements IPrintable {

    public KingAction(int cardidx, int playerID, int opponentID, boolean canExecuteEffect, boolean discard) {
        super(CardType.King, cardidx, playerID, opponentID, null, null, canExecuteEffect, discard);
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        Deck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);

        // create a temporary deck to store cards in and then swap cards accordingly
        Deck<LoveLetterCard> tmpDeck = new Deck<>("tmp", VisibilityMode.HIDDEN_TO_ALL);
        while (opponentDeck.getSize() > 0)
            tmpDeck.add(opponentDeck.draw());
        while (playerDeck.getSize() > 0)
            opponentDeck.add(playerDeck.draw());
        while (tmpDeck.getSize() > 0)
            playerDeck.add(tmpDeck.draw());

        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KingAction && super.equals(o);
    }

    @Override
    public KingAction copy() {
        return this;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "King: swap with p" + targetPlayer;
    }
}

package games.loveletter.actions;

import core.actions.IAction;
import core.components.Card;
import core.AbstractGameState;
import core.components.Deck;
import core.observations.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class DrawCard implements IAction, IPrintable {

    private final Deck<LoveLetterCard> deckFrom;
    private final Deck<LoveLetterCard> deckTo;

    private int index;
    private final int playerID;

    public DrawCard (Deck<LoveLetterCard> deckFrom, Deck<LoveLetterCard> deckTo, int index, int playerID) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.index = index;
        this.playerID = playerID;
    }

    public DrawCard (Deck<LoveLetterCard> deckFrom, Deck<LoveLetterCard> deckTo, int playerID) {
        this(deckFrom, deckTo, -1, playerID);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((LoveLetterGameState)gs).setProtection(playerID, false);

        LoveLetterCard card;
        if (index != -1){
            card = deckFrom.pick(index);
        } else {
            card = deckFrom.draw();
        }
        if (card == null) {
            return false;
        }
        return deckTo.add(card);
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString() {
        return "draw a card and remove protection status";
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}

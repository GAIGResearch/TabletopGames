package games.loveletter.actions;

import core.actions.IAction;
import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class DrawCard implements IAction, IPrintable {

    private final IDeck<LoveLetterCard> deckFrom;
    private final IDeck<LoveLetterCard> deckTo;

    private int index;
    private final int playerID;

    public DrawCard (IDeck<LoveLetterCard> deckFrom, IDeck<LoveLetterCard> deckTo, int index, int playerID) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.index = index;
        this.playerID = playerID;
    }

    public DrawCard (IDeck<LoveLetterCard> deckFrom, IDeck<LoveLetterCard> deckTo, int playerID) {
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
    public String toString() {
        return "draw a card and remove protection status";
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}

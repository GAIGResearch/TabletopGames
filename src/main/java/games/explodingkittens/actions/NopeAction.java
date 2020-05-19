package games.explodingkittens.actions;

import core.components.Card;
import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;

public class NopeAction<T> extends PlayCard<T> implements IsNope, IPrintable {

    private final int playerID;

    public NopeAction(T card, IDeck<T> playerDeck, IDeck<T> discardDeck, int playerID) {
        super(card, playerDeck, discardDeck);
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player %d nopes the previous action", playerID);
    }

    @Override
    public boolean isNope() {
        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}

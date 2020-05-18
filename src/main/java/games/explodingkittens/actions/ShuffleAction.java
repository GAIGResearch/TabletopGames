package games.explodingkittens.actions;

import core.components.Card;
import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;
import core.turnorder.TurnOrder;

public class ShuffleAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {
    final IDeck<T> shuffleDeck;

    public ShuffleAction(T card, IDeck<T> playerDeck, IDeck<T> discardPile, IDeck<T> deckToShuffle) {
        super(card, playerDeck, discardPile);
        shuffleDeck = deckToShuffle;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        shuffleDeck.shuffle();
        return super.execute(gs);
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString(){
        return "Player shuffles the deck";
    }


    @Override
    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}

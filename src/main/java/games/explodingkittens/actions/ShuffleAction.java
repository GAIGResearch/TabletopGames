package games.explodingkittens.actions;

import core.components.Card;
import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.observations.IPrintable;
import core.turnorder.TurnOrder;

public class ShuffleAction<T extends Component> extends PlayCard<T> implements IsNopeable, IPrintable {
    final Deck<T> shuffleDeck;

    public ShuffleAction(T card, Deck<T> playerDeck, Deck<T> discardPile, Deck<T> deckToShuffle) {
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

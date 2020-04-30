package updated_core.games.explodingkittens.actions;

import components.Deck;
import components.IDeck;
import updated_core.actions.IPrintable;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public class ShuffleAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {
    final IDeck<T> shuffleDeck;

    public ShuffleAction(T card, IDeck<T> playerDeck, IDeck<T> discardPile, IDeck<T> deckToShuffle) {
        super(card, playerDeck, discardPile);
        shuffleDeck = deckToShuffle;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        shuffleDeck.shuffle();
        return super.Execute(gs, turnOrder);
    }

    @Override
    public String toString(){
        return "Player shuffles the deck";
    }


    @Override
    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.Execute(gs, turnOrder);
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}

package updated_core.games.explodingkittens.actions;

import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;
import updated_core.actions.IPrintable;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public class NopeAction<T> extends PlayCard<T> implements IsNope, IPrintable {

    private final int playerID;

    public NopeAction(T card, IDeck<T> playerDeck, IDeck<T> discardDeck, int playerID) {
        super(card, playerDeck, discardDeck);
        this.playerID = playerID;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        super.Execute(gs, turnOrder);
        return true;
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
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}

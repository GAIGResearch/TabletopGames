package updated_core.games.explodingkittens.actions;

import components.Deck;
import components.IDeck;
import updated_core.actions.IPrintable;
import updated_core.games.explodingkittens.ExplodingKittensGamePhase;
import updated_core.games.explodingkittens.ExplodingKittensGameState;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public class PlaceExplodingKittenAction<T> extends PlayCard<T> implements IPrintable {
    int targetIndex;

    public PlaceExplodingKittenAction(T card, IDeck<T> playerDeck, IDeck<T> drawDeck, int targetIndex) {
        super(card, playerDeck, drawDeck);
        this.targetIndex = targetIndex;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        boolean succes = sourceDeck.remove(card);
        targetDeck.add(card, targetIndex);
        ((ExplodingKittensGameState) gs).gamePhase = ExplodingKittensGamePhase.PlayerMove;
        turnOrder.endPlayerTurn(gs);
        return succes;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player defuses the kitten and places it at index  %d", targetIndex);
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}

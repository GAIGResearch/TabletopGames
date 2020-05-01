package games.explodingkittens.actions;

import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittensGamePhase;
import games.explodingkittens.ExplodingKittensGameState;

public class PlaceExplodingKittenAction<T> extends PlayCard<T> implements IPrintable {
    int targetIndex;

    public PlaceExplodingKittenAction(T card, IDeck<T> playerDeck, IDeck<T> drawDeck, int targetIndex) {
        super(card, playerDeck, drawDeck);
        this.targetIndex = targetIndex;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean succes = sourceDeck.remove(card);
        targetDeck.add(card, targetIndex);
        ((ExplodingKittensGameState) gs).gamePhase = ExplodingKittensGamePhase.PlayerMove;
        gs.getTurnOrder().endPlayerTurn(gs);
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

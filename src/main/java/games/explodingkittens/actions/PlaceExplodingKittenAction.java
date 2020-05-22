package games.explodingkittens.actions;

import core.components.Card;
import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;

public class PlaceExplodingKittenAction<T extends Component> extends PlayCard<T> implements IPrintable {
    int targetIndex;

    public PlaceExplodingKittenAction(T card, Deck<T> playerDeck, Deck<T> drawDeck, int targetIndex) {
        super(card, playerDeck, drawDeck);
        this.targetIndex = targetIndex;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean success = sourceDeck.remove(card);
        targetDeck.add(card, targetIndex);
        gs.setMainGamePhase();
        ((ExplodingKittenTurnOrder)gs.getTurnOrder()).endPlayerTurnStep(gs);
        return success;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player defuses the kitten and places it at index  %d", targetIndex);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}

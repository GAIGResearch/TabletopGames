package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Component;
import core.components.Deck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.util.Objects;

public class DrawRandom extends DrawCard {

    public DrawRandom(int deckFrom, int deckTo) {
        super(deckFrom, deckTo, -1);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        int deckFromSize = ((Deck<Card>)gs.getComponentById(deckFrom)).getSize();
        fromIndex = gs.getRnd().nextInt(deckFromSize);
        return super.execute(gs);
    }

    @Override
    public DrawRandom copy() {
        DrawRandom c = new DrawRandom(deckFrom, deckTo);
        c.fromIndex = fromIndex;
        c.executed = executed;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrawRandom that = (DrawRandom) o;
        return deckFrom == that.deckFrom && deckTo == that.deckTo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckFrom, deckTo);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Component deckF = gameState.getComponentById(deckFrom);
        Component deckT = gameState.getComponentById(deckTo);
        return "DrawRandom{" +
                "deckFrom=" + (deckF != null? deckF.getComponentName() : "deck-from-not-found") +
                ", deckTo=" + (deckT != null? deckT.getComponentName() : "deck-to-not-found") +
                '}';
    }

    @Override
    public String toString() {
        return "DrawRandom{" +
                "deckFrom=" + deckFrom +
                ", deckTo=" + deckTo +
                '}';
    }
}

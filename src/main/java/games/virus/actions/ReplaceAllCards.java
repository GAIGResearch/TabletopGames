package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawComponents;
import core.components.Deck;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;

import java.util.Objects;

public class ReplaceAllCards extends DrawComponents {
    protected int deckDraw;

    public ReplaceAllCards(int deckFrom, int deckTo, int deckDraw, int nComponents) {
        super(deckFrom, deckTo, nComponents);
        this.deckDraw = deckDraw;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        Deck<VirusCard> from     = (Deck<VirusCard>) gs.getComponentById(deckFrom);
        Deck<VirusCard> drawDeck = (Deck<VirusCard>) gs.getComponentById(deckDraw);

        // After discarding a card, the player must draw a card from the draw deck.
        // It is is empty, move all cards from discard deck to draw one and shuffle.
        // After, draw a card and add it to the player hand.
        for (int i = 0; i < nComponents; i++) {
            if (drawDeck.getSize() == 0)
                discardToDraw((VirusGameState)gs);
            VirusCard c = drawDeck.draw();
            from.add(c);
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplaceAllCards)) return false;
        if (!super.equals(o)) return false;
        ReplaceAllCards that = (ReplaceAllCards) o;
        return deckDraw == that.deckDraw;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deckDraw);
    }

    @Override
    public void printToConsole() {
        System.out.println("Replace all virus cards");
    }

    // Move all cards from discard deck to draw one and shuffle
    public void discardToDraw(VirusGameState vgs) {
        vgs.getDrawDeck().add(vgs.getDiscardDeck());
        vgs.getDiscardDeck().clear();
        vgs.getDrawDeck().shuffle(vgs.getRnd());
    }

    @Override
    public AbstractAction copy() {
        return new ReplaceAllCards(deckFrom, deckTo, deckDraw, nComponents);
    }
}

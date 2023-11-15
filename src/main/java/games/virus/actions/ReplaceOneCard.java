package games.virus.actions;


import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Deck;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;

import java.util.Objects;

/**
 * Move 1 component (deckFrom -> deckTo), and draw another to replace them (deckDraw -> deckFrom).
 */
public class ReplaceOneCard extends DrawCard {
    protected int deckDraw;

    public ReplaceOneCard(int deckFrom, int deckTo, int fromIndex, int deckDraw) {
        super(deckFrom, deckTo, fromIndex);
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
        if (drawDeck.getSize() == 0)
            discardToDraw((VirusGameState)gs);
        from.add(drawDeck.draw());

        return true;
    }

    // Move all cards from discard deck to draw one and shuffle
    public void discardToDraw(VirusGameState vgs) {
        while (vgs.getDiscardDeck().getSize()>0) {
            VirusCard card = vgs.getDiscardDeck().draw();
            vgs.getDrawDeck().add(card);
        }
        vgs.getDrawDeck().shuffle(vgs.getRnd());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplaceOneCard)) return false;
        if (!super.equals(o)) return false;
        ReplaceOneCard that = (ReplaceOneCard) o;
        return deckDraw == that.deckDraw;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deckDraw);
    }

    @Override
    public void printToConsole() {
        System.out.println("Replace one virus card");
    }

    @Override
    public AbstractAction copy() {
        return new ReplaceOneCard(deckFrom, deckTo, fromIndex, deckDraw);
    }
}




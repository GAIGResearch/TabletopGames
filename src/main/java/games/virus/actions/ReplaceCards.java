package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawComponents;
import core.components.Deck;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Move n components (deckFrom -> deckTo), and draw n others to replace them (deckDraw -> deckFrom).
 */
public class ReplaceCards extends DrawComponents {
    protected int deckDraw;

    public ReplaceCards(int deckFrom, int deckTo, ArrayList<Integer> cardIds, int deckDraw) {
        super(deckFrom, deckTo, cardIds);
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
        for (int i = 0; i < idxs.size(); i++) {
            if (drawDeck.getSize() == 0)
                discardToDraw((VirusGameState)gs);
            from.add(drawDeck.draw());
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ReplaceCards that = (ReplaceCards) o;
        return deckDraw == that.deckDraw;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deckDraw);
    }

    @Override
    public void printToConsole() {
        System.out.println("Replace virus cards");
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
    public AbstractAction copy() {
        return new ReplaceCards(deckFrom, deckTo, idxs, deckDraw);
    }
}

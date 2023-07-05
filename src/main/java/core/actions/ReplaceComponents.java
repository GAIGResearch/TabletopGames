package core.actions;

import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;

import java.util.Objects;

/**
 * Move n components (deckFrom -> deckTo), and draw n others to replace them (deckDraw -> deckFrom).
 */
public class ReplaceComponents<T extends Component> extends DrawComponents {
    protected int deckDraw;

    public ReplaceComponents(int deckFrom, int deckTo, int nComponents, int deckDraw) {
        super(deckFrom, deckTo, nComponents);
        this.deckDraw = deckDraw;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        Deck<T> from = (Deck<T>) gs.getComponentById(deckFrom);
        Deck<T> drawDeck = (Deck<T>) gs.getComponentById(deckDraw);

        for (int i = 0; i < nComponents; i++) {
            from.add(drawDeck.draw());
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplaceComponents)) return false;
        ReplaceComponents<?> that = (ReplaceComponents<?>) o;
        return deckDraw == that.deckDraw;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckDraw);
    }

    @Override
    public void printToConsole() {
        System.out.println("Replace components");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "ReplaceComponents{" +
                "deckDraw=" + gameState.getComponentById(deckDraw).getComponentName() +
                "deckTo=" + gameState.getComponentById(deckTo).getComponentName() +
                "drawFrom=" + gameState.getComponentById(deckFrom).getComponentName() +
                '}';
    }

    @Override
    public String toString() {
        return "ReplaceComponents{" +
                "deckDraw=" + deckDraw +
                '}';
    }

    @Override
    public AbstractAction copy() {
        return new ReplaceComponents<T>(deckFrom, deckTo, nComponents, deckDraw);
    }
}

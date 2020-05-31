package core.actions;

import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;

import java.util.Arrays;
import java.util.Objects;


public class DrawComponents<T extends Component> extends AbstractAction {
    private boolean executed;
    private int[] fromIds;

    protected int deckFrom, deckTo, nComponents;

    public DrawComponents(int deckFrom, int deckTo, int nComponents) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.nComponents = nComponents;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        executed = true;
        Deck<T> from = (Deck<T>) gs.getComponentById(deckFrom);
        Deck<T> to = (Deck<T>) gs.getComponentById(deckTo);

        for (int i = 0; i < nComponents; i++) {
            T component = from.pick();
            fromIds[i] = component.getComponentID();
            to.add(component);
        }

        return true;
    }

    public T getComponent(AbstractGameState gs, int idx) {
        if (executed) {
            return (T) gs.getComponentById(fromIds[idx]);
        } else {
            return ((Deck<T>) gs.getComponentById(deckFrom)).getComponents().get(idx);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrawComponents<?> that = (DrawComponents<?>) o;
        return executed == that.executed &&
                deckFrom == that.deckFrom &&
                deckTo == that.deckTo &&
                nComponents == that.nComponents &&
                Arrays.equals(fromIds, that.fromIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(executed, deckFrom, deckTo, nComponents);
        result = 31 * result + Arrays.hashCode(fromIds);
        return result;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }

    @Override
    public void printToConsole() {
        System.out.println("Draw components");
    }
}

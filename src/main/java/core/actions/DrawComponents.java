package core.actions;

import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;

import java.util.Arrays;
import java.util.Objects;


public class DrawComponents<T extends Component> extends AbstractAction {

    protected int deckFrom, deckTo, nComponents;

    private int[] componentIds;  // Component IDs for all components moved from one deck to the other
    private boolean executed;  // Indicates if the action executed

    /**
     * Moves the first N components from one deck to another.
     * @param deckFrom - origin deck from which to move components.
     * @param deckTo - destination deck to which to move components.
     * @param nComponents - how many components should be moved.
     */
    public DrawComponents(int deckFrom, int deckTo, int nComponents) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.nComponents = nComponents;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        executed = true;
        componentIds = new int[nComponents];
        Deck<T> from = (Deck<T>) gs.getComponentById(deckFrom);
        Deck<T> to = (Deck<T>) gs.getComponentById(deckTo);

        for (int i = 0; i < nComponents; i++) {
            T component = from.pick();
            componentIds[i] = component.getComponentID();
            to.add(component);
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return new DrawComponents<T>(deckFrom, deckTo, nComponents);
    }

    public T getComponent(AbstractGameState gs, int idx) {
        if (executed) {
            return (T) gs.getComponentById(componentIds[idx]);
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
                Arrays.equals(componentIds, that.componentIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(executed, deckFrom, deckTo, nComponents);
        result = 31 * result + Arrays.hashCode(componentIds);
        return result;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Deck<T> from = (Deck<T>) gameState.getComponentById(deckFrom);
        String components = "{";
        if (executed) {
            for (int id : componentIds) {
                components += gameState.getComponentById(id).getComponentName() + ", ";
            }
        } else {
            for (int i = 0; i < nComponents; i++) {
                components += from.peek(i).getComponentName() + ", ";
            }
        }
        components += "}";
        components = components.replace(", }", "}");
        return "DrawComponents{" +
                "deckFrom=" + from.getComponentName() +
                ", deckTo=" + gameState.getComponentById(deckTo).getComponentName() +
                ", nComponents=" + nComponents +
                ", components=" + components +
                '}';
    }

    @Override
    public String toString() {
        return "DrawComponents{" +
                "deckFrom=" + deckFrom +
                ", deckTo=" + deckTo +
                ", nComponents=" + nComponents +
                ", fromIds=" + Arrays.toString(componentIds) +
                ", executed=" + executed +
                '}';
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(getString(gameState));
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}

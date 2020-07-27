package core.actions;

import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;


public class DrawComponents<T extends Component> extends AbstractAction {

    protected int deckFrom, deckTo, nComponents;

    private int[] componentIds;        // Component IDs for all components moved from one deck to the other
    private boolean executed;          // Indicates if the action executed
    protected ArrayList<Integer> ids;  // Ids of the components to be moved

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
        this.ids = new ArrayList<>();

        // the first nComponents will be moved
        for (int i=0; i<nComponents; i++)
            ids.add(i);
    }

    /**
     * Moves the components, specified in ids, from one deck to another.
     * @param deckFrom - origin deck from which to move components.
     * @param deckTo - destination deck to which to move components.
     * @param ids - ids of the components to be moved.
     */
    public DrawComponents(int deckFrom, int deckTo, ArrayList<Integer> ids) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.ids = new ArrayList<>();
        this.nComponents = ids.size();

        this.ids.addAll(ids);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        executed = true;
        componentIds = new int[nComponents];
        Deck<T> from = (Deck<T>) gs.getComponentById(deckFrom);
        Deck<T> to   = (Deck<T>) gs.getComponentById(deckTo);

        for (int i = 0; i < ids.size(); i++) {
            if (from.getSize() > 0) {
                T component = from.pick(ids.get(i));
                // actualize ids
                for (int j=i; j<ids.size(); j++)
                    ids.set(j,ids.get(j) - 1);
                componentIds[i] = component.getComponentID();
                to.add(component);
            } else {
                return false;
            }
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return new DrawComponents<T>(deckFrom, deckTo, ids);
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
        if (!(o instanceof DrawComponents)) {
            return false;
        }
        DrawComponents<?> that = (DrawComponents<?>) o;
        return deckFrom == that.deckFrom &&
                deckTo == that.deckTo &&
                nComponents == that.nComponents &&
                ids.equals(that.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckFrom, deckTo, nComponents, ids);
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
                components += from.peek(ids.get(i)).getComponentName() + ", ";
            }
        }
        components += "}";
        components = components.replace(", }", "}");
        return "DrawComponents{" +
                "deckFrom=" + from.getComponentName() +
                ", deckTo=" + gameState.getComponentById(deckTo).getComponentName() +
                ", nComponents=" + nComponents +
                ", components=" + components +
                ", ids=" + ids.toString() +
                '}';
    }

    @Override
    public String toString() {
        return "DrawComponents{" +
                "deckFrom=" + deckFrom +
                ", deckTo=" + deckTo +
                ", nComponents=" + nComponents +
                ", ids=" + ids.toString() +
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

package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.*;

import static java.util.stream.Collectors.*;

public class CompositeAction extends AbstractAction {

    final List<AbstractAction> components;

    public CompositeAction(AbstractAction... actions) {
        components = Arrays.asList(actions);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return components.stream().allMatch(a -> a.execute(gs));
    }

    @Override
    public AbstractAction copy() {
        List<AbstractAction> newActions = components.stream().map(AbstractAction::copy).collect(toList());
        return new CompositeAction(newActions.toArray(new AbstractAction[0]));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompositeAction) {
            CompositeAction other = (CompositeAction) obj;
            if (components.size() != other.components.size())
                return false;
            for (int i = 0; i < components.size(); i++) {
                if (!components.get(i).equals(other.components.get(i)))
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(components);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return components.stream().map(a -> a.getString(gameState)).collect(joining("\n"));
    }

    @Override
    public String toString() {
        return components.stream().map(Objects::toString).collect(joining("\n"));
    }
}

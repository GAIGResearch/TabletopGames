package games.dicemonastery;

import core.components.*;
import utilities.Utils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static games.dicemonastery.DiceMonasteryConstants.*;

/**
 * A wrapper around Area to provide helper methods to get stuff
 */
public class DMArea {

    protected Area area;

    public DMArea(int owner, String name) {
        area = new Area(owner, name);
    }
    private DMArea(Area area) {
        this.area = area;
    }

    public Token take(Resource resource, int player) {
        Optional<Token> cube = area.getComponents().values().stream()
                .filter(c -> c.getType() == Utils.ComponentType.TOKEN
                        && c.getOwnerId() == player
                        && ((Token) c).getTokenType().equals(resource.name()))
                .map(c -> (Token) c)
                .findFirst();
        if (cube.isPresent()) {
            removeComponent(cube.get());
            return cube.get();
        } else
            throw new AssertionError(String.format("Cannot process action. No %s for player %d in %s", resource, player, area.getComponentName()));
    }

    public int count(Resource resource, int player) {
        return (int) area.getComponents().values().stream()
                .filter(c -> c instanceof Token
                        && c.getOwnerId() == player
                        && ((Token) c).getTokenType().equals(resource.name()))
                .count();
    }

    public DMArea copy() {
        Area emptyArea = area.emptyCopy();
        area.getComponents().values().forEach( c -> emptyArea.putComponent(c.copy()));
        return new DMArea(emptyArea);
    }

    public List<Component> getAll(Predicate<Component> filter) {
        return area.getComponents().values().stream().filter(filter).collect(Collectors.toList());
    }

    public void putComponent(Component c) {
        area.putComponent(c);
    }

    public void removeComponent(Component c) {
        area.removeComponent(c);
    }

    public int size() {
        return area.size();
    }

    @Override
    public int hashCode() {
        return area.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DMArea) {
            DMArea other = (DMArea) obj;
            return area.equals(other.area);
        }
        return false;
    }
}

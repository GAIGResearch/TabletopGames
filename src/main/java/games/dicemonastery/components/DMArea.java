package games.dicemonastery.components;

import core.components.Area;
import core.components.Component;
import core.components.Token;
import games.dicemonastery.DiceMonasteryConstants.BONUS_TOKEN;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static games.dicemonastery.DiceMonasteryConstants.Resource;
import static java.util.stream.Collectors.toList;

/**
 * A wrapper around Area to provide helper methods to get stuff
 */
public class DMArea {

    protected Area area;
    BONUS_TOKEN[] tokens = new BONUS_TOKEN[2];

    public DMArea(int owner, String name) {
        area = new Area(owner, name);
    }
    private DMArea(Area area) {
        this.area = area;
    }

    public Token take(Resource resource, int player) {
        Optional<Token> cube = area.stream()
                .filter(c -> c instanceof Token
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
        return (int) area.stream()
                .filter(c -> c instanceof Token
                        && c.getOwnerId() == player
                        && ((Token) c).getTokenType().equals(resource.toString()))
                .count();
    }

    public void setToken(int i, BONUS_TOKEN token) {
        tokens[i] = token;
    }

    public BONUS_TOKEN[] getTokens() {
        return tokens.clone();
    }

    public DMArea copy() {
        Area emptyArea = area.emptyCopy();
        area.getComponents().forEach(c -> emptyArea.putComponent(c.copy()));
        DMArea retValue = new DMArea(emptyArea);
        retValue.tokens[0] = tokens[0];
        retValue.tokens[1] = tokens[1];
        return retValue;
    }

    public List<Component> getAll(Predicate<Component> filter) {
        return area.stream().filter(filter).collect(toList());
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
        return area.hashCode() + 7 * Arrays.hashCode(tokens);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DMArea) {
            DMArea other = (DMArea) obj;
            return area.equals(other.area) && Arrays.equals(tokens, other.tokens);
        }
        return false;
    }
}

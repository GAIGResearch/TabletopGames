package games.thegame.components;
import core.components.Card;

import java.util.Objects;

public class TheGameCard extends Card {

    private final int number;

    public TheGameCard(String name, int number) {
        super(name);
        this.number = number;
    }

    protected TheGameCard(String name, int number, int componentID) {
        super(name, componentID);
        this.number = number;
    }
    @Override
    public TheGameCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TheGameCard card)
            return card.number == this.number;
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }
}

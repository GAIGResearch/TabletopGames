package games.hareandtortoise.components;

import core.components.Card;

import java.util.Objects;

/**
 * A hare card. Immutable: two cards of the same type are interchangeable.
 */
public class HareCard extends Card {

    public final HareCardType type;

    public HareCard(HareCardType type) {
        super(type.text);
        this.type = type;
    }

    @Override
    public HareCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HareCard other && other.type == type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type) + 7219;
    }

    @Override
    public String toString() {
        return type.text;
    }
}

package games.hareandtortoise.components;

import core.components.Card;

import java.util.Objects;

/**
 * A hare card of the 1978 Ravensburger edition. Immutable: two cards of the same type are interchangeable.
 */
public class HareCard extends Card {

    public enum Type {
        FALL_BACK_ONE_POSITION("Fall back one position"),
        LAST_TURN_FREE("Your last turn costs nothing"),
        DRAW_OR_DISCARD_10("Either draw or discard 10 carrots"),
        LEAP_AHEAD_ONE_POSITION("Leap ahead by one position"),
        NEXT_CARROT_SQUARE("Leap ahead to the next carrot square"),
        PREVIOUS_CARROT_SQUARE("Fall back to the previous carrot square"),
        ANOTHER_TURN("Have another turn"),
        MISS_A_TURN("Miss a turn"),
        CHEW_A_LETTUCE("Chew a lettuce");

        public final String text;

        Type(String text) {
            this.text = text;
        }
    }

    public final Type type;

    public HareCard(Type type) {
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

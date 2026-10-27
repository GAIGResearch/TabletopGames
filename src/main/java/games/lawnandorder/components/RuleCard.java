package games.lawnandorder.components;

import core.components.Card;

import java.util.Objects;

/**
 * An HOA Rule card: a Standard Rule, which condemns one attribute, or a Special Rule.
 */
public class RuleCard extends Card {

    public enum Special {
        ADMINISTRATIVE_ERROR("Administrative Error"),
        EMERGENCY_SESSION("Emergency Session"),
        ZERO_TOLERANCE("Zero Tolerance Policy");

        public final String label;

        Special(String label) {
            this.label = label;
        }
    }

    /** The attribute a Standard Rule condemns, or null for a Special Rule. */
    public final LawnCard.Attribute condemned;
    /** The Special Rule, or null for a Standard Rule. */
    public final Special special;

    private RuleCard(LawnCard.Attribute condemned, Special special) {
        super(condemned != null ? "No " + condemned.label : special.label);
        this.condemned = condemned;
        this.special = special;
    }

    public static RuleCard standard(LawnCard.Attribute condemned) {
        return new RuleCard(Objects.requireNonNull(condemned), null);
    }

    public static RuleCard special(Special special) {
        return new RuleCard(null, Objects.requireNonNull(special));
    }

    @Override
    public RuleCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RuleCard that && condemned == that.condemned && special == that.special;
    }

    @Override
    public int hashCode() {
        return Objects.hash(condemned, special) + 55127;
    }

    @Override
    public String toString() {
        return componentName;
    }
}

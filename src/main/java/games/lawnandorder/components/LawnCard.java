package games.lawnandorder.components;

import core.components.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A Lawn card: one object with one attribute from each of the three categories. The 64 cards are the 64 combinations.
 */
public class LawnCard extends Card {

    /**
     * The three categories of attribute, each scored on its own track.
     */
    public enum Category {
        TYPE("Improvements"), COLOUR("Colour"), FEATURE("Character");

        /** The name of the HOA subcommittee that polices the category. */
        public final String subcommittee;

        Category(String subcommittee) {
            this.subcommittee = subcommittee;
        }
    }

    public enum Attribute {
        ORNAMENT(Category.TYPE, "Ornament"), FURNITURE(Category.TYPE, "Furniture"),
        STRUCTURE(Category.TYPE, "Structure"), WATER_FEATURE(Category.TYPE, "Water Feature"),
        RED(Category.COLOUR, "Red"), YELLOW(Category.COLOUR, "Yellow"),
        PINK(Category.COLOUR, "Pink"), BLUE(Category.COLOUR, "Blue"),
        OVERSIZED(Category.FEATURE, "Oversized"), ILLUMINATED(Category.FEATURE, "Illuminated"),
        PLASTIC(Category.FEATURE, "Plastic"), REPURPOSED(Category.FEATURE, "Repurposed");

        public final Category category;
        public final String label;

        Attribute(Category category, String label) {
            this.category = category;
            this.label = label;
        }

        /**
         * The attributes of the category, in declaration order.
         */
        public static List<Attribute> of(Category category) {
            List<Attribute> retValue = new ArrayList<>();
            for (Attribute a : values())
                if (a.category == category)
                    retValue.add(a);
            return retValue;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public final Attribute type;
    public final Attribute colour;
    public final Attribute feature;

    public LawnCard(Attribute type, Attribute colour, Attribute feature) {
        super(type.label + " / " + colour.label + " / " + feature.label);
        if (type.category != Category.TYPE || colour.category != Category.COLOUR || feature.category != Category.FEATURE)
            throw new IllegalArgumentException("Attributes in the wrong categories: " + type + ", " + colour + ", " + feature);
        this.type = type;
        this.colour = colour;
        this.feature = feature;
    }

    /**
     * The card's attribute in the category.
     */
    public Attribute get(Category category) {
        return switch (category) {
            case TYPE -> type;
            case COLOUR -> colour;
            case FEATURE -> feature;
        };
    }

    public boolean has(Attribute attribute) {
        return get(attribute.category) == attribute;
    }

    /**
     * All 64 Lawn cards.
     */
    public static List<LawnCard> allCards() {
        List<LawnCard> retValue = new ArrayList<>();
        for (Attribute t : Attribute.of(Category.TYPE))
            for (Attribute c : Attribute.of(Category.COLOUR))
                for (Attribute f : Attribute.of(Category.FEATURE))
                    retValue.add(new LawnCard(t, c, f));
        return retValue;
    }

    @Override
    public LawnCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LawnCard that && type == that.type && colour == that.colour && feature == that.feature;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, colour, feature);
    }

    @Override
    public String toString() {
        return componentName;
    }
}

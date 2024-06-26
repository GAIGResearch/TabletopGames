package core.components;

import core.CoreConstants;

import java.util.Objects;

public class FrenchCard extends Card {

    public static int[] numbers = {2,3,4,5,6,7,8,9,10};

    public enum FrenchCardType {
        Jack (11),
        Queen (12),
        King (13),
        Ace (14),
        Number (-1);

        final int number;
        FrenchCardType(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }
    }

    public enum Suite {
        Diamonds,
        Hearts,
        Clubs,
        Spades
    }

    public final Suite suite;
    public final FrenchCardType type;
    public final int number;

    public FrenchCard(FrenchCardType type, Suite suite, int number){
        super(type.toString());
        this.suite = suite;
        this.type = type;
        this.number = number;
    }

    public FrenchCard(FrenchCardType type, Suite suite){
        super(type.toString());
        this.suite = suite;
        this.type = type;
        this.number = type.number;
    }

    private FrenchCard(FrenchCardType type, Suite suite, int number, int componentID){
        super(type.toString(), componentID);
        this.suite = suite;
        this.type = type;
        this.number = number;
    }

    public static Deck<FrenchCard> generateDeck(String name, CoreConstants.VisibilityMode visibilityMode) {
        Deck<FrenchCard> deck = new Deck<>(name, visibilityMode);
        for (Suite suite : Suite.values()){
            for (FrenchCard.FrenchCardType type: FrenchCard.FrenchCardType.values()) {
                if (type == FrenchCard.FrenchCardType.Number) {
                    for (int number : numbers) {
                        deck.add(new FrenchCard(FrenchCard.FrenchCardType.Number, suite, number));
                    }
                } else {
                    deck.add(new FrenchCard(type, suite));
                }
            }
        }
        return deck;
    }

    @Override
    public FrenchCard copy() {
        return this; // immutable
    }

    @Override
    public int hashCode() {
        return Objects.hash(suite.ordinal(), type.ordinal(), number);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FrenchCard) {
            FrenchCard other = (FrenchCard) o;
            return this.suite == other.suite && this.type == other.type && this.number == other.number;
        }
        return false;
    }

    @Override
    public String toString() {
        switch (type) {
            case Number:
                return "{" + suite + " " + number + "}";
            case Queen:
            case King:
            case Jack:
            case Ace:
                return "{" + type.name() + " " + suite + "}";
        }
        return null;
    }
}
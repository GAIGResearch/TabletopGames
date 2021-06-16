package core.components;

public class FrenchCard extends Card {

    public enum FrenchCardType {
        Jack,
        Queen,
        King,
        Ace,
        Number
    }

    public final String suite;
    public final FrenchCardType type;
    public final int number;

    public FrenchCard(FrenchCardType type, String suite, int number){
        super(type.toString());
        this.suite = suite;
        this.type = type;
        this.number = number;
    }

    public FrenchCard(FrenchCardType type, String suite){
        super(type.toString());
        this.suite = suite;
        this.type = type;
        this.number = -1;
    }

    private FrenchCard(FrenchCardType type, String suite, int number, int componentID){
        super(type.toString(), componentID);
        this.suite = suite;
        this.type = type;
        this.number = number;
    }

    @Override
    public Card copy() {
        return new FrenchCard(type, suite, number, componentID);
    }

    @Override
    public String toString() {
        switch (type) {
            case Number:
                return "{" + suite + " " + number + "}";
            case Queen:
                return "{" + "Queen " + suite + "}";
            case King:
                return "{" + "King " + suite + "}";
            case Jack:
                return "{" + "Jack " + suite + "}";
            case Ace:
                return "{" + "Ace " + suite + "}";
        }
        return null;
    }
}
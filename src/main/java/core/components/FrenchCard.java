package core.components;

public class FrenchCard extends Card {

    public enum FrenchCardType {
        Jack,
        Queen,
        King,
        Ace,
        Number,
        Draw,
    }

    public final String suite;
    public final FrenchCardType type;
    public final int number;
    public final int drawN;

    public FrenchCard(FrenchCardType type, String suite, int number){
        super(type.toString());
        this.suite = suite;
        this.type = type;
        if (type == FrenchCard.FrenchCardType.Number) {
            this.drawN = number;
            this.number = -1;
        }
        else  {
            this.number = -1;
            this.drawN = -1;
        }
    }

    public FrenchCard(FrenchCardType type, String suite){
        super(type.toString());
        this.suite = suite;
        this.type = type;
        this.number = -1;
        this.drawN = -1;
    }

    public FrenchCard(FrenchCardType type, String suite, int number, int drawN){
        super(type.toString());
        this.suite = suite;
        this.type = type;
        this.number = number;
        this.drawN = drawN;
    }

    public FrenchCard(FrenchCardType type, String suite, int number, int drawN, int componentID){
        super(type.toString(), componentID);
        this.suite = suite;
        this.type = type;
        this.number = number;
        this.drawN = drawN;
    }

    @Override
    public Card copy() {
        return new FrenchCard(type, suite, number, drawN, componentID);
    }

    @Override
    public String toString() {
        switch (type) {
            case Number:
                return "{" + suite + " " + drawN + "}";
            case Draw:
                return "{" + drawN + " " + number + "}";
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
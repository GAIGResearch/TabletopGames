package core.components;

import core.components.Card;
//import games.uno.cards.UnoCard;
import core.components.Deck;
import games.poker.PokerGameState;
import games.uno.UnoGameState;
import games.uno.cards.UnoCard;
//import games.uno.cards.UnoCard;

public class FrenchCard extends Card {

    public enum FrenchCardType {
        Jack,
        Queen,
        King,
        Ace,
        Number,
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

    public boolean isPlayable(PokerGameState gameState) {
        switch (type) {
            case Number:
                return this.number == gameState.getCurrentCard().number || this.suite.equals(gameState.getCurrentSuite());
            case Jack:
            case Queen:
            case King:
            case Ace:
                return this.suite.equals(gameState.getCurrentSuite());
        }
        return false;
    }

    @Override
    public String toString() {
        switch (type) {
            case Number:
                return type + "{" + suite + " " + drawN + "}";
            case Queen:
                return type + "{" + "Queen" + suite + "}";
            case King:
                return type + "{" + "King" + suite + "}";
            case Jack:
                return type + "{" + "Jack" + suite + "}";
            case Ace:
                return type + "{" + "Ace" + suite + "}";
        }
        return null;
    }
}
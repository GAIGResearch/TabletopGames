package games.uno.cards;

import core.components.Card;
import games.uno.UnoGameState;
import core.components.Deck;

public class UnoCard extends Card {

    public enum UnoCardType {
        Number,
        Skip,
        Reverse,
        Draw,
        Wild
    }

    public final String color;
    public final UnoCardType type;
    public final int number;
    public final int drawN;

    public UnoCard(UnoCardType type, String color, int number){
        super(type.toString());
        this.color = color;
        this.type = type;
        if (type == UnoCardType.Draw) {
            this.drawN = number;
            this.number = -1;
        } else {
            this.number = number;
            this.drawN = -1;
        }
    }

    public UnoCard(UnoCardType type, String color){
        super(type.toString());
        this.color = color;
        this.type = type;
        this.number = -1;
        this.drawN = -1;
    }

    @Override
    public Card copy() {
        return new UnoCard(type, color, number);
    }

    public boolean isPlayable(UnoGameState gameState) {
        switch (type) {
            case Number:
                return this.number == gameState.getCurrentCard().number || this.color.equals(gameState.getCurrentColor());
            case Skip:
            case Reverse:
            case Draw:
                return this.color.equals(gameState.getCurrentColor());
            case Wild:
                if (this.drawN >= 1) {
                    int playerID = gameState.getCurrentPlayerID();
                    Deck<UnoCard> playerHand = gameState.getPlayerDecks().get(playerID);
                    for (UnoCard card : playerHand.getComponents()) {
                        if (card.color.equals(gameState.getCurrentColor()))
                            return false;
                    }
                }
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        switch (type) {
            case Number:
                return type + "{" + color + " " + number + "}";
            case Skip:
            case Reverse:
                return type + "{" + color + "}";
            case Draw:
                return type + "{" + drawN + " " + color + "}";
            case Wild:
                if (drawN < 1) {
                    return type.toString();
                } else {
                    return type.toString() + "{draw " + drawN + "}";
                }
        }
        return null;
    }
}

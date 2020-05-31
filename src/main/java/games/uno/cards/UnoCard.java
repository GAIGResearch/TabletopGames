package games.uno.cards;


import core.components.Card;
import games.uno.UnoGameState;

public abstract class UnoCard extends Card {

    public enum UnoCardType {
        Number,
        Skip,
        Reverse,
        DrawTwo,
        Wild,
        WildDrawFour
    }

    public final String color;
    public final UnoCardType type;
    public final int number;

    public UnoCard(String color, UnoCardType type, int number){
        super(type.toString());
        this.color = color;
        this.type = type;
        this.number = number;
    }

    public abstract UnoCard copy();
    public abstract boolean isPlayable(UnoGameState gameState);
}

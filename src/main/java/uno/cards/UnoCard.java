package uno.cards;

import uno.UnoGameState;

public abstract class UnoCard  {

    public enum UnoCardColor {
        Red,
        Blue,
        Green,
        Yellow,
        Wild
    }

    public enum UnoCardType {
        Number,
        Skip,
        Reverse,
        DrawTwo,
        Wild,
        WildDrawFour
    }

    public final UnoCardColor color;
    public final UnoCardType type;
    public final int number;

    public UnoCard(UnoCardColor color, UnoCardType type, int number){
        this.color = color;
        this.type = type;
        this.number = number;
    }

    public abstract boolean isPlayable(UnoGameState gameState);
}

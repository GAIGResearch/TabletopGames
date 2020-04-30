package updated_core.games.uno.cards;


import core.GameState;
import updated_core.actions.IAction;
import updated_core.games.uno.UnoGameState;


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

    public UnoCard(UnoCardColor color, UnoCardType type){
        this.color = color;
        this.type = type;
    }

    public abstract boolean isPlayable(UnoGameState gameState);
}

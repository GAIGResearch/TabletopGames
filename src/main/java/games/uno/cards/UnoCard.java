package games.uno.cards;


import core.components.Component;
import games.uno.UnoGameState;
import utilities.Utils;


public abstract class UnoCard extends Component {

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
        super(Utils.ComponentType.CARD, type.toString());
        this.color = color;
        this.type = type;
    }
    @Override
    public Component copy() {
        return null;
    }

    public abstract boolean isPlayable(UnoGameState gameState);
}

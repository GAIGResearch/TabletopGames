package games.sushigo.cards;

import core.components.Card;

public class SGCard extends Card {

    public enum SGCardType {
        Maki_1,
        Maki_2,
        Maki_3,
        Tempura,
        Sashimi,
        Dumpling,
        SquidNigiri,
        SalmonNigiri,
        EggNigiri,
        Wasabi,
        Chopsticks,
        Pudding
    }

    public SGCardType type;
    public int value;

    public SGCard(SGCardType type, int value)
    {
        super(type.toString());
        this.type = type;
        this.value = value;
    }

    @Override
    public Card copy() {
        return new SGCard(type, value);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}

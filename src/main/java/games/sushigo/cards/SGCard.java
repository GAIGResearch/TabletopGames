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

    public final SGCardType type;

    public SGCard(SGCardType type)
    {
        super(type.toString());
        this.type = type;
    }

    @Override
    public Card copy() {
        return this; // immutable
    }

    @Override
    public String toString() {
        return type.toString();
    }
}

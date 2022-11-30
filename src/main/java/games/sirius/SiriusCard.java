package games.sirius;

import core.components.Card;

public class SiriusCard extends Card {

    public final int value;

    public SiriusCard(String name, int value) {
        super(name);
        this.value = value;
    }

    @Override
    public Card copy(){
        return this;
        // immutable by declaration
    }

}

package games.pandemic.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;

@SuppressWarnings("unchecked")
public class Forecast extends DrawCard {

    public Forecast(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discards the card
        return super.execute(gs);
   }

    @Override
    public AbstractAction copy() {
        return new Forecast(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof Forecast;
    }

    @Override
    public String toString() {
        return "Forecast";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

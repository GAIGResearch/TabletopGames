package games.pandemic.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;

@SuppressWarnings("unchecked")
public class QuietNight extends DrawCard {

    public QuietNight(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discards the card
        return super.execute(gs);
   }

    @Override
    public AbstractAction copy() {
        return new QuietNight(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof QuietNight;
    }

    @Override
    public String toString() {
        return "QuietNight";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

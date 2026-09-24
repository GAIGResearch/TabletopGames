package games.klaverjassen.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.klaverjassen.KlaverjassenGameState;

public class ChooseTrump extends AbstractAction {

    public final FrenchCard.Suite suit;

    public ChooseTrump(FrenchCard.Suite suit) {
        this.suit = suit;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((KlaverjassenGameState) gs).setTrumpSuit(suit);
        return true;
    }

    @Override
    public ChooseTrump copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChooseTrump that && suit == that.suit;
    }

    @Override
    public int hashCode() {
        return suit.ordinal() + 481913;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Trumps " + suit;
    }
}

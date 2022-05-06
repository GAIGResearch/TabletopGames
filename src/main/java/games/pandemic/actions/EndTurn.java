package games.pandemic.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;

@SuppressWarnings("unchecked")
public class EndTurn extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
   }

    @Override
    public AbstractAction copy() {
        return new EndTurn();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof EndTurn;
    }

    @Override
    public int hashCode() {
        return 0;
    }


    @Override
    public String toString() {
        return "EndTurn";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

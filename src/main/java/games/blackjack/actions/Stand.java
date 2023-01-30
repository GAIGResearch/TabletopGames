package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.blackjack.BlackjackGameState;

import java.util.Objects;

public class Stand extends AbstractAction implements IPrintable {

    @Override
    public boolean execute(AbstractGameState gs) {
        // Nothing to do
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; //immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Stand;
    }

    @Override
    public int hashCode() {
        return 904344;
    }

    @Override
    public String getString(AbstractGameState gameState){
        return "Stand";
    }

    @Override
    public String toString() {
        return "Stand";
    }
}

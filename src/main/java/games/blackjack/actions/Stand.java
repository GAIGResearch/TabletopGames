package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;

import java.util.Objects;

public class Stand extends AbstractAction implements IPrintable {

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.getTurnOrder().endPlayerTurn(gs);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new Stand();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stand)) return false;
        Stand that = (Stand) o;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash();
    }

    @Override
    public String getString(AbstractGameState gameState){
        return "Stand";
    }

    @Override
    public String toString() {
        return "Hit";
    }
}

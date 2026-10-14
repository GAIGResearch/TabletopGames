package games.chinesecheckers.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.chinesecheckers.CCGameState;
import games.chinesecheckers.components.Peg;
import games.chinesecheckers.components.StarBoard;

import java.util.Objects;

public class MovePeg extends AbstractAction {

    final int from;
    final int to;

    public MovePeg(int from, int to)
    {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CCGameState state = (CCGameState) gs;
        StarBoard board = state.getStarBoard();
        Peg peg = board.getPeg(from);

        // a peg's destination is the nodes of its own colour (it starts on those of the opposite colour)
        boolean inDestination = peg.inDestination() || board.getBaseColour(to) == peg.colour();
        board.setPeg(from, null);
        board.setPeg(to, inDestination == peg.inDestination() ? peg : new Peg(peg.colour(), true));

        return true;
    }

    public int getFrom(){
        return from;
    }

    public int getTo(){
        return to;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MovePeg peg)) return false;
        return from == peg.from && to == peg.to;
    }

    @Override
    public int hashCode() {
        return 3 - from * 31 + 8191 * to;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Peg on Node: " + from + " move to " + "Node: " + to;
    }
}

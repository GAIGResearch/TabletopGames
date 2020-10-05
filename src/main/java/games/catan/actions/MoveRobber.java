package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;

public class MoveRobber extends AbstractAction {
    CatanTile from;
    CatanTile to;

    public MoveRobber(CatanTile from, CatanTile to){
        this.from = from;
        this.to = to;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        if (from.removeRobber()){
            to.placeRobber();
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new MoveRobber(from, to);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof MoveRobber){
            MoveRobber otherAction = (MoveRobber)other;
            return from.equals(otherAction.from) && to.equals(otherAction.to);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "MoveRobber from= " + from + " to= " + to;
    }
}

package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;

public class MoveRobber extends AbstractAction {
    CatanTile to;

    public MoveRobber(CatanTile to){
        this.to = to;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        CatanTile robberTile = cgs.getRobber(cgs.getBoard());
        if (robberTile.removeRobber()){
            to.placeRobber();
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new MoveRobber(to);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof MoveRobber){
            MoveRobber otherAction = (MoveRobber)other;
            return to.equals(otherAction.to);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "MoveRobber to " + to;
    }
}

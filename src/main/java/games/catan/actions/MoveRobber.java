package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;

public class MoveRobber extends AbstractAction {
    int x;
    int y;

    public MoveRobber(int x, int y){
        this.x = x;
        this.y = y;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        CatanTile robberTile = cgs.getRobber(cgs.getBoard());
        System.out.println("moving robber from " + robberTile.toString() + " to " + cgs.getBoard()[x][y].toString());
        if (robberTile.removeRobber()){
            cgs.getBoard()[x][y].placeRobber();
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new MoveRobber(x, y);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof MoveRobber){
            MoveRobber otherAction = (MoveRobber)other;
            return x == otherAction.x && y == otherAction.y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "MoveRobber to x=" + x + " y=" + y;
    }
}

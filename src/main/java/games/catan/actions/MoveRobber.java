package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;

import java.util.Objects;

public class MoveRobber extends AbstractAction {
    public final int x;
    public final int y;

    public MoveRobber(int x, int y){
        this.x = x;
        this.y = y;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        CatanTile robberTile = cgs.getRobber(cgs.getBoard());
        if(gs.getCoreGameParameters().verbose){
            System.out.println("moving robber from " + robberTile.toString() + " to " + cgs.getBoard()[x][y].toString());
        }

        if (robberTile.removeRobber()){
            cgs.getBoard()[x][y].placeRobber();
            return true;
        } else {
            throw new AssertionError("Cannot move robber from " + robberTile + " to " + cgs.getBoard()[x][y].toString());
        }
    }

    public int[] getXY(){
        return new int[]{x,y};
    }

    @Override
    public AbstractAction copy() {
        return this;
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
        return Objects.hash(x,y);
    }

    @Override
    public String toString() {
        return "MoveRobber to x=" + x + " y=" + y;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;
import games.catan.components.Road;

import static core.CoreConstants.VERBOSE;

// Builds the Road by reference
public class BuildRoadByRef extends AbstractAction {
    //TODO HASH,Equals,Copy,State
    Road road;

    public BuildRoadByRef(Road road){
        this.road = road;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanTile[][] board = cgs.getBoard();
        if (road.getOwner() == -1) {
            if (((Counter)cgs.getComponentActingPlayer(CatanConstants.roadCounterHash)).isMaximum()){
                if (VERBOSE)
                    System.out.println("No more roads to build for player " + gs.getCurrentPlayer());
                return false;
            }
            this.road.setOwner(cgs.getCurrentPlayer());
            ((Counter)cgs.getComponentActingPlayer(CatanConstants.roadCounterHash)).increment(1);
            return true;
        }

        return false;
    }

    @Override
    public AbstractAction copy() {
        BuildRoadByRef copy = new BuildRoadByRef(road);
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof BuildRoadByRef){
            BuildRoadByRef otherAction = (BuildRoadByRef)other;
            return road.equals(otherAction.road);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Buildroad by reference with road = " + road.toString();
    }
}

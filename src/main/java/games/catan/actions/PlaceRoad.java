package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanTile;
import games.catan.components.Road;

import static core.CoreConstants.VERBOSE;

// Places a road by reference - player is not charged resources
public class PlaceRoad extends AbstractAction {
    //TODO HASH,Equals,Copy,State
    Road road;
    Card card;

    public PlaceRoad(Road road, Card card){
        this.road = road;
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;

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
        PlaceRoad copy = new PlaceRoad(road, card);
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof PlaceRoad){
            PlaceRoad otherAction = (PlaceRoad)other;
            return road.equals(otherAction.road) && card.equals(otherAction.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Place Road with road = " + road.toString() + " and card = " + card.toString();
    }
}

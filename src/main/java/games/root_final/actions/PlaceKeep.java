package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class PlaceKeep extends AbstractAction {
    public final int locationID;
    public final int playerID;

    public PlaceKeep(int playerID, int locationID) {
        this.playerID = playerID;
        this.locationID = locationID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (gs.getCurrentPlayer() == playerID) {
            RootGameState currentState = (RootGameState) gs;
            RootParameters rp = (RootParameters) gs.getGameParameters();
            if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat) {
                currentState.getGameMap().getNodeByID(locationID).setKeep();
                currentState.increaseSubGamePhase();
                for (RootBoardNodeWithRootEdges node : currentState.getGameMap().getNonForrestBoardNodes()) {
                    if (!node.identifier.equals(rp.cornerPairs.get(currentState.getGameMap().getNodeByID(locationID).identifier))) {
                        node.addWarrior(currentState.getPlayerFaction(playerID));
                        currentState.removeCatWarrior();
                    } else {
                        try {
                            node.build(RootParameters.BuildingType.Roost);
                            currentState.removeBuilding(RootParameters.BuildingType.Roost);
                            for (int i = 0; i < 6; i++) {
                                node.addWarrior(RootParameters.Factions.EyrieDynasties);
                                currentState.removeBirdWarrior();
                            }
                        }catch (Exception e){
                            System.out.println(e.getMessage());
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new PlaceKeep(playerID, locationID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PlaceKeep other) {
            return playerID == other.playerID && locationID == other.locationID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " places keep at " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}

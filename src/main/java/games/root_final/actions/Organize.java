package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class Organize extends AbstractAction {
    public final int playerID;
    public final int locationID;

    public Organize(int playerID, int locationID){
        this.playerID = playerID;
        this.locationID = locationID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance){
            RootBoardNodeWithRootEdges location = currentState.getGameMap().getNodeByID(locationID);
            if (!location.hasToken(RootParameters.TokenType.Sympathy) && currentState.getSympathyTokens() > 0 && location.getWarrior(RootParameters.Factions.WoodlandAlliance) > 0){
                location.removeWarrior(RootParameters.Factions.WoodlandAlliance);
                currentState.addWarrior(RootParameters.Factions.WoodlandAlliance);
                location.addToken(RootParameters.TokenType.Sympathy);
                currentState.removeSympathyTokens();
                currentState.increaseActionsPlayed();
                return true;
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof Organize o){
            return playerID==o.playerID && locationID == o.locationID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Organize", playerID, locationID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID) + " removes warrior and adds sympathy at " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}

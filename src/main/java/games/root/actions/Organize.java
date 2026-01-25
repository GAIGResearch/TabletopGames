package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.RootBoardNodeWithRootEdges;

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
    public Organize copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Organize organize = (Organize) o;
        return playerID == organize.playerID && locationID == organize.locationID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " removes warrior and adds sympathy at " + locationID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID) + " removes warrior and adds sympathy at " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}

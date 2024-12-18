package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class Recruit extends AbstractAction {
    public final int locationID;
    public final int playerID;
    public final int numberOfWarriors;
    public final boolean increaseSubGamePhase;
    public final boolean playedBird;

    public Recruit(int locationID, int playerIdx, boolean increaseSubGamePhase){
        this.locationID = locationID;
        this.playerID = playerIdx;
        this.increaseSubGamePhase = increaseSubGamePhase;
        this.numberOfWarriors = 1;
        this.playedBird = false;
    }

    public Recruit(int locationID, int playerID, boolean increaseSubGamePhase, boolean playedBird){
        this.locationID = locationID;
        this.playerID = playerID;
        this.increaseSubGamePhase = increaseSubGamePhase;
        this.playedBird = playedBird;
        this.numberOfWarriors = 1;
    }

    public Recruit(int locationID, int playerID, int amount, boolean increaseSubGamePhase, boolean playedBird){
        this.locationID = locationID;
        this.playerID = playerID;
        this.numberOfWarriors = amount;
        this.increaseSubGamePhase = increaseSubGamePhase;
        this.playedBird = playedBird;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(playerID == currentState.getCurrentPlayer()){
            currentState.increaseActionsPlayed();
            if(increaseSubGamePhase){currentState.increaseSubGamePhase();}
            RootBoardNodeWithRootEdges location = currentState.getGameMap().getNodeByID(locationID);
            for(int i = 0; i< numberOfWarriors; i++) {
                if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat) {
                    if (currentState.getCatWarriors() > 0) {
                        currentState.getGameMap().getNodeByID(location.getComponentID()).addCatWarrior();
                        currentState.removeCatWarrior();
                    } else {
                        return false;
                    }
                } else if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties) {
                    if (currentState.getBirdWarriors() > 0) {
                        currentState.getGameMap().getNodeByID(location.getComponentID()).addBirdWarrior();
                        currentState.removeBirdWarrior();
                    } else {
                        return false;
                    }

                } else if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance) {
                    if (currentState.getWoodlandWarriors() > 0) {
                        currentState.getGameMap().getNodeByID(location.getComponentID()).addWoodlandWarrior();
                        currentState.removeWoodlandWarrior();
                    } else {
                        return false;
                    }

                } else if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond) {
                    if (currentState.getVagabond() > 0) {
                        currentState.getGameMap().getNodeByID(location.getComponentID()).addVagabondWarrior();
                        currentState.removeVagabondWarrior();
                    } else {
                        return false;
                    }
                }
            }
            if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties){
                if (playedBird) {currentState.addPlayedSuit(RootParameters.ClearingTypes.Bird);} else {currentState.addPlayedSuit(location.getClearingType());}
            }
            return true;
        }
        return false;
    }

    @Override
    public Recruit copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recruit recruit = (Recruit) o;
        return locationID == recruit.locationID && playerID == recruit.playerID && numberOfWarriors == recruit.numberOfWarriors && increaseSubGamePhase == recruit.increaseSubGamePhase && playedBird == recruit.playedBird;
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationID, playerID, numberOfWarriors, increaseSubGamePhase, playedBird);
    }

    @Override
    public String toString() {
        return "p" + playerID + " recruits " + numberOfWarriors + " warriors at " + locationID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootBoardNodeWithRootEdges location = gs.getGameMap().getNodeByID(locationID);
        return gs.getPlayerFaction(playerID).toString() + " recruits " + numberOfWarriors + " warriors at " + location.identifier;
    }

}

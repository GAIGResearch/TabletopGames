package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.pandemic.actions.MovePlayer;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.List;
import java.util.Objects;

public class Recruit extends AbstractAction {
    public RootBoardNodeWithRootEdges location;
    public int playerID;
    public int numberOfWarriors = 1;
    public final boolean increaseSubGamePhase;
    public boolean playedBird = false;
    public Recruit(RootBoardNodeWithRootEdges location, int playerIdx, boolean increaseSubGamePhase){
        this.location = location;
        this.playerID = playerIdx;
        this.increaseSubGamePhase = increaseSubGamePhase;
    }

    public Recruit(RootBoardNodeWithRootEdges location, int playerID, boolean increaseSubGamePhase, boolean playedBird){
        this.location = location;
        this.playerID = playerID;
        this.increaseSubGamePhase = increaseSubGamePhase;
        this.playedBird = playedBird;
    }

    public Recruit(RootBoardNodeWithRootEdges location, int playerID, int amount, boolean increaseSubGamePhase, boolean playedBird){
        this.location = location;
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
    public AbstractAction copy() {
        return new Recruit(location, playerID, numberOfWarriors, increaseSubGamePhase, playedBird);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if(obj instanceof Recruit)
        {
            Recruit other = (Recruit) obj;
            return location.getComponentID()==other.location.getComponentID() && playerID == other.playerID && numberOfWarriors == other.numberOfWarriors && increaseSubGamePhase == other.increaseSubGamePhase;

        }else return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, location, numberOfWarriors, increaseSubGamePhase);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " recruits " + numberOfWarriors + " warriors at " + location.identifier;
    }

}

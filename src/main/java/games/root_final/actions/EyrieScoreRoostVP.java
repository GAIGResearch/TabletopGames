package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;

import java.util.Objects;

public class EyrieScoreRoostVP extends AbstractAction {
    public final int playerID;
    public final int score;

    public EyrieScoreRoostVP(int playerID, int score){
        this.playerID = playerID;
        this.score = score;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties){
            currentState.addGameScorePLayer(currentState.getCurrentPlayer(),score);
            currentState.increaseSubGamePhase();
            return true;
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
        if (obj instanceof EyrieScoreRoostVP e){
            return playerID == e.playerID && score == e.score;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Roosts",playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID) + " scores " + score + " for built roosts";
    }
}

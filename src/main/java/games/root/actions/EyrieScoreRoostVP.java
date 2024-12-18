package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

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
            currentState.addGameScorePlayer(currentState.getCurrentPlayer(),score);
            currentState.increaseSubGamePhase();
            return true;
        }
        return false;
    }

    @Override
    public EyrieScoreRoostVP copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EyrieScoreRoostVP that = (EyrieScoreRoostVP) o;
        return playerID == that.playerID && score == that.score;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, score);
    }

    @Override
    public String toString() {
        return "p" + playerID + " scores " + score + " for built roosts";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID) + " scores " + score + " for built roosts";
    }
}

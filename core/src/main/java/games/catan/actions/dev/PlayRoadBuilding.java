package games.catan.actions.dev;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.Arrays;
import java.util.Objects;

public class PlayRoadBuilding extends AbstractAction {
    public final int playerID;
    final AbstractAction[] roadsToBuild;

    public PlayRoadBuilding(int playerID, AbstractAction[] roadsToBuild) {
        this.playerID = playerID;
        this.roadsToBuild = roadsToBuild;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        for (AbstractAction br: roadsToBuild) {
            br.execute(gs);
        }
        return true;
    }

    @Override
    public PlayRoadBuilding copy() {
        AbstractAction[] cp = new AbstractAction[roadsToBuild.length];
        for (int i = 0; i < roadsToBuild.length; i++) {
            cp[i] = roadsToBuild[i].copy();
        }
        PlayRoadBuilding copy = new PlayRoadBuilding(playerID, cp);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayRoadBuilding)) return false;
        PlayRoadBuilding that = (PlayRoadBuilding) o;
        return playerID == that.playerID && Arrays.equals(roadsToBuild, that.roadsToBuild);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(playerID);
        result = 31 * result + Arrays.hashCode(roadsToBuild);
        return result;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "p" + playerID + " plays Dev:RoadBuilding (" + Arrays.toString(roadsToBuild) + ")";
    }
}

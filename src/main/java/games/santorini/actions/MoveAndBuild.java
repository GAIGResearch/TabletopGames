package games.santorini.actions;

import com.sun.deploy.net.proxy.BrowserProxyConfig;
import core.AbstractGameState;
import core.actions.AbstractAction;
import games.santorini.SantoriniGameState;
import games.santorini.components.SantoriniCell;

import java.util.Objects;

public class MoveAndBuild extends AbstractAction {

    int rowPlayer;
    int colPlayer;
    int rowMove;
    int colMove;
    int rowBuild;
    int colBuild;

    public MoveAndBuild(int rowPlayer, int colPlayer, int rowMove, int colMove, int rowBuild, int colBuild)
    {
        this.rowPlayer = rowPlayer;
        this.colPlayer = colPlayer;
        this.rowMove   = rowMove;
        this.colMove   = colMove;
        this.rowBuild  = rowBuild;
        this.colBuild  = colBuild;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SantoriniGameState sgs = (SantoriniGameState) gs;

        int playerID = sgs.getCurrentPlayer();

        // Remove player from actual cell
        SantoriniCell sc = sgs.grid.getElement(rowPlayer, colPlayer);
        sc.setNonPlayerIn();

        // Move player to destination cell
        SantoriniCell scPlayer = sgs.grid.getElement(rowMove, colMove);
        scPlayer.setPlayerIn(playerID);

        // Build
        SantoriniCell scBuilding = sgs.grid.getElement(rowMove, colMove);
        scBuilding.setLevel(scBuilding.getLevel() + 1);

        return true;
    }

    @Override
    public AbstractAction copy() {
       return new MoveAndBuild(rowPlayer, colPlayer, rowMove, colMove, rowBuild, colBuild);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MoveAndBuild that = (MoveAndBuild) o;
        return that.rowPlayer == rowPlayer &&
               that.colPlayer == colPlayer &&
               that.rowMove   == rowMove &&
               that.colMove   == colMove &&
               that.rowBuild  == rowBuild &&
               that.colBuild  == colBuild;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player in [" + rowPlayer + ", " + colPlayer + "], move to [" +
                rowMove + ", " + colMove + "] and build in [" +
                rowBuild + ", " + colBuild + "]";
    }
}

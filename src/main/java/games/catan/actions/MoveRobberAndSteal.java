package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanTile;

import java.util.Objects;
import java.util.Random;

import static core.CoreConstants.DefaultGamePhase.Main;

public class MoveRobberAndSteal extends AbstractAction {
    public final int x;
    public final int y;
    public final int player, targetPlayer;

    public MoveRobberAndSteal(int x, int y, int player, int targetPlayer){
        this.x = x;
        this.y = y;
        this.player = player;
        this.targetPlayer = targetPlayer;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        CatanTile robberTile = cgs.getRobber(cgs.getBoard());
        if(gs.getCoreGameParameters().verbose){
            System.out.println("moving robber from " + robberTile.toString() + " to " + cgs.getBoard()[x][y].toString());
        }

        if (robberTile.removeRobber()){
            cgs.getBoard()[x][y].placeRobber();

            Random random = new Random(gs.getGameParameters().getRandomSeed());
            int nResTarget = cgs.getNResourcesInHand(targetPlayer);
            if (nResTarget == 0){
                return false;
            }
            int cardIndex = random.nextInt(nResTarget);
            CatanParameters.Resource resource = cgs.pickResourceFromHand(targetPlayer, cardIndex);
            cgs.getPlayerResources(player).get(resource).increment();
            cgs.getPlayerResources(targetPlayer).get(resource).decrement();

            cgs.setGamePhase(Main);
            return true;
        } else {
            throw new AssertionError("Cannot move robber from " + robberTile + " to " + cgs.getBoard()[x][y].toString());
        }
    }

    @Override
    public MoveRobberAndSteal copy() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof MoveRobberAndSteal){
            MoveRobberAndSteal otherAction = (MoveRobberAndSteal)other;
            return x == otherAction.x && y == otherAction.y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x,y);
    }

    @Override
    public String toString() {
        return "MoveRobber to x=" + x + " y=" + y;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

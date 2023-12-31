package games.catan.actions.robber;

import core.AbstractGameState;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanTile;

import java.util.Objects;
import java.util.Random;

import static core.CoreConstants.DefaultGamePhase.Main;

public class MoveRobberAndSteal extends MoveRobber {
    public final int targetPlayer;  // Player to steal from random resource

    public MoveRobberAndSteal(int x, int y, int player, int targetPlayer){
        super(x, y, player);
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

            Random random = gs.getRnd();
            if (targetPlayer != -1) {
                // We might not have anyone to steal from, that's ok
                int nResTarget = cgs.getNResourcesInHand(targetPlayer);
                if (nResTarget == 0) {
                    cgs.setGamePhase(Main);
                    return false;
                }
                int cardIndex = random.nextInt(nResTarget);
                CatanParameters.Resource resource = cgs.pickResourceFromHand(targetPlayer, cardIndex);
                cgs.getPlayerResources(player).get(resource).increment();
                cgs.getPlayerResources(targetPlayer).get(resource).decrement();
            }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoveRobberAndSteal)) return false;
        if (!super.equals(o)) return false;
        MoveRobberAndSteal that = (MoveRobberAndSteal) o;
        return targetPlayer == that.targetPlayer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetPlayer);
    }

    @Override
    public String toString() {
        return "p" + player + " moves robber to (" + x + ";" + y + "). Steals from p" + targetPlayer;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

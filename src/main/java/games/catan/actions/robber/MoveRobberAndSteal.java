package games.catan.actions.robber;

import core.AbstractGameState;
import core.components.grid.HexCoord;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanTile;

import java.util.Objects;
import java.util.Random;

import static core.CoreConstants.DefaultGamePhase.Main;

public class MoveRobberAndSteal extends MoveRobber {
    public final int targetPlayer;  // Player to steal from random resource

    public MoveRobberAndSteal(HexCoord hex, int player, int targetPlayer){
        super(hex, player);
        this.targetPlayer = targetPlayer;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        CatanTile robberTile = cgs.getRobber();
        if(gs.getCoreGameParameters().verbose){
            System.out.println("moving robber from " + robberTile.toString() + " to " + cgs.getTile(hex).toString());
        }

        cgs.setRobber(hex);

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
        return "p" + player + " moves robber to " + hex + ". Steals from p" + targetPlayer;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

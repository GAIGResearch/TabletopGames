package games.catan.actions.robber;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.interfaces.IExtendedSequence;
import games.catan.CatanGameState;
import games.catan.components.Building;
import games.catan.components.CatanTile;

import java.util.*;

import static core.CoreConstants.DefaultGamePhase.Main;

public class MoveRobber extends AbstractAction implements IExtendedSequence {
    public final int x;
    public final int y;
    public final int player;

    boolean executed;

    public MoveRobber(int x, int y, int player){
        this.x = x;
        this.y = y;
        this.player = player;
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
        } else {
            throw new AssertionError("Cannot move robber from " + robberTile + " to " + cgs.getBoard()[x][y].toString());
        }

        if (_computeAvailableActions(gs).size() > 0) {
            // it may be that we have nobody to steal from, don't even try
            gs.setActionInProgress(this);
        } else {
            executed = true;
        }
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return _computeAvailableActions(state, state.getCoreGameParameters().actionSpace);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state, ActionSpace actionSpace) {
        List<AbstractAction> actions = new ArrayList<>();
        CatanGameState gs = (CatanGameState) state;
        CatanTile[][] board = gs.getBoard();
        CatanTile tile = board[x][y];
        Set<Integer> targets = new LinkedHashSet<>();
        Building[] settlements = gs.getBuildings(tile);
        for (Building settlement : settlements) {
            if (settlement.getOwnerId() != -1 && settlement.getOwnerId() != gs.getCurrentPlayer()) {
                targets.add(settlement.getOwnerId());
            }
        }
        for (int target : targets) {
            actions.add(new StealResource(player, target));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
        state.setGamePhase(Main);  // If there were no targets to steal from, then this is not set in StealResource action, need it to stop looping
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public MoveRobber copy() {
        MoveRobber mr = new MoveRobber(x, y, player);
        mr.executed = executed;
        return mr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoveRobber)) return false;
        MoveRobber that = (MoveRobber) o;
        return x == that.x && y == that.y && player == that.player && executed == that.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, player, executed);
    }

    @Override
    public String toString() {
        return "p" + player + " moves robber to x=" + x + " y=" + y;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

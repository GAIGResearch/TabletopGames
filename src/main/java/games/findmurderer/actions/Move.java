package games.findmurderer.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.findmurderer.MurderGameState;
import games.findmurderer.MurderParameters;
import games.findmurderer.components.Person;
import utilities.Vector2D;

import java.util.HashSet;
import java.util.Objects;

/* Person action to move. Applied after each action by either detective or killer */

public class Move extends AbstractAction {
    final int whoID;
    final Vector2D fromPos;
    public final Vector2D toPos;

    public Move(int whoID, Vector2D fromPos, Vector2D toPos) {
        this.whoID = whoID;
        this.fromPos = fromPos;
        this.toPos = toPos;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MurderGameState mgs = (MurderGameState) gs;
        MurderParameters mp = (MurderParameters)mgs.getGameParameters();
        Person p = mgs.getGrid().getElement(fromPos.getX(), fromPos.getY());
        mgs.getGrid().setElement(fromPos.getX(), fromPos.getY(), null);
        mgs.getGrid().setElement(toPos.getX(), toPos.getY(), p);

        // Update interaction history
        // Find all neighbours of person at this position
        HashSet<Integer> neighbours = new HashSet<>();
        for (MurderParameters.Direction d: MurderParameters.Direction.values()) {
            Vector2D targetPos = new Vector2D(toPos.getX() + d.xDiff, toPos.getY() + d.yDiff);
            // Cannot go off the grid
            if (targetPos.getX() >= 0 && targetPos.getX() < mgs.getGrid().getWidth()
                    && targetPos.getY() >= 0 && targetPos.getY() < mgs.getGrid().getHeight()) {
                Person other = mgs.getGrid().getElement(targetPos.getX(), targetPos.getY());
                if (other != null) neighbours.add(other.getComponentID());
            }
        }
        // Add neighbours to interaction history for this round
        if (neighbours.size() > 0) {
            int round = gs.getTurnOrder().getRoundCounter();
            if (!p.interactionHistory.containsKey(round)) p.interactionHistory.put(round, new HashSet<>());
            p.interactionHistory.get(round).addAll(neighbours);
        }

        // If this person is in the detective's vision range, update detective information too in game state
        if (mp.distanceFunction.apply(toPos, mgs.getDetectiveFocus()) <= mp.detectiveVisionRange) {
            // TODO: inaccurate, this reveals too much information (e.g. if someone comes into vision range that the detective knows nothing about, all interaction history so far will be revealed but the detective shouldn't know that)
//            mgs.getDetectiveInformation().put(whoID, round);
        }

        mgs.getPersonToPositionMap().put(p.getComponentID(), toPos);

        return true;
    }

    @Override
    public Move copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move move = (Move) o;
        return whoID == move.whoID && Objects.equals(fromPos, move.fromPos) && Objects.equals(toPos, move.toPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(whoID, fromPos, toPos);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
//        MurderParameters.Direction d = MurderParameters.Direction.getDirection(toPos.getX() - fromPos.getX(), toPos.getY() - fromPos.getY());
//        if (d != null) {
//            return "Move " + whoID + " " + d.name().toLowerCase();
//        } else {
//            return "Move " + whoID + " to " + toPos;
//        }
        return "Move";
    }
}

package games.cantstop.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cantstop.CantStopGamePhase;
import games.cantstop.CantStopGameState;
import games.cantstop.CantStopParameters;

import java.util.*;

import static java.util.stream.Collectors.*;

public class AllocateDice extends AbstractAction {

    final int[] numberSplit;

    public AllocateDice(int... values) {
        numberSplit = values.clone();
    }

    public boolean isLegal(CantStopGameState state) {
        // this is legal if all the values can be used. Hence these must not be complete.
        CantStopParameters params = (CantStopParameters) state.getGameParameters();
        boolean retValue = true;
        // technically 7+7 is not valid if we have moved the 7 marker, and it is only one away from the top
        // in this case what will happen in the forward model is that we'll reduce this just 7.
        Map<Integer, Long> numberCounts = Arrays.stream(numberSplit).boxed().collect(groupingBy(n -> n, counting()));
        for (int n : numberSplit) {
            int markerPosition = Math.max(state.getTemporaryMarkerPosition(n), state.getMarkerPosition(n, state.getCurrentPlayer()));
            boolean canMoveOnTrack = !state.isTrackComplete(n) && (markerPosition + numberCounts.get(n) - 1) < params.maxValue(n);
            retValue = canMoveOnTrack && retValue;
        }
        // then each number must either have a marker already, or a spare marker is available
        if (retValue) {
            int moveableMarkers = params.MARKERS - state.getMarkersMoved().size();
            for (int n : numberSplit) {
                if (state.getTemporaryMarkerPosition(n) > 0)
                    moveableMarkers++; // we have a moveable marker on this one
            }
            retValue = moveableMarkers >= numberCounts.size();
        }
        return retValue;
    }

    public int[] getValues() {
        return numberSplit.clone();
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CantStopGameState state = (CantStopGameState) gs;
        for (int n : numberSplit)
            state.moveMarker(n);
        state.setGamePhase(CantStopGamePhase.Decision);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AllocateDice) {
            AllocateDice other = (AllocateDice) obj;
            if (numberSplit.length == other.numberSplit.length) {
                if (numberSplit.length == 1) {
                    return numberSplit[0] == other.numberSplit[0];
                } else if (numberSplit.length == 2) {
                    return (numberSplit[0] == other.numberSplit[0] && numberSplit[1] == other.numberSplit[1]) ||
                            (numberSplit[0] == other.numberSplit[1] && numberSplit[1] == other.numberSplit[0]);
                    // for equals and hashcode, we only care about the two numbers - not the precise permutation
                } else {
                    throw new AssertionError("Not yet implemented for variants with three or more dice!");
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (numberSplit.length == 1)
            return -31 + numberSplit[0] + 56;
        if (numberSplit[0] < numberSplit[1])
            return -31 * numberSplit[0] - 255 * numberSplit[1];
        return -31 * numberSplit[1] - 255 * numberSplit[0];
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Use dice : " + Arrays.toString(numberSplit);

    }
}

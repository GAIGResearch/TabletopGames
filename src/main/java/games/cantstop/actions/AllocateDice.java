package games.cantstop.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cantstop.CantStopGamePhase;
import games.cantstop.CantStopGameState;
import games.cantstop.CantStopParameters;

public class AllocateDice extends AbstractAction {

    final int[] numberSplit;

    public AllocateDice(int... values) {
        numberSplit = values.clone();
    }

    public boolean isLegal(CantStopGameState state) {
        // this is legal as long as at least one of the tracks is not yet completed
        // argh - actually more complicated. We actually need all of the tracks
        boolean retValue = false;
        for (int n : numberSplit) {
            retValue = !state.trackComplete(n) || retValue;
        }
        // and as long as we have a spare marker to move, OR at least one of the numbers can be moved
        if (retValue) {
            CantStopParameters params = (CantStopParameters) state.getGameParameters();
            boolean moveableMarker = false;
            for (int n : numberSplit) {
                moveableMarker = moveableMarker || state.getTemporaryMarkerPosition(n) > 0 &&
                        state.getTemporaryMarkerPosition(n) < params.maxValue(n);
            }
            boolean unmovedMarkers = state.getMarkersMoved().size() < params.MARKERS;
            retValue = unmovedMarkers || moveableMarker;
        }
        return retValue;
    }

    public int[] getValues() {
        return numberSplit.clone();
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CantStopGameState state = (CantStopGameState) gs;
        if (state.trackComplete(numberSplit[0]) && state.trackComplete(numberSplit[1]))
            throw new AssertionError("At least one of the two moves needs to be valid for " + this);
        state.moveMarker(numberSplit[0]);
        state.moveMarker(numberSplit[1]);
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
            return (numberSplit[0] == other.numberSplit[0] && numberSplit[1] == other.numberSplit[1]) ||
                    (numberSplit[0] == other.numberSplit[1] && numberSplit[1] == other.numberSplit[0]);
            // for equals and hashcode, we only are about the two numbers - not the precise permutation
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (numberSplit[0] < numberSplit[1])
            return -31 * numberSplit[0] - 255 * numberSplit[1];
        return -31 * numberSplit[1] - 255 * numberSplit[0];
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use pairs of dice : " + numberSplit[0] + " - " + numberSplit[1];
    }
}

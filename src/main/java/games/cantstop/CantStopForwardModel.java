package games.cantstop;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Dice;
import games.cantstop.actions.*;
import utilities.Utils;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class CantStopForwardModel extends AbstractForwardModel {

    private final Pass passAction = new Pass(false);
    private final RollDice rollAction = new RollDice();
    private final Pass bust = new Pass(true);

    @Override
    protected void _setup(AbstractGameState firstState) {
        // everything is reset in CantStopGameState._reset();
        // nothing extra is required here - except to set the Phase
        firstState.setGamePhase(CantStopGamePhase.Decision);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        if (action instanceof Pass) {
            CantStopGameState state = (CantStopGameState) currentState;
            // this is the trigger to move on to the next player
            if (((Pass) action).bust) {
                // first we remove all the temporary progress...
                state.temporaryMarkerPositions = new HashMap<>(); // easy!
            } else {
                /// ... or we make the temporary progress permanent
                CantStopParameters params = (CantStopParameters) state.getGameParameters();

                int playerId = state.getCurrentPlayer();
                for (Integer trackNumber : state.temporaryMarkerPositions.keySet()) {
                    int maxValue = params.maxValue(trackNumber);
                    int newValue = Math.min(maxValue, state.temporaryMarkerPositions.get(trackNumber));
                    state.playerMarkerPositions.get(playerId)[trackNumber] = newValue;
                    if (newValue == maxValue) {
                        state.completedColumns[trackNumber] = true;
                        // and then check game end condition
                        if (state.getGameScore(playerId) >= params.COLUMNS_TO_WIN) {
                            for (int p = 0; p < state.getNPlayers(); p++)
                                state.setPlayerResult(p == playerId ? Utils.GameResult.WIN : Utils.GameResult.LOSE, playerId);
                        }
                    }
                }
            }
            // then we pass to the next player
            if (state.isNotTerminal())
                state.getTurnOrder().endPlayerTurn(state);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        CantStopGameState state = (CantStopGameState) gameState;
        CantStopGamePhase phase = (CantStopGamePhase) state.getGamePhase();
        List<AbstractAction> retValue = new ArrayList<>();
        switch (phase) {
            case Decision:
                // We just have two actions here
                retValue.add(passAction);
                retValue.add(rollAction);
                break;
            case Allocation:
                List<Integer> values = state.dice.stream().map(Dice::getValue).collect(toList());
                // we now need all combinations of values
                // there are only three combinations - so we enumerate these, and then filter out the illegal ones
                List<AllocateDice> temp = new ArrayList<>();
                temp.add(new AllocateDice(values.get(0) + values.get(1), values.get(2) + values.get(3)));
                temp.add(new AllocateDice(values.get(0) + values.get(2), values.get(1) + values.get(3)));
                temp.add(new AllocateDice(values.get(0) + values.get(3), values.get(1) + values.get(2)));
                retValue.addAll(temp.stream().filter(ad -> ad.isLegal(state)).collect(toList()));
                if (retValue.isEmpty()) {
                    // in this case we have gone bust - not really a decision, but lets the player know
                    retValue.add(bust);
                }
            default:
                throw new AssertionError("Unknown phase " + phase);
        }
        return retValue;
    }

    @Override
    protected CantStopForwardModel _copy() {
        return this;
    }
}

package games.cantstop;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.StandardForwardModelWithTurnOrder;
import core.actions.AbstractAction;
import core.components.Dice;
import core.forwardModels.SequentialActionForwardModel;
import games.cantstop.actions.Pass;
import games.cantstop.actions.RollDice;
import games.cantstop.actions.AllocateDice;

import java.util.*;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

public class CantStopForwardModel extends StandardForwardModel {

    private final Pass passAction = new Pass(false);
    private final RollDice rollAction = new RollDice();
    private final Pass bust = new Pass(true);

    @Override
    protected void _setup(AbstractGameState firstState) {
        // everything is reset in CantStopGameState._reset();
        // nothing extra is required here - except to set the Phase
        CantStopGameState state = (CantStopGameState) firstState;
        CantStopParameters params = (CantStopParameters) state.getGameParameters();
        state.completedColumns = new boolean[13];
        state.playerMarkerPositions = new int[state.getNPlayers()][13];
        state.temporaryMarkerPositions = new HashMap<>();
        state.dice = new ArrayList<>();
        for (int i = 0; i < params.DICE_NUMBER; i++) {
            state.dice.add(new Dice(params.DICE_SIDES));
        }
        firstState.setGamePhase(CantStopGamePhase.Decision);
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        if (action instanceof Pass) {
            Pass pass = (Pass) action;
            CantStopGameState state = (CantStopGameState) currentState;
            // this is the trigger to move on to the next player
            if (!pass.bust)
                makeTemporaryMarkersPermanentAndClear(state);
            else
                state.temporaryMarkerPositions = new HashMap<>();
            // then we clear temp markers and pass to the next player

            if (state.isNotTerminal()) {
                endPlayerTurn(state);
                state.setGamePhase(CantStopGamePhase.Decision);
            }
        }
        // Until a player explicitly passes, it is still their turn
    }

    public void makeTemporaryMarkersPermanentAndClear(CantStopGameState state) {
        CantStopParameters params = (CantStopParameters) state.getGameParameters();

        int playerId = state.getCurrentPlayer();
        for (Integer trackNumber : state.temporaryMarkerPositions.keySet()) {
            int maxValue = params.maxValue(trackNumber);
            int newValue = state.temporaryMarkerPositions.get(trackNumber);
            state.playerMarkerPositions[playerId][trackNumber] = newValue;
            if (newValue == maxValue) {
                state.completedColumns[trackNumber] = true;
                // and then check game end condition
                if (state.getGameScore(playerId) >= params.COLUMNS_TO_WIN) {
                    endGame(state);
                }
            }
        }
        state.temporaryMarkerPositions = new HashMap<>();
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
                Map<Boolean, List<AllocateDice>> legalSplit = temp.stream().distinct().collect(partitioningBy(ad -> ad.isLegal(state)));
                retValue.addAll(legalSplit.getOrDefault(true, Collections.emptyList())); // legal actions go in directly
                // then we splitup the legal actions, as we can use just one of the numbers
                List<AllocateDice> legalSingleNumbers = legalSplit.getOrDefault(false, Collections.emptyList()).stream()
                        .flatMapToInt(a -> Arrays.stream(a.getValues()))
                        .mapToObj(AllocateDice::new)
                        .distinct()
                        .filter(a -> a.isLegal(state))
                        .collect(toList());
                retValue.addAll(legalSingleNumbers);
                if (retValue.isEmpty()) {
                    // in this case we have gone bust - not really a decision, but lets the player know
                    retValue.add(bust);
                }
                break;
            default:
                throw new AssertionError("Unknown phase " + phase);
        }
        return retValue;
    }
}

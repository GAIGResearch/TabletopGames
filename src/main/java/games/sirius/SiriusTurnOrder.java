package games.sirius;

import core.AbstractGameState;
import core.CoreConstants;
import core.turnorders.*;
import games.sirius.SiriusConstants.SiriusPhase;

import java.util.Arrays;

import static games.sirius.SiriusConstants.MoonType.MINING;

public class SiriusTurnOrder extends TurnOrder {

    int[] nextPlayer;
    // We have different rules for different phases
    // During move selection we are formally simultaneous  (moveSelected)
    // While for everything else we go strictly in order of current ranking (nextPlayer)

    public SiriusTurnOrder(int nPlayers) {
        super(nPlayers);
        nextPlayer = new int[nPlayers];
    }

    @Override
    public int nextPlayer(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        switch (phase) {
            case Move:
                // In this case move selection is simultaneous
                // so the next player is the first one who has not selected a move (and is not us)
                for (int i = 0; i < state.moveSelected.length; i++)
                    if (i != turnOwner && state.moveSelected[i] == -1) return i;
                // else we change phase
                return 0; // we don't actually know yet...
            case Draw:
                return nextPlayer[getCurrentPlayer(state)];
            default:
                throw new AssertionError("Unknown Phase " + phase);
        }
    }

    @Override
    public void endPlayerTurn(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.TURN_OVER, state, null));
        state.getPlayerTimer()[getCurrentPlayer(state)].incrementTurn();

        switch (phase) {
            case Move:
                if (state.allMovesSelected()) {
                    applyMovesAndSetTurnOrder(state);
                    state.setGamePhase(SiriusPhase.Draw);
                }
                break;
            case Draw:
                if (allActionsComplete()) {
                    endRound(state);
                    Arrays.fill(state.moveSelected, -1);
                    state.setGamePhase(SiriusPhase.Move);
                }
                break;
        }
        turnCounter++;
        int oldTurnOwner = turnOwner;
        turnOwner = nextPlayer(state);
        nextPlayer[oldTurnOwner] = -1;
    }

    protected boolean allActionsComplete() {
        return nextPlayer[turnOwner] == -1;  // the current player is last to move
    }

    protected void applyMovesAndSetTurnOrder(SiriusGameState state) {
        state.applyChosenMoves();
        // TODO: Replace placeholder that takes actions in simple order
        Arrays.setAll(nextPlayer, i -> i == nextPlayer.length - 1 ? 0 : i + 1);
    }

    // for unit-testing
    public int[] getNextPlayer() {
        return nextPlayer.clone();
    }

    @Override
    public void endRound(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusParameters params = (SiriusParameters) state.getGameParameters();
        state.getPlayerTimer()[getCurrentPlayer(state)].incrementRound();
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ROUND_OVER, state, null));

        // add cards
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == MINING) {
                int drawLimit = moon.getDeckSize() == 0 ? params.cardsPerEmptyMoon : params.cardsPerNonEmptyMoon;
                for (int i = 0; i < drawLimit; i++) {
                    moon.addCard((SiriusCard) state.ammoniaDeck.draw());
                }
            }
        }
        roundCounter++;
    }


    @Override
    protected void _reset() {

    }

    @Override
    protected TurnOrder _copy() {
        SiriusTurnOrder retValue = new SiriusTurnOrder(nPlayers);
        retValue.nextPlayer = nextPlayer.clone();
        return retValue;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * Arrays.hashCode(nextPlayer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SiriusTurnOrder) {
            SiriusTurnOrder other = (SiriusTurnOrder) obj;
            return Arrays.equals(nextPlayer, other.nextPlayer)
                    && super.equals(obj);
        }
        return false;
    }
}

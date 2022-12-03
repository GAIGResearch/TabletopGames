package games.sirius;

import core.AbstractGameState;
import core.CoreConstants;
import core.turnorders.TurnOrder;
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
        Arrays.setAll(nextPlayer, i -> i == nextPlayer.length - 1 ? -1 : i + 1);
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
                return getPlayerAtRank(1);
            case Draw:
                // if there is no next player (i.e. -1), then the next is 0 for Move
                int defaultNext = nextPlayer[getCurrentPlayer(state)];
                return defaultNext > -1 ? defaultNext : 0;
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
                    state.applyChosenMoves();
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
        turnOwner = nextPlayer(state);
    }

    protected boolean allActionsComplete() {
        return nextPlayer[turnOwner] == -1;  // the current player is last to move
    }

    // returns the current rank of the player for determining move order (1 is first, and so on)
    public int getRank(int player) {
        int count = -1;
        int nxt = player;
        do {
            nxt = nextPlayer[nxt];
            count++;
        } while (nxt > -1);
        return nPlayers - count;
    }

    public int getPlayerAtRank(int rank) {
        // this is not very efficient - but it is only a rarely needed function call
        // so current design decision is not to add to the state information
        for (int p = 0; p < nPlayers; p++) {
            int r = getRank(p);
            if (r == rank) return p;
        }
        throw new AssertionError("Should not be reachable");
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
                    if (state.ammoniaDeck.getSize() > 0)
                        moon.addCard((SiriusCard) state.ammoniaDeck.draw());
                }
            }
        }
        // move first rank player to last
        int pFirst = getPlayerAtRank(1);
        int pLast = getPlayerAtRank(nPlayers);
        nextPlayer[pFirst] = -1;
        nextPlayer[pLast] = pFirst;
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

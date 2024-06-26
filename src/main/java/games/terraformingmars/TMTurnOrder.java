package games.terraformingmars;

import core.AbstractGameState;
import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;
import evaluation.metrics.Event;
import games.terraformingmars.actions.TMAction;

import java.util.Arrays;
import java.util.Objects;

import static core.CoreConstants.GameResult.GAME_ONGOING;

public class TMTurnOrder extends AlternatingTurnOrder {
    final int nActionsPerPlayer;

    int nActionsTaken, nPassed;
    boolean[] passed;

    public TMTurnOrder(int nPlayers, int nActionsPerPlayer) {
        super(nPlayers);
        this.nActionsPerPlayer = nActionsPerPlayer;
        this.nActionsTaken = 0;
        this.nPassed = 0;
        this.passed = new boolean[nPlayers];
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        // Called after every main action execution in TM

        if (gameState.getGameStatus() != GAME_ONGOING) return;

        listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, gameState, turnOwner)));

        if (nActionsTaken == nActionsPerPlayer || passed[turnOwner]) {
            nActionsTaken = 0;
            turnCounter++;
            moveToNextPlayer(gameState, nextPlayer(gameState));
//            if (nPassed == nPlayers) endRound(gameState);
//            else {
//            }
        }
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        int next = (nPlayers + turnOwner + direction) % nPlayers;
        if (nPassed < nPlayers) {  // Still one player has not passed, so keep going
            while (passed[next]) {
                next = (nPlayers + next + direction) % nPlayers;
            }
        }
        return next;
    }

    @Override
    public void _startRound(AbstractGameState gameState) {
        Arrays.fill(this.passed, false);
        nPassed = 0;
    }

    public void registerActionTaken(TMGameState gameState, TMAction action, int player) {
        if (player == turnOwner) {
            nActionsTaken++;
            if (action.pass && nActionsTaken == 1) {
                // First action is pass, player is out
                passed[turnOwner] = true;
                nPassed++;
            }
            endPlayerTurn(gameState);
        }
    }

    @Override
    protected TurnOrder _copy() {
        TMTurnOrder to = new TMTurnOrder(nPlayers, nActionsPerPlayer);
        to.nActionsTaken = nActionsTaken;
        to.nPassed = nPassed;
        to.passed = passed.clone();
        to.direction = direction;
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMTurnOrder)) return false;
        if (!super.equals(o)) return false;
        TMTurnOrder that = (TMTurnOrder) o;
        return nActionsPerPlayer == that.nActionsPerPlayer &&
                nActionsTaken == that.nActionsTaken &&
                nPassed == that.nPassed &&
                Arrays.equals(passed, that.passed);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), nActionsPerPlayer, nActionsTaken, nPassed);
        result = 31 * result + Arrays.hashCode(passed);
        return result;
    }
}

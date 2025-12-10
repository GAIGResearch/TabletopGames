package games.pickomino;

import core.actions.AbstractAction;
import games.pickomino.actions.NullTurn;
import games.pickomino.actions.SelectDicesAction;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class PickominoForwardModelTest {

    private PickominoForwardModel forwardModel;
    private PickominoGameState gameState;
    private PickominoParameters parameters;

    // @BeforeEach
    public void setUp() {
        parameters = new PickominoParameters();
        gameState = new PickominoGameState(parameters, 2);
        forwardModel = new PickominoForwardModel();
        forwardModel.setup(gameState);
    }

    @Test
    public void setupInitialisesTilesAndDice() {
        setUp();
        assertEquals(parameters.maxTileValue - parameters.minTileValue + 1, gameState.remainingTiles.getSize());
        assertEquals(2, gameState.playerTiles.size());
        assertEquals(0, gameState.playerTiles.get(0).getSize());
        assertEquals(0, gameState.playerTiles.get(1).getSize());
        assertEquals(parameters.numberOfDices, gameState.remainingDices);
        assertEquals(0, gameState.totalDicesValue);
        assertEquals(0, Arrays.stream(gameState.assignedDices).sum());
        assertEquals(parameters.numberOfDices, Arrays.stream(gameState.currentRoll).sum());
    }

    @Test
    public void stopActionAvailableWhenThresholdReached() {
        setUp();
        // Ensure remaining tiles deck has tiles (setup should have created them)
        assertTrue("Remaining tiles deck should not be empty", gameState.remainingTiles.getSize() > 0);
        
        Arrays.fill(gameState.assignedDices, 0);
        Arrays.fill(gameState.currentRoll, 0);
        gameState.totalDicesValue = 16; // selecting worms (6) adds 5 each, giving 26 in total
        gameState.currentRoll[5] = 2; // two worms rolled
        gameState.currentRoll[0] = 1; // one more die to allow continue action
        gameState.remainingDices = 3; // should match the sum of currentRoll

        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);

        assertTrue("Stop action should be available when threshold is reached", 
                   actions.contains(new SelectDicesAction(6, true)));
        assertTrue("Continue action should be available when remainingDices > currentRoll count", 
                   actions.contains(new SelectDicesAction(6, false)));
    }

    @Test
    public void stopActionStealsMatchingTileFromOpponent() {
        setUp();
        int stealingPlayer = gameState.getCurrentPlayer();
        int otherPlayer = 1 - stealingPlayer;
        PickominoTile tile = new PickominoTile("Tile 21 (Score: 1)", 21, 1);
        gameState.playerTiles.get(otherPlayer).add(tile);

        Arrays.fill(gameState.assignedDices, 0);
        Arrays.fill(gameState.currentRoll, 0);
        gameState.totalDicesValue = 16; // +5 from a single worm hits value 21
        gameState.currentRoll[5] = 1; // one worm rolled
        gameState.remainingDices = 1; // should match the sum of currentRoll

        assertTrue(forwardModel.computeAvailableActions(gameState).contains(new SelectDicesAction(6, true)));

        forwardModel.next(gameState, new SelectDicesAction(6, true));

        assertEquals(0, gameState.playerTiles.get(otherPlayer).getSize());
        assertEquals(1, gameState.playerTiles.get(stealingPlayer).getSize());
        assertEquals(21, gameState.playerTiles.get(stealingPlayer).peek().getValue());
    }

    @Test
    public void nullTurnReturnedWhenNoSelectableDice() {
        setUp();

        gameState.assignedDices[0] = 1;
        gameState.assignedDices[1] = 1;
        gameState.assignedDices[2] = 0;
        gameState.assignedDices[3] = 0;
        gameState.assignedDices[4] = 0;
        gameState.assignedDices[5] = 1;

        gameState.remainingDices = parameters.numberOfDices - Arrays.stream(gameState.assignedDices).sum();

        gameState.currentRoll[0] = gameState.remainingDices;
        gameState.currentRoll[1] = 0;
        gameState.currentRoll[2] = 0;
        gameState.currentRoll[3] = 0;
        gameState.currentRoll[4] = 0;
        gameState.currentRoll[5] = 0;

        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);

        assertEquals(1, actions.size());
        assertEquals(new NullTurn(), actions.get(0));
    }

}


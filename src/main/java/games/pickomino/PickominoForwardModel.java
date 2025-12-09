package games.pickomino;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.pickomino.actions.NullTurn;
import games.pickomino.actions.SelectDicesAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class PickominoForwardModel extends StandardForwardModel {

    /**
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        PickominoGameState pGameState = (PickominoGameState) firstState;

        // Prepare a start game state

        // Create tiles with their values and scores
        pGameState.remainingTiles = new Deck<PickominoTile>("Remaining Tiles", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        PickominoParameters pGameParameters = (PickominoParameters) pGameState.getGameParameters();
        int currentScore = 0;
        for (int tileValue = pGameParameters.minTileValue; tileValue <= pGameParameters.maxTileValue; ++tileValue) {
            
            if(currentScore < pGameParameters.tilesPointsSteps.length && tileValue == pGameParameters.tilesPointsSteps[currentScore]) currentScore++;

            String tileName = "Tile " + tileValue + " (Score: " + currentScore + ")";
            pGameState.remainingTiles.add(new PickominoTile(tileName, tileValue, currentScore));
        }

        // Create player tiles
        pGameState.playerTiles = new ArrayList<>();
        for (int playerID = 0; playerID < pGameState.getNPlayers(); ++playerID) {
            pGameState.playerTiles.add(new Deck<PickominoTile>(
                "Player " + playerID + " tiles",
                playerID,
                core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL
            ));
        }

        pGameState.remainingDices = pGameParameters.numberOfDices;

        // select the first player
        pGameState.setFirstPlayer(pGameState.getRnd().nextInt(pGameState.getNPlayers()));

        // prepare the first turn
        setupTurn(pGameState);

    }

    private void setupTurn(PickominoGameState pGameState) {
        PickominoParameters pGameParameters = (PickominoParameters) pGameState.getGameParameters();
        pGameState.remainingDices = pGameParameters.numberOfDices;
        Arrays.fill(pGameState.assignedDices,0);
        pGameState.totalDicesValue = 0;
        rollDices(pGameState);
    }

    private void rollDices(PickominoGameState pGameState) {
        // roll the dices and store the result in the currentRoll array
        Arrays.fill(pGameState.currentRoll,0);
        for (int i = 0; i < pGameState.remainingDices; ++i) {
            int roll = pGameState.getRnd().nextInt(6);
            pGameState.currentRoll[roll]++;
        }
        if (pGameState.getCoreGameParameters().verbose) {
            StringBuilder rollStr = new StringBuilder("Dice roll (p" + pGameState.getCurrentPlayer() + "): ");
            boolean first = true;
            for (int d = 0; d < 6; d++) {
                if (pGameState.currentRoll[d] > 0) {
                    if (!first) rollStr.append(", ");
                    rollStr.append(pGameState.currentRoll[d]).append("x").append(d + 1);
                    first = false;
                }
            }
            System.out.println(rollStr);
        }
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        PickominoGameState pgs = (PickominoGameState) gameState;

        // Compute the minimum value to reach to stop, and the values of other players top tiles
        assert pgs.remainingTiles.getSize() != 0 : "No tiles left, should not happen when computing available actions";
        int minValueToStop = pgs.remainingTiles.peek().getValue(); // we assume the deck is sorted.
        int[] stealableValues = new int[pgs.getNPlayers() - 1];
        Arrays.fill(stealableValues, -1);
        int i = 0;
        for(int playerID = 0; playerID < pgs.getNPlayers(); playerID++){
            if(playerID != pgs.getCurrentPlayer()){
                PickominoTile topTile = pgs.playerTiles.get(playerID).peek();
                if(topTile != null) stealableValues[i] = topTile.getValue();
                ++i;
            }
        }

        // For each dice value, check if it is selectable and if the player can stop
        // The player must stop if there are not enough dices after selection
        for(int d=1; d<=6; ++d){
            if(pgs.assignedDices[d-1] > 0 || pgs.currentRoll[d-1] == 0) continue; // if not selectable, skip

            int valueIncrement = (d == 6 ? 5 : d) * pgs.currentRoll[d - 1]; // duplicated code, but it is easier to understand this way.
            int totalValue = pgs.totalDicesValue + valueIncrement;
            boolean canStop = false;
            boolean hasWorms = pgs.assignedDices[5] > 0 || d == 6;
            if(totalValue >= minValueToStop && hasWorms) {
                canStop = true;
            } else {
                // It is possible to select this dice if another player has a tile with the same value
                for(int value : stealableValues){
                    if(value == totalValue && hasWorms) {
                        canStop = true;
                        break;
                    }
                }
            }
            if(canStop) actions.add(new SelectDicesAction(d, true));

            if(pgs.remainingDices > pgs.currentRoll[d-1]) {
                actions.add(new SelectDicesAction(d, false));
            }
        } // end for d

        // If no action is available, the dice throw is null
        if(actions.isEmpty()) {
            actions.add(new NullTurn());
        }

        return actions;
    }

    /**
     * This is a method hook for any game-specific functionality that should run after an Action is executed
     * by the forward model
     *
     * @param currentState the current game state
     * @param actionTaken  the action taken by the current player, already applied to the game state
     */
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        PickominoGameState pGameState = (PickominoGameState) currentState;

        // Check for end of turn
        boolean endOfTurn = false;
        if(actionTaken instanceof NullTurn) endOfTurn = true;
        if(actionTaken instanceof SelectDicesAction a) {
            if(a.isStop()) endOfTurn = true;
        }

        // Check for end of game, when there is no tile left in the remaining tiles deck
        if(endOfTurn && pGameState.remainingTiles.getSize() == 0) {
            if (pGameState.getCoreGameParameters().verbose) {
                System.out.println("Game over! All tiles have been taken.");
            }
            endGame(pGameState);
            return;
        }

        if(endOfTurn) {
            if (pGameState.getCoreGameParameters().verbose) {
                System.out.println("End of turn for p" + pGameState.getCurrentPlayer());
            }
            endPlayerTurn(pGameState);
            if(pGameState.getCurrentPlayer() == 0) {
                if (pGameState.getCoreGameParameters().verbose) {
                    System.out.println("End of round " + pGameState.getRoundCounter());
                }
                endRound(pGameState);
            }
            setupTurn(pGameState);
            if (pGameState.getCoreGameParameters().verbose) {
                System.out.println("Starting turn for p" + pGameState.getCurrentPlayer());
            }
            return;
        }

        // Otherwise, the action is valid and the turn continues
        // Roll remaining dices. The player will have to choose again.
        rollDices(pGameState);

    }


}


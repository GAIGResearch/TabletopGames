package games.backgammon;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Dice;
import gametemplate.actions.GTAction;

import java.util.ArrayList;
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
public class BGForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        BGGameState gameState = (BGGameState) firstState;
        BGParameters bgp = (BGParameters) firstState.getGameParameters();

        gameState.piecesPerPoint = new int[2][bgp.boardSize];
        gameState.piecesOnBar = new int[2];
        gameState.piecesOnBar[0] = bgp.startingAtBar;
        gameState.piecesOnBar[1] = bgp.startingAtBar;
        gameState.piecesBorneOff = new int[2];

        // Initialize pieces according to BGParameters
        gameState.piecesPerPoint[0][0] = bgp.startingAt24;
        gameState.piecesPerPoint[0][5] = bgp.startingAt6;
        gameState.piecesPerPoint[0][7] = bgp.startingAt8;
        gameState.piecesPerPoint[0][12] = bgp.startingAt13;

        gameState.piecesPerPoint[1][0] = bgp.startingAt24;
        gameState.piecesPerPoint[1][5] = bgp.startingAt6;
        gameState.piecesPerPoint[1][7] = bgp.startingAt8;
        gameState.piecesPerPoint[1][12] = bgp.startingAt13;

        gameState.dice = new Dice[bgp.diceNumber];
        for (int i = 0; i < bgp.diceNumber; i++) {
            gameState.dice[i] = new Dice(bgp.diceSides);
        }
        gameState.diceUsed = new boolean[bgp.diceNumber];

        gameState.blots = new int[2];
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        // TODO: Add in bearing off actions once possible
        // TODO: force move of any pieces on bar first
        List<AbstractAction> actions = new ArrayList<>();
        // We create the set of possible actions
        // For each available dice value we consider each point that has player tokens on
        // and add the possible moves to the list of actions
        // (removing any moves that would move to a point occupied by two or more opponent tokens)
        BGGameState bgs = (BGGameState) gameState;
        int playerId = bgs.getCurrentPlayer();
        int[] diceAvailable = bgs.getAvailableDiceValues();
        int boardSize = bgs.piecesPerPoint[playerId].length;
        for (int i : diceAvailable) {
            for (int j = 0; j < boardSize; j++) {
                if (bgs.piecesPerPoint[playerId][j] > 0) {
                    int to = j + bgs.dice[i].getValue();
                    if (to < boardSize) {
                        if (bgs.piecesPerPoint[1 - playerId][boardSize - to - 1] < 2) {
                            // we can move to this point
                            actions.add(new MovePiece(j, to));
                        }
                    }
                }
            }
        }
        return actions;
    }

    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        // a player's turn ends when they have used all the dice values, or have no valid moves
        BGGameState bgs = (BGGameState) currentState;
        int[] diceAvailable = bgs.getAvailableDiceValues();
        if (diceAvailable.length == 0 || computeAvailableActions(currentState).isEmpty()) {
            // end of turn: switch player
            bgs.rollDice();
            endPlayerTurn(bgs);  // default is to move to next player
        }
    }
}

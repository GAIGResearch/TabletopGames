package games.pentegrammai;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Dice;
import core.components.Token;

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
public class PenteForwardModel extends StandardForwardModel {

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        PenteGameState state = (PenteGameState) firstState;
        state.board = new ArrayList<>();
        for (int i = 0; i < state.boardSize; i++) {
            state.board.add(new ArrayList<>());
        }
        // Place pieces for each player on their starting points
        int piecesPerPlayer = state.boardSize / 2;
        for (int player = 0; player < 2; player++) {
            int start = state.playerStart[player];
            for (int i = 0; i < piecesPerPlayer; i++) {
                Token t = new Token("P" + player + "_T" + i);
                t.setOwnerId(player);
                state.board.get(start + i).add(t);
            }
        }
        state.die = new Dice(state.dieSides);
        state.die.roll(state.getRnd());
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        PenteGameState state = (PenteGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        int player = state.getCurrentPlayer();

        int dieValue = state.die.getValue();
        for (Token t : state.getPlayerTokens(player)) {
            int from = state.findTokenPosition(t);
            int to = (from + dieValue) % state.boardSize;
            if (!state.isAtGoal(t, player) && (state.canPlace(to) || state.isSacred(to))) {
                actions.add(new PenteMoveAction(from, to));
            }
        }
        if (actions.isEmpty()) {
            actions.add(new DoNothing());
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
    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        PenteGameState state = (PenteGameState) currentState;
        int piecesPerPlayer = state.boardSize / 2;
        for (int player = 0; player < 2; player++) {
            if (state.getPiecesAtGoal(player) == piecesPerPlayer) {
                endGame(state);
                return;
            }
        }
        endPlayerTurn(state);
        if (state.getCurrentPlayer() == 0) {
            endRound(state);
        }
        state.die.roll(state.getRnd());
    }
}

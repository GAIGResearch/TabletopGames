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
        PenteParameters params = state.getParams();
        state.playerGoal = new int[2];
        state.blotCount = new int[2];
        state.playerGoal[0] = params.sacredPoints[1];
        state.playerGoal[1] = params.sacredPoints[0];
        state.offBoard = new ArrayList<>();

        // player entry only used in Kidd's variant
        state.playerEntry = new int[2];
        state.playerEntry[0] = 0;
        state.playerEntry[1] = params.boardSize / 2;

        state.board = new ArrayList<>();
        for (int i = 0; i < params.boardSize; i++) {
            state.board.add(new ArrayList<>());
        }
        // Place pieces for each player on their starting points
        int piecesPerPlayer = params.boardSize / 2;
        // in this case all Tokens start off the board
        for (int player = 0; player < 2; player++) {
            int entry = player == 0 ? 0 : params.boardSize / 2;
            for (int i = 0; i < piecesPerPlayer; i++) {
                Token t = new Token("P" + player + "_T" + i);
                t.setOwnerId(player);
                if (state.getParams().kiddsVariant) {
                    state.setOffBoard(t);
                } else {
                    state.board.get(entry + i).add(t);
                }
            }
        }
        if (params.customDie != null) {
            state.die = params.customDie.copy();
        } else {
            state.die = new Dice(params.dieSides);
        }
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

        if (!state.offBoard.isEmpty() && state.offBoard.stream().anyMatch(t -> t.getOwnerId() == player)) {
            // we have to move pieces that are off the board first
            int from = -1;
            int to = (state.playerEntry[player] + dieValue - 1) % state.board.size();
            if (state.canPlace(to)) {
                actions.add(new PenteMoveAction(from, to));
            }
        } else {
            for (int from = 0; from < state.board.size(); from++) {
                int to = (from + dieValue) % state.board.size();
                if (state.canPlace(to) && state.getPiecesAt(from, player) > 0) {
                    actions.add(new PenteMoveAction(from, to));
                }
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
        int piecesPerPlayer = state.board.size() / 2;
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

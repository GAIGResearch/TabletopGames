package games.chinesecheckers;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.grid.HexCoord;
import games.chinesecheckers.actions.MovePeg;
import games.chinesecheckers.components.Peg;
import games.chinesecheckers.components.StarBoard;

import java.util.*;

import static core.CoreConstants.GameResult.*;

public class CCForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        CCGameState state = (CCGameState) firstState;
        CCParameters params = (CCParameters) state.getGameParameters();

        state.starBoard = new StarBoard();

        Peg.Colour[] playerColours = params.playerColours.get(state.getNPlayers());

        for (int i = 0; i < state.getNPlayers(); i++) {
            loadPegs(state, playerColours[i]);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        CCGameState state = (CCGameState) gameState;
        int player = gameState.getCurrentPlayer();

        return loadPlayerActions(player, state);
    }

    /**
     * Returns true if the peg can be placed on the node
     * All board nodes in the main areas are neutral; the colour here refers just to the 10
     * starting nodes for each player
     */
    private boolean isPlayerPlaceable(Peg.Colour col, Peg.Colour playerCol) {
        return col == playerCol || col == Peg.Colour.neutral ||
                col == Peg.Colour.values()[(playerCol.ordinal() + 3) % 6]; //opposite
    }

    private List<AbstractAction> loadPlayerActions(int player, CCGameState state) {
        //player index to colour
        Peg.Colour playerCol = state.getPlayerColour(player);
        StarBoard board = state.starBoard;
        List<AbstractAction> actions = new ArrayList<>();
        for (int node = 0; node < board.nNodes(); node++) { // Check all Nodes
            Peg peg = board.getPeg(node);
            if (peg != null && peg.colour() == playerCol) {
                actions.addAll(exploreNodeAction(board, node));
            }
        }
        return actions;
    }

    /**
     * In which we use a form of breadth-first search to find all the possible moves we can make
     * starting from the given node
     */
    private List<AbstractAction> exploreNodeAction(StarBoard board, int node) {
        Peg peg = board.getPeg(node);
        Peg.Colour playerCol = peg.colour();
        Set<AbstractAction> actions = new LinkedHashSet<>();
        // first get the single directly adjacent moves
        HexCoord from = board.getPosition(node);
        for (int d = 0; d < from.nDirections(); d++) {
            int nei_0 = board.getNode(from.neighbour(d));
            if (nei_0 >= 0 && !board.isOccupied(nei_0) && isPlayerPlaceable(board.getBaseColour(nei_0), playerCol)) {
                // once in its destination, a peg steps only within it
                if (!peg.inDestination() || board.getBaseColour(nei_0) != Peg.Colour.neutral) {
                    actions.add(new MovePeg(node, nei_0));
                }
            }
        }
        // then get the jumping stuff
        repeatAction(board, node, actions, playerCol);
        return new ArrayList<>(actions);
    }

    private void repeatAction(StarBoard board, int node, Set<AbstractAction> actions, Peg.Colour playerCol) {
        // This looks for a chain of jumps, without revisiting previous nodes, that starts at the given node
        Set<Integer> visited = new LinkedHashSet<>();
        Queue<Integer> toVisit = new ArrayDeque<>();
        toVisit.add(node);

        while (!toVisit.isEmpty()) {
            int expNode = toVisit.poll();
            visited.add(expNode);
            // once in target zone, a peg may not leave it
            boolean canLeaveZone = board.getBaseColour(expNode) != playerCol;
            HexCoord here = board.getPosition(expNode);
            for (int d = 0; d < here.nDirections(); d++) {
                // over an occupied neighbour to the empty node beyond it in the same direction
                HexCoord over = here.neighbour(d);
                int neighbour = board.getNode(over);
                if (neighbour < 0 || !board.isOccupied(neighbour)) continue;
                int stride = board.getNode(over.neighbour(d));
                if (stride >= 0 && !board.isOccupied(stride) &&
                        (canLeaveZone || board.getBaseColour(stride) == playerCol) &&
                        !visited.contains(stride)) {
                    toVisit.add(stride);
                }
            }
        }
        visited.remove(node);
        for (int v : visited) {
            if (isPlayerPlaceable(board.getBaseColour(v), playerCol))
                actions.add(new MovePeg(node, v));
        }
    }

    @Override
    protected void endGame(AbstractGameState gs) {
        // We override the standard endGame() as we have one winner, and the rest losers
        // with no formal score mechanism
        CCGameState state = (CCGameState) gs;

        state.setGameStatus(CoreConstants.GameResult.GAME_END);
        state.setPlayerResult(WIN_GAME, state.getCurrentPlayer());
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (i != state.getCurrentPlayer()) {
                state.setPlayerResult(LOSE_GAME, i);
            }
        }
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        CCGameState state = (CCGameState) currentState;

        for (int p = 0; p < state.getNPlayers(); p++) {
            if (checkWinCondition(state, p)) {
                endGame(state);
            }
        }
        if (state.isNotTerminal()) {
            int currentPlayer = state.getCurrentPlayer();
            endPlayerTurn(state);
            if (state.getCurrentPlayer() == 0 && currentPlayer != 0) {
                endRound(state);
            }
        }
    }

    private boolean checkWinCondition(CCGameState state, int player) {
        Peg.Colour colour = state.getPlayerColour(player);
        CCParameters params = (CCParameters) state.getGameParameters();
        int[] colourIndices = params.colourIndices.get(colour);
        int counter = 0;
        boolean PegIn = false;
        // we win if all target nodes are occupied, and at least one of them is ours (anti-spoiling rule)
        StarBoard board = state.getStarBoard();
        for (int i : colourIndices) {
            Peg peg = board.getPeg(i);
            if (peg != null && peg.colour() == colour) {
                PegIn = true;
            }
            if (peg != null) {
                counter++;
            }
        }
        return counter >= 10 && PegIn;
    }

    private void loadPegs(CCGameState state, Peg.Colour colour) {
        CCParameters params = (CCParameters) state.getGameParameters();
        int[] indices = params.colourIndices.get(params.boardOpposites.get(colour));
        for (int i : indices) {
            state.starBoard.setPeg(i, new Peg(colour, false));
        }
    }
}

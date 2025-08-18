package games.chinesecheckers;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import games.chinesecheckers.actions.MovePeg;
import games.chinesecheckers.components.CCNode;
import games.chinesecheckers.components.Peg;
import games.chinesecheckers.components.StarBoard;

import java.util.*;
import java.util.stream.Collectors;

import static core.CoreConstants.GameResult.*;

public class CCForwardModel extends StandardForwardModel {

    private static boolean isColourInPlay(Peg.Colour col, CCGameState state) {
        if (col == Peg.Colour.neutral) return true;
        CCParameters params = (CCParameters) state.getGameParameters();
        Peg.Colour[] colours = params.playerColours.get(state.getNPlayers());
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (colours[i] == col) return true;
        }
        return false;
    }

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

    private static CCNode neighbourInDirection(CCNode node, int dir) {
        int id = node.getNeighbourBySide(dir);
        if (id == -1) {
            return null; // No neighbour in this direction
        }
        for (CCNode neig : node.getNeighbours()) {
            if (neig.getID() == id) {
                return neig;
            }
        }
        return null;
    }

    /**
     * Returns true if the peg can be placed on the node
     * All board nodes in the main areas are neutral; the colour here refers just to the 10
     * starting nodes for each player
     *
     * @param col
     * @param playerCol
     * @return
     */
    private static boolean isPlayerPlaceable(Peg.Colour col, Peg.Colour playerCol) {
        return col == playerCol || col == Peg.Colour.neutral ||
                col == Peg.Colour.values()[(playerCol.ordinal() + 3) % 6]; //opposite
    }

    private List<AbstractAction> loadPlayerActions(int player, CCGameState state) {
        //player index to colour
        Peg.Colour playerCol = state.getPlayerColour(player);
        List<AbstractAction> actions = new ArrayList<>();
        for (CCNode node : state.starBoard.getBoardNodes()) { // Check all Nodes
            if (node.getOccupiedPeg() != null && node.getOccupiedPeg().getColour() == playerCol) {
                actions.addAll(exploreNodeAction(node));
            }
        }
        return actions;
    }

    /**
     * In which we use a form of breadth-first search to find all the possible moves we can make
     * starting from the given node
     *
     * @param node
     * @return
     */
    private static List<AbstractAction> exploreNodeAction(CCNode node) {
        Peg.Colour playerCol = node.getOccupiedPeg().getColour();
        List<AbstractAction> actions = new ArrayList<>();
        // first get the single directly adjacent moves
        for (CCNode nei_0 : node.getNeighbours()) {
            if (!nei_0.isNodeOccupied() && isPlayerPlaceable(nei_0.getBaseColour(), playerCol)) {
                if (node.getOccupiedPeg().getInDestination()) {
                    if (nei_0.getBaseColour() != Peg.Colour.neutral) {
                        MovePeg action = new MovePeg(node.getID(), nei_0.getID());
                        if (!actions.contains(action)) {
                            actions.add(action);
                        }
                    }
                } else {
                    MovePeg action = new MovePeg(node.getID(), nei_0.getID());
                    if (!actions.contains(action)) {
                        actions.add(action);
                    }
                }
            }
        }
        // then get the jumping stuff
        repeatAction(node, actions, playerCol);
        return actions.stream().distinct().collect(Collectors.toList());
    }

    private static void repeatAction(CCNode node, List<AbstractAction> actions, Peg.Colour playerCol) {
        List<CCNode> visited = new ArrayList<>();
        Queue<CCNode> toVisit = new LinkedList<>();
        toVisit.add(node);

        // This should be looking for a chain of moves, without revisiting previous nodes
        // that starts at the given node. Hence (if true), it should only be called once per peg
        // and not once per neighbouring peg

        while (!toVisit.isEmpty()) {
            CCNode expNode = toVisit.poll();
            visited.add(expNode);
            // once in target zone, a peg may not leave it
            boolean canLeaveZone = expNode.getBaseColour() != playerCol;
            for (CCNode neighbour : expNode.getNeighbours()) {
                int side = expNode.getSideOfNeighbour(neighbour.getID());
                if (neighbour.isNodeOccupied()) {
                    CCNode stride = neighbourInDirection(neighbour, side);
                    if (stride != null && !stride.isNodeOccupied() &&
                            (canLeaveZone || stride.getBaseColour() == playerCol) &&
                            !visited.contains(stride)) {
                        toVisit.add(stride);
                    }
                }
            }
        }
        visited.remove(node);
        visited.removeIf(n -> (!isPlayerPlaceable(n.getBaseColour(), playerCol)));
        for (CCNode v : visited) {
            MovePeg action = new MovePeg(node.getID(), v.getID());
            actions.add(action);
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
        List<CCNode> nodes = state.getStarBoard().getBoardNodes();
        for (int i : colourIndices) {
            if (nodes.get(i).isNodeOccupied() && nodes.get(i).getOccupiedPeg().getColour() == colour) {
                PegIn = true;
            }
            if (nodes.get(i).isNodeOccupied()) {
                counter++;
            }
        }
        return counter >= 10 && PegIn;
    }

    private void loadPegs(CCGameState state, Peg.Colour colour) {
        CCParameters params = (CCParameters) state.getGameParameters();
        int[] indices = params.colourIndices.get(params.boardOpposites.get(colour));
        for (int i : indices) {
            state.starBoard.getBoardNodes().get(i).setOccupiedPeg(new Peg(colour, state.starBoard.getBoardNodes().get(i)));
        }
    }
}

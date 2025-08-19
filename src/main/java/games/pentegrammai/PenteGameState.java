package games.pentegrammai;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Dice;
import core.components.Token;
import games.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PenteGameState extends AbstractGameState {

    // Board: 10 points, index 0-4 east, 5-9 west. Sacred points: 2 (east), 7 (west)
    protected List<List<Token>> board; // board.get(i) = list of tokens at point i
    protected int[] playerStartEast = {0, 5}; // player 0 starts east (0-4), player 1 west (5-9)
    protected int[] playerGoal = {7, 2}; // for clarity, same as sacred
    protected int[] playerStart; // [0]=east, [1]=west
    protected Dice die; // single die for the game

    public int boardSize;
    public int dieSides;
    public int[] sacredPoints;

    public PenteGameState(PenteParameters params, int nPlayers) {
        super(params, nPlayers);
        playerStart = new int[]{0, 5};
        this.boardSize = params.boardSize;
        this.dieSides = params.dieSides;
        this.sacredPoints = Arrays.copyOf(params.sacredPoints, params.sacredPoints.length);
        die = new Dice(dieSides);
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.PenteGrammai;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        for (List<Token> tokens : board) {
            components.addAll(tokens);
        }
        return components;
    }

    @Override
    protected PenteGameState _copy(int playerId) {
        PenteGameState copy = new PenteGameState((PenteParameters) getGameParameters(), getNPlayers());
        copy.board = new ArrayList<>();
        for (List<Token> tokens : this.board) {
            List<Token> newTokens = new ArrayList<>();
            for (Token t : tokens) {
                newTokens.add(t.copy());
            }
            copy.board.add(newTokens);
        }
        copy.playerStart = playerStart.clone();
        copy.die = die.copy();
        copy.boardSize = boardSize;
        copy.dieSides = dieSides;
        copy.sacredPoints = Arrays.copyOf(sacredPoints, sacredPoints.length);
        return copy;
    }

    /**
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            // Score: fraction of pieces at goal + fraction advanced
            double score = getPiecesAtGoal(playerId) / (double) (boardSize / 2);
            int totalAdvanced = 0;
            for (Token t : getPlayerTokens(playerId)) {
                int pos = findTokenPosition(t);
                int dist = distanceToGoal(playerId, pos);
                score += (boardSize - dist) / (double) (boardSize * (boardSize / 2));
            }
            return score;
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    /**
     * @param playerId - player observing the state.
     * @return the true score for the player, according to the game rules. May be 0 if there is no score in the game.
     */
    @Override
    public double getGameScore(int playerId) {
        return getPiecesAtGoal(playerId);
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PenteGameState that)) return false;
        return Objects.equals(board, that.board) &&
                Arrays.equals(playerStart, that.playerStart) &&
                Objects.equals(die, that.die);
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, Arrays.hashCode(playerStart), die, super.hashCode());
    }

    // --- Helper methods for game logic ---

    public List<Token> getPlayerTokens(int playerId) {
        List<Token> tokens = new ArrayList<>();
        for (List<Token> point : board) {
            for (Token t : point) {
                if (t.getOwnerId() == playerId) tokens.add(t);
            }
        }
        return tokens;
    }

    public int findTokenPosition(Token t) {
        for (int i = 0; i < board.size(); i++) {
            if (board.get(i).contains(t)) return i;
        }
        return -1;
    }

    public int distanceToGoal(int playerId, int pos) {
        int goal = playerGoal[playerId];
        // All players move anti-clockwise (+1)
        if (pos <= goal) return goal - pos;
        else return boardSize - pos + goal;
    }

    public boolean isSacred(int pos) {
        for (int sacred : sacredPoints) {
            if (pos == sacred) return true;
        }
        return false;
    }

    public boolean isOccupied(int pos) {
        return !board.get(pos).isEmpty();
    }

    public boolean canPlace(int pos) {
        // Only one piece per point except sacred points (can have any number, both players)
        if (isSacred(pos)) return true;
        return board.get(pos).isEmpty();
        // Only one piece per point (not sacred)
    }

    public boolean isAtGoal(Token t, int playerId) {
        return findTokenPosition(t) == playerGoal[playerId];
    }

    public int getPiecesAtGoal(int playerId) {
        int goal = playerGoal[playerId];
        int count = 0;
        for (Token t : board.get(goal)) {
            if (t.getOwnerId() == playerId) count++;
        }
        return count;
    }

    /**
     * Helper method for tests: directly set the die value.
     */
    public void setDieValue(int value) {
        this.die.setValue(value);
    }
}

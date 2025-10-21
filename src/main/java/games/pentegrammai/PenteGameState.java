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
    int[] blotCount;
    protected int[] playerGoal;
    protected int[] playerEntry;
    protected Dice die; // single die for the game
    List<Token> offBoard = new ArrayList<>(); // tokens that are off the board (due to blots)

    public int[] sacredPoints;

    public PenteGameState(AbstractParameters parameters, int nPlayers) {
        super(parameters, nPlayers);
        PenteParameters params = (PenteParameters) parameters;
        this.sacredPoints = Arrays.copyOf(params.sacredPoints, params.sacredPoints.length);
        die = new Dice(params.dieSides);
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
        PenteGameState copy = new PenteGameState(getGameParameters(), getNPlayers());
        copy.board = new ArrayList<>();
        for (List<Token> tokens : this.board) {
            List<Token> newTokens = new ArrayList<>();
            for (Token t : tokens) {
                newTokens.add(t.copy());
            }
            copy.board.add(newTokens);
        }
        copy.die = die.copy();
        copy.blotCount = Arrays.copyOf(blotCount, blotCount.length);
        copy.playerGoal = Arrays.copyOf(playerGoal, playerGoal.length);
        copy.playerEntry = Arrays.copyOf(playerEntry, playerEntry.length);
        copy.offBoard = new ArrayList<>();
        for (Token t : this.offBoard) {
            copy.offBoard.add(t.copy());
        }
        copy.sacredPoints = Arrays.copyOf(sacredPoints, sacredPoints.length);
        return copy;
    }

    public PenteParameters getParams() {
        return (PenteParameters) getGameParameters();
    }
    /**
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            int boardSize = this.board.size();
            // Score: fraction of pieces at goal + fraction advanced
            double score = getPiecesAtGoal(playerId) / (double) (boardSize / 2);
            int totalAdvanced = 0;
            for (int i = 0; i < boardSize; i++) {
                int distanceToGoal = distanceToGoal(playerId, i);
                totalAdvanced += getPiecesAt(i, playerId) * distanceToGoal;
            }
            score += (double) totalAdvanced / boardSize / 2;
            return score;
        } else {
            return getGameScore(playerId);
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
        return board.equals(that.board) && offBoard.equals(that.offBoard) &&
                Arrays.equals(sacredPoints, that.sacredPoints) &&
                Arrays.equals(blotCount, that.blotCount) &&
                Arrays.equals(playerGoal, that.playerGoal) &&
                Arrays.equals(playerEntry, that.playerEntry) &&
                Objects.equals(die, that.die);
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, offBoard, die, super.hashCode()) +
                31 * Arrays.hashCode(sacredPoints) +
                31 * 31 * Arrays.hashCode(playerGoal) +
                31 * 31 * Arrays.hashCode(playerEntry) +
                31 * 31 * 31 * Arrays.hashCode(blotCount);
    }

    public int distanceToGoal(int playerId, int pos) {
        int goal = playerGoal[playerId];
        if (pos <= goal) return goal - pos;
        else return this.board.size() - pos + goal;
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
        if (getParams().kiddsVariant) {
            // backgammon like rules
            int otherPlayersPieces = getPiecesAt(pos, 1 - getCurrentPlayer());
            return otherPlayersPieces < 2; // can place if less than 2 pieces of
        } else {
            return isSacred(pos) || board.get(pos).isEmpty();
            // Only one piece per point (not sacred)
        }
    }

    public int getPiecesAtGoal(int playerId) {
        int goal = playerGoal[playerId];
        return getPiecesAt(goal, playerId);
    }

    public int getPiecesAt(int from, int player) {
        int count = 0;
        for (Token t : board.get(from)) {
            if (t.getOwnerId() == player) count++;
        }
        return count;
    }


    public void setOffBoard(Token removed) {
        offBoard.add(removed);
    }
    public int getOffBoard(int player) {
        int count = 0;
        for (Token t : offBoard) {
            if (t.getOwnerId() == player) count++;
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

package games.hareandtortoise;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import games.hareandtortoise.components.HareCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static games.hareandtortoise.HareAndTortoiseParameters.BOARD;
import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;

/**
 * <p>State tracked (the board layout itself is fixed, in HareAndTortoiseParameters.BOARD):</p>
 * <ul>
 *     <li>squares - the square of each player's runner: 0 is START, 1-63 the board, HOME_SQUARE (64) HOME</li>
 *     <li>carrots - the carrots each player holds</li>
 *     <li>lettuces - the lettuce cards each player still holds</li>
 *     <li>lettuceToChew - whether each runner has reached a lettuce square and has yet to chew a lettuce there</li>
 *     <li>missNextTurn - whether each player's next turn will be skipped</li>
 *     <li>finishPositions - the place in which each player got HOME (1 for the first), or 0 if not home yet</li>
 *     <li>hareDeck - the hare cards, top first</li>
 *     <li>nUnseenHareCards - how many cards at the top of the hareDeck have never been drawn</li>
 * </ul>
 * The carrot patch has no limit in the 1978 rules, so it is not modelled.
 */
public class HareAndTortoiseGameState extends AbstractGameState {

    int[] squares;
    int[] carrots;
    int[] lettuces;
    boolean[] lettuceToChew;
    boolean[] missNextTurn;
    int[] finishPositions;
    Deck<HareCard> hareDeck;
    int nUnseenHareCards;

    public HareAndTortoiseGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.HareAndTortoise;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.add(hareDeck);
        return components;
    }

    public int getSquare(int player) {
        return squares[player];
    }

    public SquareType getSquareType(int player) {
        return BOARD[squares[player]];
    }

    public int getCarrots(int player) {
        return carrots[player];
    }

    public int getLettuces(int player) {
        return lettuces[player];
    }

    public boolean hasLettuceToChew(int player) {
        return lettuceToChew[player];
    }

    public boolean missesNextTurn(int player) {
        return missNextTurn[player];
    }

    public int getFinishPosition(int player) {
        return finishPositions[player];
    }

    public boolean isHome(int player) {
        return finishPositions[player] > 0;
    }

    public Deck<HareCard> getHareDeck() {
        return hareDeck;
    }

    public int getNUnseenHareCards() {
        return nUnseenHareCards;
    }

    /**
     * Puts the player's runner on the square, and changes their carrots by the given amount (negative to pay).
     */
    public void moveRunner(int player, int square, int carrotChange) {
        squares[player] = square;
        carrots[player] += carrotChange;
    }

    public void addCarrots(int player, int amount) {
        carrots[player] += amount;
    }

    /**
     * Discards one of the player's lettuces, and draws carrots for their race position.
     */
    public void chewLettuce(int player) {
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) gameParameters;
        lettuces[player]--;
        carrots[player] += params.carrotsPerRacePosition * getRacePosition(player);
    }

    public void setLettuceToChew(int player, boolean value) {
        lettuceToChew[player] = value;
    }

    public void setFinishPosition(int player, int position) {
        finishPositions[player] = position;
    }

    public int getNPlayersHome() {
        int n = 0;
        for (int f : finishPositions)
            if (f > 0) n++;
        return n;
    }

    /**
     * Whether a runner still in the race stands on the square. START and HOME are never occupied in this sense:
     * any number of runners may share them.
     */
    public boolean isOccupied(int square) {
        if (square == 0 || square == HOME_SQUARE) return false;
        for (int p = 0; p < getNPlayers(); p++)
            if (squares[p] == square)
                return true;
        return false;
    }

    /**
     * The player's position in the race, 1 for the leader.
     */
    public int getRacePosition(int player) {
        if (isHome(player))
            return finishPositions[player];
        // every player home is ahead of every runner still racing; runners on the same square (START) share a place
        int position = getNPlayersHome() + 1;
        for (int p = 0; p < getNPlayers(); p++)
            if (p != player && !isHome(p) && squares[p] > squares[player])
                position++;
        return position;
    }

    @Override
    protected HareAndTortoiseGameState _copy(int playerId) {
        HareAndTortoiseGameState copy = new HareAndTortoiseGameState(gameParameters, getNPlayers());
        copy.squares = squares.clone();
        copy.carrots = carrots.clone();
        copy.lettuces = lettuces.clone();
        copy.lettuceToChew = lettuceToChew.clone();
        copy.missNextTurn = missNextTurn.clone();
        copy.finishPositions = finishPositions.clone();
        copy.hareDeck = hareDeck.copy();
        copy.nUnseenHareCards = nUnseenHareCards;
        // Every payment is made openly, so only the order of the hare cards never yet drawn is hidden. A drawn card
        // goes to the bottom, where its place is known to all.
        if (playerId != -1 && getCoreGameParameters().partialObservable)
            Collections.shuffle(copy.hareDeck.getComponents().subList(0, nUnseenHareCards), redeterminisationRnd);
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        // the share of the course covered
        return (double) squares[playerId] / HOME_SQUARE;
    }

    /**
     * One more than the number of players behind the player in the race.
     */
    @Override
    public double getGameScore(int playerId) {
        return getNPlayers() + 1 - getRacePosition(playerId);
    }

    @Override
    public int getOrdinalPosition(int playerId) {
        return getRacePosition(playerId);
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HareAndTortoiseGameState that)) return false;
        return nUnseenHareCards == that.nUnseenHareCards &&
                Arrays.equals(squares, that.squares) &&
                Arrays.equals(carrots, that.carrots) &&
                Arrays.equals(lettuces, that.lettuces) &&
                Arrays.equals(lettuceToChew, that.lettuceToChew) &&
                Arrays.equals(missNextTurn, that.missNextTurn) &&
                Arrays.equals(finishPositions, that.finishPositions) &&
                Objects.equals(hareDeck, that.hareDeck);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), hareDeck, nUnseenHareCards);
        result = 31 * result + Arrays.hashCode(squares);
        result = 31 * result + Arrays.hashCode(carrots);
        result = 31 * result + Arrays.hashCode(lettuces);
        result = 31 * result + Arrays.hashCode(lettuceToChew);
        result = 31 * result + Arrays.hashCode(missNextTurn);
        result = 31 * result + Arrays.hashCode(finishPositions);
        return result;
    }
}

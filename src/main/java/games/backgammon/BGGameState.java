package games.backgammon;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import games.GameType;

import java.util.*;

public class BGGameState extends AbstractGameState {

//    Backgammon involves moving 15 checkers around a board, aiming to be the first to "bear off" (remove) all your pieces before your opponent. Players move their pieces based on dice rolls, and a key strategy involves hitting opponent's pieces (blots) to send them to the bar, forcing them to re-enter the game.
//    Here's a more detailed breakdown of the rules:
//
//    1. Setup:
//    Board: The board has 24 triangular points, divided into four quadrants, with the "home" areas for each player on opposite sides.
//            Checkers: Each player starts with 15 checkers, initially placed in a specific configuration on the board.
//            Dice: Two dice are used
//
//    2. Game Play:
//    Moving Checkers:
//    Players move their checkers around the board, in opposite directions, based on the numbers rolled on the two dice.
//    Hitting Checkers:
//    If a checker lands on a point occupied by only one of the opponent's checkers (a "blot"), that checker is hit and sent to the "bar".
//    Re-entering Checkers:
//    Players must re-enter any checkers on the bar before moving other checkers.
//    Bearing Off:
//    Once all checkers are in the home board, players can start "bearing off" (removing) checkers from the board.
//    Winning the Game:
//    The first player to bear off all their checkers wins the game.
//
//    3. Key Rules:
//    Open Points: A point that is not occupied by two or more of the opponent's checkers.
//    Closed Points: A point occupied by two or more of the opponent's checkers.
//    Moving Checkers: Players must move both numbers rolled, if possible.
//    Cannot Move: If a player cannot play all the numbers rolled, they must play as many as possible, starting with the larger number.
//    Bouncing: If a player cannot move a checker because of the opponent's closed points, they cannot move any other checkers until the opponent's points become open.

    protected List<List<Token>> counters; // the master list of counters, used to track the pieces on the board
    protected int[][] playerTrackMapping; // maps player-specific track positions to counters indices
    protected int[] piecesBorneOff;

    protected Dice[] dice;
    protected int[] availableDiceValues; // the values of the dice rolled, used for actions
    protected boolean[] diceUsed;

    protected int[] blots;

    protected List<Token> movedThisTurn;

    public BGGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.Backgammon;
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
        for (int playerId = 0; playerId < getNPlayers(); playerId++) {
            components.addAll(counters.get(playerId));
        }
        return components;
    }

    public int getPiecesOnPoint(int playerId, int point) {
        return (int) counters.get(point).stream().filter(token -> token.getOwnerId() == playerId).count();
    }

    public int getPiecesOnBar(int playerId) {
        return getPiecesOnPoint(playerId, 0);
    }

    public int getPiecesBorneOff(int playerId) {
        return piecesBorneOff[playerId];
    }

    /*
    Returns the index of the space on the physical board corresponding to the nth point on the 'race track' for the player
     */
    public int getPhysicalSpace(int playerId, int nthPoint) {
        return playerTrackMapping[playerId][nthPoint];
    }

    /*
    The reverse mapping from the physical space on the board to the logical position on the player's track.
     */
    public int getLogicalPosition(int playerId, int physicalPoint) {
        if (physicalPoint == 0) return 0; // the bar is always just off the track point
        for (int i = 0; i < playerTrackMapping[playerId].length; i++) {
            if (playerTrackMapping[playerId][i] == physicalPoint) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException("Physical point not found in player's track: " + physicalPoint);
    }

    public void movePiece(int playerId, int from, int to) {
        // Moving on the board
        List<Token> fromList = counters.get(from);
        Optional<Token> tokenOpt = fromList.stream()
                .filter(t -> t.getOwnerId() == playerId)
                .findFirst();
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("No pieces on the from point for player " + playerId);
        }
        Token token = tokenOpt.get();
        if (!movedThisTurn.contains(token)) {
            movedThisTurn.add(token);
        }
        fromList.remove(token);
        if (to == -1) {
            piecesBorneOff[playerId]++;
        } else {
            counters.get(to).add(token);
        }
    }

    public void movePieceToBar(int playerId, int point) {
        movePiece(playerId, point, 0);
        blots[playerId]++;
    }

    public void rollDice() {
        for (Dice die : dice) {
            die.roll(rnd);
        }
        updateAvailableDiceValues();
    }

    protected void updateAvailableDiceValues() {
        BGParameters params = (BGParameters) getGameParameters();
        if (params.doubleActions && Arrays.stream(dice).allMatch(d -> d.getValue() == dice[0].getValue())) {
            // if doubles, we get double the actions
            diceUsed = new boolean[dice.length * 2];
            availableDiceValues = new int[dice.length * 2];
            for (int i = 0; i < dice.length; i++) {
                availableDiceValues[i] = dice[i].getValue();
                availableDiceValues[i + dice.length] = dice[i].getValue();
            }
        } else {
            // otherwise, we just use the number of dice rolled
            diceUsed = new boolean[dice.length];
            availableDiceValues = new int[dice.length];
            for (int i = 0; i < dice.length; i++) {
                availableDiceValues[i] = dice[i].getValue();
            }
        }
    }

    // for testing only
    public void setDiceValues(int[] values) {
        for (int i = 0; i < dice.length; i++) {
            dice[i].setValue(values[i]);
        }
        updateAvailableDiceValues();
    }

    public void useDiceValue(int dieValue) {
        for (int i = 0; i < availableDiceValues.length; i++) {
            if (!diceUsed[i] && availableDiceValues[i] == dieValue) {
                diceUsed[i] = true;
                return;
            }
        }
        throw new IllegalArgumentException("Die value not found: " + dieValue);
    }

    public int[] getDiceValues() {
        int[] values = new int[dice.length];
        for (int i = 0; i < dice.length; i++) {
            values[i] = dice[i].getValue();
        }
        return values;
    }

    public int[] getAvailableDiceValues() {
        // only return values for dice not yet used
        int[] values = new int[availableDiceValues.length];
        int count = 0;
        for (int i = 0; i < diceUsed.length; i++) {
            if (!diceUsed[i]) {
                values[count++] = availableDiceValues[i];
            }
        }
        return Arrays.copyOf(values, count);
    }

    public int piecesOnHomeBoard(int playerId) {
        BGParameters params = (BGParameters) getGameParameters();
        int count = 0;
        for (int i = 0; i < params.homeBoardSize; i++) {
            if (params.route == BGParameters.Route.CommonHalfA)
                count += getPiecesOnPoint(playerId, playerTrackMapping[playerId][params.boardSize - params.entryBoardSize - i - 1]);
            else
                count += getPiecesOnPoint(playerId, playerTrackMapping[playerId][params.boardSize - i - 1]);
        }
        return count;
    }

    public boolean allPiecesOnHomeBoard(int playerId) {
        BGParameters params = (BGParameters) getGameParameters();
        return piecesOnHomeBoard(playerId) == params.piecesPerPlayer - piecesBorneOff[playerId];
    }

    // returns the physical space on the board where the token is located
    public int locationOfToken(Token t) {
        for (int i = 0; i < counters.size(); i++) {
            if (counters.get(i).contains(t)) {
                // found the token, return the point
                return i;
            }
        }
        throw new IllegalArgumentException("Token not found in game state: " + t);
    }

    @Override
    protected BGGameState _copy(int playerId) {
        BGGameState copy = new BGGameState(gameParameters, getNPlayers());
        copy.piecesBorneOff = Arrays.copyOf(piecesBorneOff, piecesBorneOff.length);
        copy.blots = Arrays.copyOf(blots, blots.length);
        copy.dice = new Dice[dice.length];
        for (int i = 0; i < dice.length; i++) {
            copy.dice[i] = dice[i].copy();
        }
        copy.diceUsed = Arrays.copyOf(diceUsed, diceUsed.length);
        copy.availableDiceValues = Arrays.copyOf(availableDiceValues, availableDiceValues.length);

        copy.counters = new ArrayList<>();
        for (int i = 0; i < counters.size(); i++) {
            // individual counters are immutable, so we can avoid the overhead of copying them
            List<Token> playerCounters = new ArrayList<>(counters.get(i));
            copy.counters.add(playerCounters);
        }
        copy.playerTrackMapping = playerTrackMapping; // this is immutable, so we can just copy the reference
        copy.movedThisTurn = new ArrayList<>(movedThisTurn);
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
            // for a default heuristic, we count a piece borne off as worth 1.0
            // and otherwise we divide its current position by 24, so a point on the bar is worth zero
            double score = 0;
            BGParameters params = (BGParameters) getGameParameters();
            double length = params.boardSize;
            double maxPoints = params.piecesPerPlayer;
            int boardLength = playerTrackMapping[playerId].length;
            for (int i = 0; i < boardLength; i++) {
                // pieces are worth more the further they have moved
                score += getPiecesOnPoint(playerId, getPhysicalSpace(playerId, i)) * i / length;
                // opponent pieces are worth less the further they have moved
                score -= getPiecesOnPoint(1 - playerId, getPhysicalSpace(1 - playerId, i)) * (length - i) / length;
            }
            // borne off
            score += piecesBorneOff[playerId];
            score -= piecesBorneOff[1 - playerId];
            return score / maxPoints;  // to scale to 1.0 when all our pieces are borne off and none of the opponent
        }
        // if the game is over, return 1.0 for a win and 0.0 for a loss
        return playerResults[playerId].value;
    }

    /**
     * @param playerId - player observing the state.
     * @return the true score for the player, according to the game rules. May be 0 if there is no score in the game.
     */
    @Override
    public double getGameScore(int playerId) {
        // the score is the number of pieces borne off
        return piecesBorneOff[playerId];
    }


    @Override
    protected boolean _equals(Object o) {
        if (o instanceof BGGameState bgs) {
            return Arrays.equals(piecesBorneOff, bgs.piecesBorneOff) &&
                    Arrays.equals(blots, bgs.blots) &&
                    Arrays.equals(diceUsed, bgs.diceUsed) &&
                    Arrays.equals(availableDiceValues, bgs.availableDiceValues) &&
                    Arrays.equals(dice, bgs.dice) &&
                    counters.equals(bgs.counters) &&
                    movedThisTurn.equals(bgs.movedThisTurn) &&
                    Arrays.deepEquals(playerTrackMapping, bgs.playerTrackMapping);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(piecesBorneOff) + 31 *
                Arrays.hashCode(blots) + 31 * 31 *
                Arrays.hashCode(diceUsed) + 31 * 31 * 31 *
                Arrays.hashCode(dice) + 31 * 31 * 31 * 31 *
                Arrays.hashCode(availableDiceValues) - 31 * 255 *
                Objects.hash(counters, movedThisTurn) + 31 * 31 * 31 * 31 * 31 *
                Arrays.deepHashCode(playerTrackMapping) +
                super.hashCode();
    }

}

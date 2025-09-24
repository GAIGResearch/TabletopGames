package games.backgammon;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import games.GameType;

import java.util.*;

public class BGGameState extends AbstractGameState {

//    Backgammon involves moving 15 checkers around a board, aiming to be the first to "bear off" (remove) all your pieces before your opponent. Players move their pieces based on dice rolls, and a key strategy involves hitting opponent's pieces (blots) to send them to the bar, forcing them to re-enter the game.
//    Here's a more detailed breakdown of the rules:
//            1. Setup:
//    Board: The board has 24 triangular points, divided into four quadrants, with the "home" areas for each player on opposite sides.
//            Checkers: Each player starts with 15 checkers, initially placed in a specific configuration on the board.
//            Dice: Two dice are used, and each player has a cup to shake them.
//    Doubling Cube: A doubling cube is used to increase the stakes during the game.
//            2. Game Play:
//    Starting the Game:
//    Players roll one die each to determine who goes first, and the player with the higher roll starts.
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
//            3. Key Rules:
//    Open Points: A point that is not occupied by two or more of the opponent's checkers.
//    Closed Points: A point occupied by two or more of the opponent's checkers.
//    Doubling: Players can double the stakes at any time before rolling the dice, and the other player must either accept the double or forfeit the game.
//            Redoubles: After a double, the player who accepted the double can offer to redouble the stakes.
//    Moving Checkers: Players must move both numbers rolled, if possible.
//    Cannot Move: If a player cannot play all the numbers rolled, they must play as many as possible, starting with the larger number.
//            Bouncing: If a player cannot move a checker because of the opponent's closed points, they cannot move any other checkers until the opponent's points become open.

    // the first index is the player, the second index is the point on the board
    // the arrays for each player are in reverse order..i.e each is from the perspective of the player
    // with [0] being the first point (on the player's home board) and [23] being the last (24th) point (on the opponent's home board)
    // so a player's home board is [0] through [5]
    protected int[][] piecesPerPoint;
    protected int[] piecesOnBar;  // currently out the game and need to be put into play as first moves
    protected int[] piecesBorneOff;

    protected Dice[] dice;
    protected boolean[] diceUsed;

    protected int[] blots;

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
        // Let's create one Token per piece (while we're not using Tokens to record the pieces in game state, this may be helpful to something tracking the number of pieces)
        List<Component> tokens = new ArrayList<>();
        for (int player = 0; player < 2; player++) {
            for (int i = 0; i < piecesPerPoint[player].length; i++) {
                tokens.add(new Token("Piece for "  + player + " on point " + i));
            }
        }
        tokens.addAll(Arrays.stream(dice).toList());

        return tokens;
    }

    public int getPiecesOnPoint(int playerId, int point) {
        return piecesPerPoint[playerId][point];
    }
    public int getPiecesOnBar(int playerId) {
        return piecesOnBar[playerId];
    }
    public int[] getPlayerPieces(int playerId) {
        return piecesPerPoint[playerId].clone();
    }

    public int getPiecesBorneOff(int playerId) {
        return piecesBorneOff[playerId];
    }
    /**
     * -1 indicates the bar (from), or borne off (to)
     */
    public void movePiece(int playerId, int from, int to) {
        int boardLength = piecesPerPoint[0].length;
        int ownPiecesOnStartSpace = from < 0 ? piecesOnBar[playerId] : piecesPerPoint[playerId][from];
        int opponentPiecesOnEndSpace = to < 0 ? 0 : piecesPerPoint[1 - playerId][boardLength - to - 1];
        if (ownPiecesOnStartSpace <= 0)
            throw new IllegalArgumentException("No pieces on the from point");
        if (opponentPiecesOnEndSpace > 1)
            throw new IllegalArgumentException("Cannot move to a point occupied by two or more opponent pieces");


        // actual hit of opponent pieces is done in MovePiece class
        if (from < 0)
            piecesOnBar[playerId]--;
        else
            piecesPerPoint[playerId][from]--;

        if (to < 0)
            piecesBorneOff[playerId]++;
        else
            piecesPerPoint[playerId][to]++;
    }

    public void rollDice() {
        for (Dice die : dice) {
            die.roll(rnd);
        }
        Arrays.fill(diceUsed, false);
    }

    // for testing only
    public void setDiceValues(int[] values) {
        for (int i = 0; i < dice.length; i++) {
            dice[i].setValue(values[i]);
        }
        Arrays.fill(diceUsed, false);
    }

    public void useDiceValue(int dieValue) {
        for (int i = 0; i < dice.length; i++) {
            if (!diceUsed[i] && dice[i].getValue() == dieValue) {
                diceUsed[i] = true;
                return;
            }
        }
        throw new IllegalArgumentException("Die value not found");
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
        int[] values = new int[dice.length];
        int count = 0;
        for (int i = 0; i < dice.length; i++) {
            if (!diceUsed[i]) {
                values[count++] = dice[i].getValue();
            }
        }
        return Arrays.copyOf(values, count);
    }

    public int piecesOnHomeBoard(int playerId) {
        BGParameters params = (BGParameters) getGameParameters();
        int count = 0;
        for (int i = 0; i < params.homeBoardSize; i++) {
            count += piecesPerPoint[playerId][i];
        }
        return count;
    }

    public boolean allPiecesOnHomeBoard(int playerId) {
        BGParameters params = (BGParameters) getGameParameters();
        if (piecesOnBar[playerId] > 0)
            return false;
        for (int i = params.homeBoardSize; i < params.boardSize; i++) {
            if (piecesPerPoint[playerId][i] > 0) {
                return false;
            }
        }
        return true;
    }

    public void movePieceToBar(int playerId, int point) {
        if (piecesPerPoint[playerId][point] <= 0) {
            throw new IllegalArgumentException("No pieces on the point");
        }
        piecesPerPoint[playerId][point]--;
        piecesOnBar[playerId]++;
        blots[playerId]++;
    }

    @Override
    protected BGGameState _copy(int playerId) {
        BGGameState copy = new BGGameState(gameParameters, getNPlayers());
        copy.piecesPerPoint = new int[piecesPerPoint.length][];
        for (int i = 0; i < piecesPerPoint.length; i++) {
            copy.piecesPerPoint[i] = Arrays.copyOf(piecesPerPoint[i], piecesPerPoint[i].length);
        }
        copy.piecesOnBar = Arrays.copyOf(piecesOnBar, piecesOnBar.length);
        copy.piecesBorneOff = Arrays.copyOf(piecesBorneOff, piecesBorneOff.length);
        copy.blots = Arrays.copyOf(blots, blots.length);
        copy.dice = new Dice[dice.length];
        for (int i = 0; i < dice.length; i++) {
            copy.dice[i] = dice[i].copy();
        }
        copy.diceUsed = Arrays.copyOf(diceUsed, diceUsed.length);
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
            int boardLength = piecesPerPoint[0].length;
            for (int i = 0; i < piecesPerPoint[playerId].length; i++) {
                // pieces are worth more the closer they are to the home board (i.e. the lower the index)
                score += piecesPerPoint[playerId][boardLength - i - 1] * i / 24.0;
                // opponent pieces are worth less the closer they are to their home board (i.e. the lower the index)
                score -= piecesPerPoint[1 - playerId][i] * i / 24.0;
            }
            // borne off
            score += piecesBorneOff[playerId];
            score -= piecesBorneOff[1 - playerId];
            return score / 15.0;  // to scale to 1.0 when all our pieces are borne off and none of the opponent
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
                    Arrays.equals(piecesOnBar, bgs.piecesOnBar) &&
                    Arrays.equals(blots, bgs.blots) &&
                    Arrays.equals(diceUsed, bgs.diceUsed) &&
                    Arrays.equals(dice, bgs.dice) &&
                    Arrays.deepEquals(piecesPerPoint, bgs.piecesPerPoint);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(piecesBorneOff) + 31 *
                Arrays.hashCode(piecesOnBar) + 31 * 31 *
                Arrays.hashCode(blots) + 31 * 31 * 31 *
                Arrays.hashCode(diceUsed) + 31 * 31 * 31 * 31 *
                Arrays.hashCode(dice) + 31 * 31 * 31 * 31 * 31 *
                Arrays.deepHashCode(piecesPerPoint) +
                super.hashCode();
    }

}

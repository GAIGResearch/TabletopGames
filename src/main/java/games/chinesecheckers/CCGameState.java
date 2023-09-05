package games.chinesecheckers;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import games.GameType;
import games.chinesecheckers.components.Peg;
import games.chinesecheckers.components.StarBoard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CCGameState extends AbstractGameState {

    public int PLAYER_PEGS = 10;
    StarBoard starBoard;

    public CCGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        if (nPlayers == 5) {
            throw new AssertionError("Chinese Checkers does not support 5 players");
        }
    }

    public StarBoard getStarBoard()
    {
        return starBoard;
    }

    @Override
    protected GameType _getGameType() {
        return GameType.ChineseCheckers;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(starBoard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        CCGameState copy = new CCGameState(gameParameters, getNPlayers());
        copy.starBoard = starBoard.copy();
        copy.PLAYER_PEGS = PLAYER_PEGS;

        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            return new CCHeuristic().evaluateState(this, playerId);
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    @Override
    public double getGameScore(int playerId) {
        return playerResults[playerId].value;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CCGameState)) return false;
        if (!super.equals(o)) return false;
        CCGameState that = (CCGameState) o;
        return PLAYER_PEGS == that.PLAYER_PEGS && Objects.equals(starBoard, that.starBoard);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), starBoard, PLAYER_PEGS);
        result = 31 * result;
        return result;
    }

    public Peg.Colour getPlayerColour(int player) {
        if (nPlayers == 2) {
            return Peg.Colour.values()[player * 3];
        }
        else if (nPlayers == 3) {
            return Peg.Colour.values()[player * 2];
        }
        else if (nPlayers == 4) {
            int[] orderForFourPlayers = {0, 2, 3, 5};
            return Peg.Colour.values()[orderForFourPlayers[player]];
        }
        else if (nPlayers == 6) {
            return Peg.Colour.values()[player];
        }
        else {
            throw new AssertionError("Chinese Checkers does not support player " + player);
        }
    }
}

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

    StarBoard starBoard;

    public CCGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        // GameType only supports a min/max range of players...so special case for 5 players
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
        return new ArrayList<>() {{
            add(starBoard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        CCGameState copy = new CCGameState(gameParameters, getNPlayers());
        copy.starBoard = starBoard.copy();

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
        return Objects.equals(starBoard, that.starBoard);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), starBoard);
        result = 31 * result;
        return result;
    }

    public Peg.Colour getPlayerColour(int player) {
        CCParameters params = (CCParameters) gameParameters;
        int nPlayers = getNPlayers();
        return params.playerColours.get(nPlayers)[player];
    }
}

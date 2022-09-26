package games.cantstop;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import games.GameType;

import java.util.*;

public class CantStopGameState extends AbstractGameState {

    // The core game state is made up of the 11 tracks (2 through 12), with the positions of each player,
    // and the temporary markers if in the middle of someone's turn

    private boolean[] completedColumns;
    private List<int[]> playerMarkerPositions;
    private Map<Integer, Integer> temporaryMarkerPositions;

    private CantStopGameState(CantStopGameState copyFrom) {
        // used by copy method only
        super(copyFrom.gameParameters, GameType.CantStop);
        // TurnOrder will be copied later
        completedColumns = copyFrom.completedColumns.clone();
        for (int[] playerMarkers : playerMarkerPositions) {
            playerMarkerPositions.add(playerMarkers.clone());
        }
        temporaryMarkerPositions.putAll(copyFrom.temporaryMarkerPositions);
    }

    public CantStopGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new CantStopTurnOrder(nPlayers), GameType.CantStop);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return Collections.emptyList();  // TODO: Add stuff here if ever needed
    }

    @Override
    protected CantStopGameState _copy(int playerId) {
        return new CantStopGameState(this);
        // substance dealt with in private constructor above
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
        // have not bothered to implement one.
    }

    @Override
    public double getGameScore(int playerId) {
        // this is simply the number of columns we have topped out on
        int score = 0;
        CantStopParameters params = (CantStopParameters) getGameParameters();
        for (int n = 2; n <= 12; n++) {
            if (completedColumns[n]) {
                if (playerMarkerPositions.get(playerId)[n] == params.maxValue(n))
                    score++;
            }
        }
        return score;
    }

    @Override
    protected void _reset() {
        completedColumns = new boolean[12];
        playerMarkerPositions = new ArrayList<>();
        temporaryMarkerPositions = new HashMap<>();
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof CantStopGameState) {
            CantStopGameState other = (CantStopGameState) o;
            return Arrays.equals(completedColumns, other.completedColumns) &&
                    temporaryMarkerPositions.equals(other.temporaryMarkerPositions) &&
                    playerMarkerPositions.equals(other.playerMarkerPositions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(gameStatus, gamePhase, turnOrder, gameParameters, playerMarkerPositions, temporaryMarkerPositions);
        hash = hash * 31 + Arrays.hashCode(playerResults);
        hash = hash * 31 + Arrays.hashCode(completedColumns);
        return hash;
    }
}

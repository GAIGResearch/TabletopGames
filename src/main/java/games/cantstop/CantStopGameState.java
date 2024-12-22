package games.cantstop;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Dice;
import core.interfaces.IPrintable;
import games.GameType;

import java.util.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class CantStopGameState extends AbstractGameState implements IPrintable {

    // The core game state is made up of the 11 tracks (2 through 12), with the positions of each player,
    // and the temporary markers if in the middle of someone's turn

    protected boolean[] completedColumns;
    protected int[][] playerMarkerPositions;
    protected Map<Integer, Integer> temporaryMarkerPositions;
    protected List<Dice> dice;

    private CantStopGameState(CantStopGameState copyFrom) {
        // used by copy method only
        super(copyFrom.gameParameters.copy(), copyFrom.getNPlayers());
        // TurnOrder will be copied later
        completedColumns = copyFrom.completedColumns.clone();
        playerMarkerPositions = new int[copyFrom.getNPlayers()][];
        for (int p = 0; p < copyFrom.getNPlayers(); p++)
            playerMarkerPositions[p] = copyFrom.playerMarkerPositions[p].clone();
        temporaryMarkerPositions = new HashMap<>();
        temporaryMarkerPositions.putAll(copyFrom.temporaryMarkerPositions);
        dice = copyFrom.dice.stream().map(Dice::copy).collect(toList());
    }

    public CantStopGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.CantStop;
    }

    public void rollDice() {
        dice.forEach(d -> d.roll(rnd));
    }

    /**
     * True if the specified number track is complete and has been scored
     **/
    public boolean isTrackComplete(int n) {
        return completedColumns[n];
    }

    public void moveMarker(int n) {
        if (!isTrackComplete(n)) {
            int currentPosition = temporaryMarkerPositions.getOrDefault(n, playerMarkerPositions[getCurrentPlayer()][n]);
            temporaryMarkerPositions.put(n, currentPosition + 1);
        }
    }

    /**
     * Returns the current dice roll as an int[2]
     **/
    public int[] getDice() {
        return dice.stream().mapToInt(Dice::getValue).toArray();
    }

    public void setDice(int[] numbers) {
        if (numbers.length != ((CantStopParameters) getGameParameters()).DICE_NUMBER)
            throw new IllegalArgumentException("Invalid number of dice results");
        for (int i = 0; i < numbers.length; i++) {
            dice.get(i).setValue(numbers[i]);
        }
    }

    /**
     * The current marker position for the specified player and number track
     **/
    public int getMarkerPosition(int number, int player) {
        return playerMarkerPositions[player][number];
    }

    /**
     * The temporary marker position for the current player on the specified number track
     **/
    public int getTemporaryMarkerPosition(int number) {
        return temporaryMarkerPositions.getOrDefault(number, 0);
    }

    /**
     * The numbers of the tracks that have had markers moved in the current turn
     **/
    public List<Integer> getMarkersMoved() {
        return new ArrayList<>(temporaryMarkerPositions.keySet());
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
        if (isNotTerminal()) {
            return getGameScore(playerId) / ((CantStopParameters) gameParameters).COLUMNS_TO_WIN;
        } else
            return getPlayerResults()[playerId].value;
        // have not bothered to implement one.
    }

    @Override
    public double getGameScore(int playerId) {
        // this is simply the number of columns we have topped out on
        int score = 0;
        CantStopParameters params = (CantStopParameters) getGameParameters();
        for (int n = 2; n <= 12; n++) {
            if (completedColumns[n]) {
                if (playerMarkerPositions[playerId][n] >= params.maxValue(n))
                    score++;
            }
        }
        return score;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof CantStopGameState) {
            CantStopGameState other = (CantStopGameState) o;
            return Arrays.equals(completedColumns, other.completedColumns) &&
                    temporaryMarkerPositions.equals(other.temporaryMarkerPositions) &&
                    dice.equals(other.dice) &&
                    Arrays.deepEquals(playerMarkerPositions, other.playerMarkerPositions);
        }
        // rnd is deliberately excluded
        return false;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(temporaryMarkerPositions, dice);
        hash = hash * 31 + super.hashCode();
        hash = hash * 31 + Arrays.hashCode(completedColumns);
        hash = hash * 31 + Arrays.deepHashCode(playerMarkerPositions);
        // rnd is deliberately excluded
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int result = Objects.hash(gameParameters);
        sb.append(result).append("|");
        result = Objects.hash(getAllComponents());
        sb.append(result).append("|");
        result = Objects.hash(gameStatus);
        sb.append(result).append("|");
        result = Objects.hash(gamePhase);
        sb.append(result).append("|");
        result = Arrays.hashCode(playerResults);
        sb.append(result).append("|*|");
        result = Arrays.hashCode(completedColumns);
        sb.append(result).append("|");
        result = Objects.hashCode(playerMarkerPositions);
        sb.append(result).append("|");
        result = Objects.hashCode(temporaryMarkerPositions);
        sb.append(result).append("|");
        result = Objects.hashCode(dice);
        sb.append(result).append("|");

        return sb.toString();
    }

    @Override
    public void printToConsole() {
        StringBuilder sb = new StringBuilder();
        CantStopParameters params = (CantStopParameters) gameParameters;
        sb.append("--------------------------------------------------\n");
        sb.append("Scores:\t");
        for (int p = 0; p < getNPlayers(); p++)
            sb.append(getGameScore(p)).append("\t");
        sb.append("\n");
        sb.append(String.format("Player %d to move. Phase %s%n%n", getCurrentPlayer(), getGamePhase()));
        sb.append("Dice Values: ")
                .append(dice.stream().map(d -> String.valueOf(d.getValue())).collect(joining(", ")))
                .append("\n");
        sb.append("Number\t");
        for (int p = 0; p < getNPlayers(); p++)
            sb.append("P").append(p).append("\t\t");
        sb.append("Max\n");
        for (int n = 2; n <= 12; n++) {
            sb.append(String.format("%2d :\t", n));
            if (isTrackComplete(n))
                sb.append(" COMPLETED");
            else {
                for (int p = 0; p < getNPlayers(); p++) {
                    if (p == getCurrentPlayer() && temporaryMarkerPositions.containsKey(n))
                        sb.append(String.format("%2d/%d\t\t", playerMarkerPositions[p][n], temporaryMarkerPositions.get(n)));
                    else
                        sb.append(String.format("%2d\t\t", playerMarkerPositions[p][n]));
                }
                sb.append(params.maxValue(n));
            }
            sb.append("\n");
        }
        sb.append("--------------------------------------------------\n");
        System.out.println(sb);
    }
}

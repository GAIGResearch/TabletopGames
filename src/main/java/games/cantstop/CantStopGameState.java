package games.cantstop;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Dice;
import core.interfaces.IPrintable;
import core.turnorders.StandardTurnOrder;
import games.GameType;

import java.util.*;

import static java.util.stream.Collectors.*;

public class CantStopGameState extends AbstractGameState implements IPrintable {

    // The core game state is made up of the 11 tracks (2 through 12), with the positions of each player,
    // and the temporary markers if in the middle of someone's turn

    protected boolean[] completedColumns;
    protected List<int[]> playerMarkerPositions;
    protected Map<Integer, Integer> temporaryMarkerPositions;
    protected List<Dice> dice;
    protected Random rnd;

    private CantStopGameState(CantStopGameState copyFrom) {
        // used by copy method only
        super(copyFrom.gameParameters, GameType.CantStop);
        // TurnOrder will be copied later
        completedColumns = copyFrom.completedColumns.clone();
        playerMarkerPositions = new ArrayList<>();
        for (int[] playerMarkers : copyFrom.playerMarkerPositions) {
            playerMarkerPositions.add(playerMarkers.clone());
        }
        temporaryMarkerPositions = new HashMap<>();
        temporaryMarkerPositions.putAll(copyFrom.temporaryMarkerPositions);
        dice = copyFrom.dice.stream().map(Dice::copy).collect(toList());
        if (rnd == null) {
            rnd = new Random(System.currentTimeMillis());
        } else {
            rnd = new Random(rnd.nextLong());
        }
    }

    public void rollDice() {
        dice.forEach(d -> d.roll(rnd));
    }

    public boolean trackComplete(int n) {
        return completedColumns[n];
    }

    public void moveMarker(int n) {
        if (!trackComplete(n)) {
            int currentPosition = temporaryMarkerPositions.getOrDefault(n, playerMarkerPositions.get(getCurrentPlayer())[n]);
            temporaryMarkerPositions.put(n, currentPosition + 1);
        }
    }

    public void setDice(int[] numbers) {
        if (numbers.length != ((CantStopParameters) getGameParameters()).DICE_NUMBER)
            throw new IllegalArgumentException("Invalid number of dice results");
        for (int i = 0; i < numbers.length; i++) {
            dice.get(i).setValue(numbers[i]);
        }
    }
    public int[] getDice() {
        return dice.stream().mapToInt(Dice::getValue).toArray();
    }

    public int getMarkerPosition(int number, int player) {
        return playerMarkerPositions.get(player)[number];
    }

    public int getTemporaryMarkerPosition(int number)  {
        return temporaryMarkerPositions.getOrDefault(number, 0);
    }

    public List<Integer> getMarkersMoved() {
        return new ArrayList<>(temporaryMarkerPositions.keySet());
    }

    public CantStopGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new StandardTurnOrder(nPlayers), GameType.CantStop);
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
                if (playerMarkerPositions.get(playerId)[n] >= params.maxValue(n))
                    score++;
            }
        }
        return score;
    }

    @Override
    protected void _reset() {
        CantStopParameters params = (CantStopParameters) getGameParameters();
        completedColumns = new boolean[13];
        playerMarkerPositions = new ArrayList<>();
        temporaryMarkerPositions = new HashMap<>();
        for (int p = 0; p < getNPlayers(); p++) {
            playerMarkerPositions.add(new int[13]);
        }
        dice = new ArrayList<>();
        for (int i = 0; i < params.DICE_NUMBER; i++) {
            dice.add(new Dice(params.DICE_SIDES));
        }
        if (rnd == null) {
            rnd = new Random(System.currentTimeMillis());
        } else {
            rnd = new Random(rnd.nextLong());
        }
        gamePhase = CantStopGamePhase.Decision;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof CantStopGameState) {
            CantStopGameState other = (CantStopGameState) o;
            return Arrays.equals(completedColumns, other.completedColumns) &&
                    temporaryMarkerPositions.equals(other.temporaryMarkerPositions) &&
                    dice.equals(other.dice) &&
                    playerMarkerPositions.equals(other.playerMarkerPositions);
        }
        // rnd is deliberately excluded
        return false;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(gameStatus, gamePhase, turnOrder, gameParameters, playerMarkerPositions,
                temporaryMarkerPositions, dice);
        hash = hash * 31 + Arrays.hashCode(playerResults);
        hash = hash * 31 + Arrays.hashCode(completedColumns);
        // rnd is deliberately excluded
        return hash;
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
            if (trackComplete(n))
                sb.append(" COMPLETED");
            else {
                for (int p = 0; p < getNPlayers(); p++) {
                    if (p == getCurrentPlayer() && temporaryMarkerPositions.containsKey(n))
                        sb.append(String.format("%2d/%d\t\t", playerMarkerPositions.get(p)[n], temporaryMarkerPositions.get(n)));
                    else
                        sb.append(String.format("%2d\t\t", playerMarkerPositions.get(p)[n]));
                }
                sb.append(params.maxValue(n));
            }
            sb.append("\n");
        }
        sb.append("--------------------------------------------------\n");
        System.out.println(sb);
    }
}

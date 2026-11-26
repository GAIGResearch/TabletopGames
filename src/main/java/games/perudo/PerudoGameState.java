package games.perudo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import games.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static games.perudo.PerudoParameters.PACO;

/**
 * <p>State tracked:</p>
 * <ul>
 *     <li>dice - dice[p][f - 1] is the number of player p's dice showing face f this round (the order of the dice
 *     under a cup means nothing). Each player sees only their own; how many dice each player has (the row's total)
 *     is public</li>
 *     <li>bidQuantity, bidFace - the current bid ("bidQuantity dice showing bidFace"); bidFace is 0 before the
 *     round's opening bid</li>
 *     <li>bidder - the player who made the current bid, or -1</li>
 *     <li>nextRoundStarter - the player who will start the next round, or -1</li>
 *     <li>eliminationRound - the round in which each player lost their last die, or -1 while they are still in</li>
 *     <li>palificoRound - whether this round is a Palifico round (PerudoParameters.palifico)</li>
 *     <li>palificoUsed - whether each player has already had their Palifico round</li>
 * </ul>
 */
public class PerudoGameState extends AbstractGameState {

    int[][] dice;
    int bidQuantity;
    int bidFace;
    int bidder = -1;
    int nextRoundStarter = -1;
    int[] eliminationRound;
    boolean palificoRound;
    boolean[] palificoUsed;

    public PerudoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Perudo;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>();
    }

    public PerudoParameters getParams() {
        return (PerudoParameters) gameParameters;
    }

    /**
     * @return the number of the player's dice showing the face (1 to PerudoParameters.nFaces)
     */
    public int getDiceShowing(int player, int face) {
        return dice[player][face - 1];
    }

    /**
     * @return a copy of the player's count of dice per face, indexed by face - 1
     */
    public int[] getDice(int player) {
        return dice[player].clone();
    }

    public int getNDice(int player) {
        return Arrays.stream(dice[player]).sum();
    }

    public int getTotalDice() {
        int total = 0;
        for (int p = 0; p < getNPlayers(); p++)
            total += getNDice(p);
        return total;
    }

    public int getBidQuantity() {
        return bidQuantity;
    }

    public int getBidFace() {
        return bidFace;
    }

    public boolean hasBid() {
        return bidFace != 0;
    }

    public int getBidder() {
        return bidder;
    }

    public int getNextRoundStarter() {
        return nextRoundStarter;
    }

    public int getEliminationRound(int player) {
        return eliminationRound[player];
    }

    public boolean isPalificoRound() {
        return palificoRound;
    }

    public boolean hasHadPalifico(int player) {
        return palificoUsed[player];
    }

    public void setPalificoRound(boolean palificoRound) {
        this.palificoRound = palificoRound;
    }

    public void setPalificoUsed(int player, boolean used) {
        palificoUsed[player] = used;
    }

    /**
     * @return the number of dice on the table that count towards a bid on the face
     */
    public int countMatching(int face) {
        int count = 0;
        for (int p = 0; p < getNPlayers(); p++) {
            count += getDiceShowing(p, face);
            // Pacos are wild, except in a Palifico round
            if (face != PACO && !palificoRound)
                count += getDiceShowing(p, PACO);
        }
        return count;
    }

    public void setDice(int player, int[] faceCounts) {
        if (faceCounts.length != getParams().nFaces)
            throw new IllegalArgumentException("Expected " + getParams().nFaces + " face counts, got " + faceCounts.length);
        dice[player] = faceCounts.clone();
    }

    public void setBid(int quantity, int face, int player) {
        bidQuantity = quantity;
        bidFace = face;
        bidder = player;
    }

    public void clearBid() {
        bidQuantity = 0;
        bidFace = 0;
        bidder = -1;
    }

    public void setNextRoundStarter(int player) {
        nextRoundStarter = player;
    }

    public void setEliminationRound(int player, int round) {
        eliminationRound[player] = round;
    }

    public void removeDie(int player) {
        // Which face goes does not matter: every die is rolled again before the next round's bidding
        for (int f = 0; f < dice[player].length; f++) {
            if (dice[player][f] > 0) {
                dice[player][f]--;
                return;
            }
        }
        throw new IllegalStateException("Player " + player + " has no dice to lose");
    }

    public void addDie(int player) {
        // Its face does not matter: every die is rolled again before the next round's bidding
        dice[player][0]++;
    }

    /**
     * @return every player's dice, for the reveal when a challenge is called: "player 0: 1 3 3 5; player 1: ..."
     */
    public String diceOnTable() {
        StringJoiner players = new StringJoiner("; ");
        for (int p = 0; p < getNPlayers(); p++) {
            if (getNDice(p) == 0) continue;
            StringJoiner faces = new StringJoiner(" ", "player " + p + ": ", "");
            for (int f = 0; f < dice[p].length; f++)
                for (int d = 0; d < dice[p][f]; d++)
                    faces.add(String.valueOf(f + 1));
            players.add(faces.toString());
        }
        return players.toString();
    }

    @Override
    protected PerudoGameState _copy(int playerId) {
        PerudoGameState copy = new PerudoGameState(gameParameters, getNPlayers());
        copy.dice = new int[dice.length][];
        for (int p = 0; p < dice.length; p++)
            copy.dice[p] = dice[p].clone();
        copy.bidQuantity = bidQuantity;
        copy.bidFace = bidFace;
        copy.bidder = bidder;
        copy.nextRoundStarter = nextRoundStarter;
        copy.eliminationRound = eliminationRound.clone();
        copy.palificoRound = palificoRound;
        copy.palificoUsed = palificoUsed.clone();
        return copy;
    }

    /**
     * Rolls again the dice of every player other than the observer, keeping how many dice each has.
     */
    @Override
    public void redeterminise(int playerId) {
        int nFaces = getParams().nFaces;
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId) continue;
            int n = getNDice(p);
            int[] rolled = new int[nFaces];
            for (int d = 0; d < n; d++)
                rolled[redeterminisationRnd.nextInt(nFaces)]++;
            dice[p] = rolled;
        }
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            // the share of the dice on the table that the player holds
            int total = getTotalDice();
            return total == 0 ? 0 : (double) getNDice(playerId) / total;
        }
        return getPlayerResults()[playerId].value;
    }

    /**
     * The number of dice the player holds.
     */
    @Override
    public double getGameScore(int playerId) {
        return getNDice(playerId);
    }

    /**
     * The later a player was eliminated, the better they rank.
     */
    @Override
    public double getTiebreak(int playerId, int tier) {
        if (tier > 1) return 0;
        // players still in all tie here
        return eliminationRound[playerId] == -1 ? Integer.MAX_VALUE : eliminationRound[playerId];
    }

    @Override
    public int getTiebreakLevels() {
        return 1;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof PerudoGameState that)) return false;
        return bidQuantity == that.bidQuantity && bidFace == that.bidFace && bidder == that.bidder &&
                nextRoundStarter == that.nextRoundStarter && palificoRound == that.palificoRound &&
                Arrays.deepEquals(dice, that.dice) && Arrays.equals(eliminationRound, that.eliminationRound) &&
                Arrays.equals(palificoUsed, that.palificoUsed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bidQuantity, bidFace, bidder, nextRoundStarter, palificoRound,
                Arrays.deepHashCode(dice), Arrays.hashCode(eliminationRound), Arrays.hashCode(palificoUsed));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < getNPlayers(); p++)
            sb.append("Player ").append(p).append(": ").append(Arrays.toString(dice[p])).append("\n");
        sb.append("Bid: ").append(hasBid() ? bidQuantity + " x " + bidFace + " by player " + bidder : "none");
        return sb.toString();
    }
}

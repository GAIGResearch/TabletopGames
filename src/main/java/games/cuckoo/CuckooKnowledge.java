package games.cuckoo;

import java.util.Arrays;

/**
 * Which players' cards each player knows this round. Each player holds at most one card, so knowing a player's card
 * is all there is to know about them. Every player knows their own card.
 */
public class CuckooKnowledge {

    // known[observer][holder]: whether the observer knows the holder's card
    private final boolean[][] known;

    public CuckooKnowledge(int nPlayers) {
        known = new boolean[nPlayers][nPlayers];
        for (int p = 0; p < nPlayers; p++)
            known[p][p] = true;
    }

    private CuckooKnowledge(boolean[][] known) {
        this.known = known;
    }

    public boolean knows(int observer, int holder) {
        return known[observer][holder];
    }

    /**
     * Records a swap between two players. Every observer knows where the two cards went, so what they knew of one
     * player's card they now know of the other's. So each of the two knows the card they gave away, and the card they received.
     */
    public void exchanged(int player, int neighbour) {
        for (boolean[] observer : known) {
            boolean knewPlayers = observer[player];
            observer[player] = observer[neighbour];
            observer[neighbour] = knewPlayers;
        }
        known[player][player] = known[neighbour][neighbour] = true;
    }

    /**
     * Records that every player has seen the holder's card (a King shown to refuse a swap).
     */
    public void revealed(int holder) {
        for (boolean[] observer : known)
            observer[holder] = true;
    }

    /**
     * Records that the holder's card was replaced by one only they have seen (the dealer's cut).
     */
    public void replaced(int holder) {
        for (int p = 0; p < known.length; p++)
            known[p][holder] = p == holder;
    }

    public CuckooKnowledge copy() {
        boolean[][] copy = new boolean[known.length][];
        for (int p = 0; p < known.length; p++)
            copy[p] = known[p].clone();
        return new CuckooKnowledge(copy);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CuckooKnowledge that && Arrays.deepEquals(known, that.known);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(known);
    }

    @Override
    public String toString() {
        return Arrays.deepToString(known);
    }
}

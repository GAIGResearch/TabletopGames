package games.gofish;

import core.components.Deck;
import core.components.FrenchCard;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * For each player, the ranks they are publicly known not to hold. Redeterminisation uses {@link #permits} so that a
 * player is never dealt a card of a rank they are known not to hold.
 */
public class GoFishKnownVoids {

    // one bit per rank (FrenchCard.number) for each player
    private final int[] voids;

    public GoFishKnownVoids(int nPlayers) {
        voids = new int[nPlayers];
    }

    private GoFishKnownVoids(int[] voids) {
        this.voids = voids;
    }

    public Set<Integer> get(int player) {
        Set<Integer> ranks = new TreeSet<>();
        for (int rank = 0; rank < Integer.SIZE; rank++)
            if (isVoid(player, rank))
                ranks.add(rank);
        return ranks;
    }

    public boolean isVoid(int player, int rank) {
        return (voids[player] >> rank & 1) == 1;
    }

    public void record(int player, int rank) {
        voids[player] |= 1 << rank;
    }

    /**
     * The player has been seen to receive cards of the rank.
     */
    public void received(int player, int rank) {
        voids[player] &= ~(1 << rank);
    }

    /**
     * Forgets what is known of the player's hand, when they draw a card nobody else sees.
     */
    public void drew(int player) {
        voids[player] = 0;
    }

    /**
     * Whether redeterminisation may put the card in the deck.
     */
    public boolean permits(Deck<FrenchCard> deck, FrenchCard card) {
        // any card may go in the draw deck, which has no owner
        return deck.getOwnerId() < 0 || !isVoid(deck.getOwnerId(), card.number);
    }

    public GoFishKnownVoids copy() {
        return new GoFishKnownVoids(voids.clone());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GoFishKnownVoids that && Arrays.equals(voids, that.voids);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(voids);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < voids.length; p++)
            sb.append(p == 0 ? "" : ", ").append(get(p));
        return sb.toString();
    }
}

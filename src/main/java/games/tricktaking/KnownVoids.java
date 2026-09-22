package games.tricktaking;

import core.components.Deck;
import core.components.FrenchCard;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * For each player, the suits they are publicly known not to hold, from having failed to follow suit to a trick.
 * Redeterminisation uses {@link #permits} so that a player is never dealt a card of a suit they are known to be
 * void in.
 */
public class KnownVoids {

    private final List<Set<FrenchCard.Suite>> voids;

    public KnownVoids(int nPlayers) {
        voids = new ArrayList<>(nPlayers);
        for (int p = 0; p < nPlayers; p++)
            voids.add(EnumSet.noneOf(FrenchCard.Suite.class));
    }

    private KnownVoids(List<Set<FrenchCard.Suite>> voids) {
        this.voids = voids;
    }

    /**
     * The suits the player is known to be void in (a live view).
     */
    public Set<FrenchCard.Suite> get(int player) {
        return voids.get(player);
    }

    /**
     * Records what the player's card, about to be played to the trick, reveals: if it does not follow the suit led,
     * the player holds none of that suit.
     */
    public void record(int player, Trick trick, FrenchCard card) {
        FrenchCard.Suite lead = trick.getLeadSuit();
        if (lead != null && trick.getOrder().suitOf(card) != lead)
            voids.get(player).add(lead);
    }

    /**
     * Forgets all known voids (at a new deal).
     */
    public void clear() {
        for (Set<FrenchCard.Suite> v : voids)
            v.clear();
    }

    /**
     * Whether redeterminisation may put the card in the deck: always for a deck with no owner, otherwise only if the
     * owner is not known to be void in the card's suit.
     */
    public boolean permits(Deck<FrenchCard> deck, FrenchCard card) {
        return permits(deck, card.suite);
    }

    /**
     * As {@link #permits(Deck, FrenchCard)}, but for a card belonging to the suit the order gives it.
     */
    public BiPredicate<Deck<FrenchCard>, FrenchCard> permits(CardOrder order) {
        return (deck, card) -> permits(deck, order.suitOf(card));
    }

    private boolean permits(Deck<FrenchCard> deck, FrenchCard.Suite suit) {
        return deck.getOwnerId() < 0 || !voids.get(deck.getOwnerId()).contains(suit);
    }

    public KnownVoids copy() {
        List<Set<FrenchCard.Suite>> copy = new ArrayList<>(voids.size());
        for (Set<FrenchCard.Suite> v : voids)
            copy.add(v.isEmpty() ? EnumSet.noneOf(FrenchCard.Suite.class) : EnumSet.copyOf(v));
        return new KnownVoids(copy);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KnownVoids that && voids.equals(that.voids);
    }

    @Override
    public int hashCode() {
        // from suit ordinals, as an enum's own hashCode differs between runs
        int result = 1;
        for (Set<FrenchCard.Suite> v : voids) {
            int bits = 0;
            for (FrenchCard.Suite s : v)
                bits |= 1 << s.ordinal();
            result = 31 * result + bits;
        }
        return result;
    }

    @Override
    public String toString() {
        return voids.toString();
    }
}

package games.tricktaking;

import core.components.Component;
import core.components.Deck;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * For each player, the suits they are publicly known not to hold, from having failed to follow suit to a trick.
 * Redeterminisation uses {@link #permits} so that a player is never dealt a card of a suit they are known to be
 * void in.
 *
 * @param <S> the type of suit
 */
public class KnownVoids<S extends Enum<S>> {

    private final Class<S> suitClass;
    private final List<Set<S>> voids;

    public KnownVoids(int nPlayers, Class<S> suitClass) {
        this.suitClass = suitClass;
        voids = new ArrayList<>(nPlayers);
        for (int p = 0; p < nPlayers; p++)
            voids.add(EnumSet.noneOf(suitClass));
    }

    private KnownVoids(Class<S> suitClass, List<Set<S>> voids) {
        this.suitClass = suitClass;
        this.voids = voids;
    }

    /**
     * The suits the player is known to be void in (a live view).
     */
    public Set<S> get(int player) {
        return voids.get(player);
    }

    /**
     * Records what the player's card, about to be played to the trick, reveals: if it does not follow the suit led,
     * the player holds none of that suit. A card that belongs to no suit reveals nothing.
     */
    public <C extends Component> void record(int player, Trick<C, S> trick, C card) {
        S lead = trick.getLeadSuit();
        S suit = trick.getOrder().suitOf(card);
        if (lead != null && suit != null && suit != lead)
            voids.get(player).add(lead);
    }

    /**
     * As {@link #record(int, Trick, Component)}, for a game in which a follower may play a trump
     * even when holding the suit led (Pitch): a trump then reveals nothing.
     *
     * @param trumps the trump suit, or null if there are no trumps
     */
    public <C extends Component> void record(int player, Trick<C, S> trick, C card, S trumps) {
        if (trick.getOrder().suitOf(card) != trumps)
            record(player, trick, card);
    }

    /**
     * As {@link #record(int, Trick, Component)}, for a game in which a player who cannot follow suit must play a
     * trump if they can (Scarto): a card that neither follows nor is a trump also shows the player has no trumps.
     * A card that belongs to no suit reveals nothing.
     *
     * @param trumps the trump suit
     */
    public <C extends Component> void recordMustTrump(int player, Trick<C, S> trick, C card, S trumps) {
        S lead = trick.getLeadSuit();
        S suit = trick.getOrder().suitOf(card);
        if (lead != null && suit != null && suit != lead) {
            voids.get(player).add(lead);
            if (suit != trumps)
                voids.get(player).add(trumps);
        }
    }

    /**
     * Forgets all known voids (at a new deal).
     */
    public void clear() {
        for (Set<S> v : voids)
            v.clear();
    }

    /**
     * Whether redeterminisation may put a card in a deck, with each card belonging to the suit the order gives it:
     * always for a deck with no owner, otherwise only if the owner is not known to be void in the card's suit.
     */
    public <C extends Component> BiPredicate<Deck<C>, C> permits(CardOrder<C, S> order) {
        return (deck, card) -> deck.getOwnerId() < 0 || !voids.get(deck.getOwnerId()).contains(order.suitOf(card));
    }

    public KnownVoids<S> copy() {
        List<Set<S>> copy = new ArrayList<>(voids.size());
        for (Set<S> v : voids) {
            Set<S> c = EnumSet.noneOf(suitClass);
            c.addAll(v);
            copy.add(c);
        }
        return new KnownVoids<>(suitClass, copy);
    }

    @Override
    public boolean equals(Object o) {
        // suitClass is fixed by the game, and only used to build the sets
        return o instanceof KnownVoids<?> that && voids.equals(that.voids);
    }

    @Override
    public int hashCode() {
        // from suit ordinals, as an enum's own hashCode differs between runs
        int result = 1;
        for (Set<S> v : voids) {
            int bits = 0;
            for (S s : v)
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

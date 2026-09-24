package games.tricktaking;

import core.CoreConstants.VisibilityMode;
import core.components.Deck;
import core.components.FrenchCard;

import java.util.Objects;

/**
 * <p>The trick in progress in a trick-taking game: a face-up Deck of the cards played to it, in the order played.
 * Index 0 is the lead card, and the card at index i was played by {@link #playerOf}(i): the players in turn from the
 * leader, leaving out any player sitting out.</p>
 *
 * <p>The game supplies the rules: which cards may be played (see {@link PlayRule}) and what is trumps.</p>
 *
 * <p>A game starts each trick with a new Trick, so two states in the same position hold Tricks with different
 * componentIDs. Equality is therefore by value: the cards, the leader, the number of players, the card order and
 * any player sitting out.</p>
 */
public class Trick extends Deck<FrenchCard> {

    private final int nPlayers;
    private final int leader;
    private final CardOrder order;
    private final int sittingOut;

    public Trick(String name, int nPlayers, int leader) {
        this(name, nPlayers, leader, CardOrder.STANDARD);
    }

    /**
     * A trick whose cards belong to suits, and rank within them, as the order says.
     */
    public Trick(String name, int nPlayers, int leader, CardOrder order) {
        this(name, nPlayers, leader, order, -1);
    }

    /**
     * A trick with the given card order, to which one player may play no card (in Euchre, the partner of a player
     * going alone).
     *
     * @param sittingOut the player who plays no card to the trick, or -1 if everyone plays
     */
    public Trick(String name, int nPlayers, int leader, CardOrder order, int sittingOut) {
        super(name, VisibilityMode.VISIBLE_TO_ALL);
        this.nPlayers = nPlayers;
        this.leader = leader;
        this.order = order;
        this.sittingOut = sittingOut;
    }

    private Trick(String name, int ID, int nPlayers, int leader, CardOrder order, int sittingOut) {
        super(name, -1, ID, VisibilityMode.VISIBLE_TO_ALL);
        this.nPlayers = nPlayers;
        this.leader = leader;
        this.order = order;
        this.sittingOut = sittingOut;
    }

    /**
     * The player who plays no card to this trick, or -1 if everyone plays.
     */
    public int getSittingOut() {
        return sittingOut;
    }

    /**
     * How the cards in this trick belong to suits and rank.
     */
    public CardOrder getOrder() {
        return order;
    }

    /**
     * The player who led (or, if the trick is empty, is to lead) this trick.
     */
    public int getLeader() {
        return leader;
    }

    public int getNPlayers() {
        return nPlayers;
    }

    /**
     * Adds the card to the trick as the next card played.
     */
    public void play(FrenchCard card) {
        addToBottom(card);  // index 0 stays the lead card
    }

    /**
     * The suit the lead card belongs to, or null if no card has been played.
     */
    public FrenchCard.Suite getLeadSuit() {
        return getSize() == 0 ? null : order.suitOf(get(0));
    }

    /**
     * The player who played the card at the given index.
     */
    public int playerOf(int index) {
        int player = leader;
        for (int i = 0; i < index; i++) {
            player = (player + 1) % nPlayers;
            if (player == sittingOut)
                player = (player + 1) % nPlayers;
        }
        return player;
    }

    /**
     * True once every player has played a card to the trick.
     */
    public boolean isComplete() {
        return getSize() == (sittingOut < 0 ? nPlayers : nPlayers - 1);
    }

    /**
     * The player winning the trick so far: whoever played the highest trump, or if no trump has been played, the
     * highest card of the suit led. Aces are high. The trick must not be empty.
     *
     * @param trumps the trump suit, or null if there are no trumps
     */
    public int winner(FrenchCard.Suite trumps) {
        int best = 0;
        for (int i = 1; i < getSize(); i++) {
            if (beats(get(i), get(best), trumps))
                best = i;
        }
        return playerOf(best);
    }

    /**
     * Whether the card beats the card currently winning the trick.
     */
    private boolean beats(FrenchCard card, FrenchCard winning, FrenchCard.Suite trumps) {
        FrenchCard.Suite suit = order.suitOf(card);
        if (suit == order.suitOf(winning))
            return order.rank(card) > order.rank(winning);
        return suit == trumps;
    }

    @Override
    public Trick copy() {
        Trick copy = new Trick(componentName, componentID, nPlayers, leader, order, sittingOut);
        copyTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trick trick)) return false;
        return nPlayers == trick.nPlayers && leader == trick.leader && sittingOut == trick.sittingOut &&
                order.equals(trick.order) &&
                components.equals(trick.components);
    }

    @Override
    public int hashCode() {
        // order is left out: CardOrder.STANDARD has no run-stable hash, and games hash their trump suit in the state
        return Objects.hash(components, nPlayers, leader, sittingOut);
    }
}

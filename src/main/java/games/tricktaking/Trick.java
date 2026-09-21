package games.tricktaking;

import core.CoreConstants.VisibilityMode;
import core.components.Deck;
import core.components.FrenchCard;

import java.util.Objects;

/**
 * <p>The trick in progress in a trick-taking game: a face-up Deck of the cards played to it, in the order played.
 * Index 0 is the lead card, and the card at index i was played by player (leader + i) % nPlayers.</p>
 *
 * <p>The game supplies the rules: which cards may be played (see {@link PlayRule}) and what is trumps.</p>
 *
 * <p>A game starts each trick with a new Trick, so two states in the same position hold Tricks with different
 * componentIDs. Equality is therefore by value: the cards, the leader and the number of players.</p>
 */
public class Trick extends Deck<FrenchCard> {

    private final int nPlayers;
    private final int leader;

    public Trick(String name, int nPlayers, int leader) {
        super(name, VisibilityMode.VISIBLE_TO_ALL);
        this.nPlayers = nPlayers;
        this.leader = leader;
    }

    private Trick(String name, int ID, int nPlayers, int leader) {
        super(name, -1, ID, VisibilityMode.VISIBLE_TO_ALL);
        this.nPlayers = nPlayers;
        this.leader = leader;
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
     * The suit of the lead card, or null if no card has been played.
     */
    public FrenchCard.Suite getLeadSuit() {
        return getSize() == 0 ? null : get(0).suite;
    }

    /**
     * The player who played the card at the given index.
     */
    public int playerOf(int index) {
        return (leader + index) % nPlayers;
    }

    /**
     * True once every player has played a card to the trick.
     */
    public boolean isComplete() {
        return getSize() == nPlayers;
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
        if (card.suite == winning.suite)
            return card.number > winning.number;  // FrenchCard numbers Aces 14, so Aces are high
        return card.suite == trumps;
    }

    @Override
    public Trick copy() {
        Trick copy = new Trick(componentName, componentID, nPlayers, leader);
        copyTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trick trick)) return false;
        return nPlayers == trick.nPlayers && leader == trick.leader && components.equals(trick.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(components, nPlayers, leader);
    }
}

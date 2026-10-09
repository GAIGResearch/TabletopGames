package games.lawnandorder;

import core.components.Deck;
import core.components.PartialObservableDeck;
import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;

import java.util.List;

/**
 * Scoring and risk helpers. Apart from categoryScore(List, ...), these work on attribute counts: an int[] indexed by
 * LawnCard.Attribute ordinal, holding the number of cards with each attribute. They are used by the feature vectors,
 * which run often, so they avoid streams and allocation.
 */
public class LawnAndOrderUtils {

    /** Cached, as Attribute.values() and Category.values() copy their array on every call. */
    public static final LawnCard.Attribute[] ATTRIBUTES = LawnCard.Attribute.values();
    public static final LawnCard.Category[] CATEGORIES = LawnCard.Category.values();
    private static final int N_SPECIALS = RuleCard.Special.values().length;
    /** The attribute ordinals of each category, indexed by Category ordinal. */
    private static final int[][] CATEGORY_ATTRIBUTES = new int[CATEGORIES.length][];

    static {
        for (LawnCard.Category category : CATEGORIES) {
            List<LawnCard.Attribute> attributes = LawnCard.Attribute.of(category);
            CATEGORY_ATTRIBUTES[category.ordinal()] = new int[attributes.size()];
            for (int i = 0; i < attributes.size(); i++)
                CATEGORY_ATTRIBUTES[category.ordinal()][i] = attributes.get(i).ordinal();
        }
    }

    private LawnAndOrderUtils() {
    }

    /**
     * The points the cards score on the track of the category.
     */
    public static int categoryScore(List<LawnCard> cards, LawnCard.Category category, LawnAndOrderParameters params) {
        // each attribute of the category scores its group of cards independently
        int total = 0;
        for (LawnCard.Attribute attribute : LawnCard.Attribute.of(category)) {
            int n = 0;
            for (LawnCard card : cards)
                if (card.has(attribute))
                    n++;
            total += params.groupPoints(n);
        }
        return total;
    }

    /**
     * The points cards with these attribute counts score on the track of the category.
     */
    public static int categoryScore(int[] counts, LawnCard.Category category, LawnAndOrderParameters params) {
        int total = 0;
        for (int a : CATEGORY_ATTRIBUTES[category.ordinal()])
            total += params.groupPoints(counts[a]);
        return total;
    }

    /**
     * The attribute counts of the cards.
     */
    public static int[] attributeCounts(Deck<LawnCard> cards) {
        int[] counts = new int[ATTRIBUTES.length];
        for (int i = 0; i < cards.getSize(); i++)
            addCard(counts, cards.get(i), 1);
        return counts;
    }

    /**
     * Adds the card's three attributes to the counts (delta 1), or takes them away (delta -1).
     */
    public static void addCard(int[] counts, LawnCard card, int delta) {
        counts[card.type.ordinal()] += delta;
        counts[card.colour.ordinal()] += delta;
        counts[card.feature.ordinal()] += delta;
    }

    /**
     * What the player still needs on each track to reach the target score, indexed by Category ordinal.
     */
    public static int[] shortfalls(LawnAndOrderGameState state, int player) {
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();
        int[] retValue = new int[CATEGORIES.length];
        for (LawnCard.Category category : CATEGORIES)
            retValue[category.ordinal()] = Math.max(0, params.targetScore - state.getTrackScore(player, category));
        return retValue;
    }

    /**
     * The points cards with these attribute counts score over the three tracks, each track's points capped at the
     * player's shortfall on it.
     */
    public static int usefulScore(int[] counts, int[] shortfalls, LawnAndOrderParameters params) {
        int total = 0;
        for (LawnCard.Category category : CATEGORIES)
            total += Math.min(shortfalls[category.ordinal()], categoryScore(counts, category, params));
        return total;
    }

    /**
     * Which attributes the rules revealed this round condemn, indexed by Attribute ordinal.
     */
    public static boolean[] condemned(LawnAndOrderGameState state) {
        boolean[] retValue = new boolean[ATTRIBUTES.length];
        Deck<RuleCard> revealed = state.getRevealedRules();
        for (int i = 0; i < revealed.getSize(); i++) {
            RuleCard r = revealed.get(i);
            if (r.condemned != null)
                retValue[r.condemned.ordinal()] = true;
        }
        return retValue;
    }

    /**
     * The Citations the card receives when it is played, from the condemned attributes.
     */
    public static int immediateCitations(boolean[] condemned, LawnCard card) {
        int n = 0;
        if (condemned[card.type.ordinal()]) n++;
        if (condemned[card.colour.ordinal()]) n++;
        if (condemned[card.feature.ordinal()]) n++;
        return n;
    }

    /**
     * The Rule cards a player has not seen this round: all of them, less the revealed rules and the Insider Tips they
     * sit beside. These are the agenda and the other Insider Tips. The tips the player has seen can never be revealed
     * this round.
     */
    public static final class UnseenRules {
        /** The unseen Standard Rules condemning each attribute, indexed by Attribute ordinal. */
        public final int[] standard = new int[ATTRIBUTES.length];
        /** The unseen Special Rules of each kind, indexed by Special ordinal. */
        public final int[] special = new int[N_SPECIALS];
        /** The number of unseen Rule cards. */
        public int size;
        /** The number of unseen Standard Rules. */
        public int standardCount;
        /** The number of Standard Rules on the Insider Tips the player sits beside. */
        public int standardOnSeenTips;
    }

    public static UnseenRules unseenRules(LawnAndOrderGameState state, int player) {
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();
        UnseenRules u = new UnseenRules();
        // the full set of Rule cards, as LawnAndOrderParameters.ruleCards
        for (int a = 0; a < ATTRIBUTES.length; a++)
            u.standard[a] = 1;
        u.special[RuleCard.Special.ADMINISTRATIVE_ERROR.ordinal()] = params.nAdministrativeError;
        u.special[RuleCard.Special.EMERGENCY_SESSION.ordinal()] = params.nEmergencySession;
        u.special[RuleCard.Special.ZERO_TOLERANCE.ordinal()] = params.nZeroTolerance;
        Deck<RuleCard> revealed = state.getRevealedRules();
        for (int i = 0; i < revealed.getSize(); i++)
            remove(u, revealed.get(i));
        PartialObservableDeck<RuleCard> tips = state.getInsiderTips();
        for (int i = 0; i < tips.getSize(); i++) {
            if (!tips.isComponentVisible(i, player)) continue;
            RuleCard r = tips.get(i);
            remove(u, r);
            if (r.condemned != null)
                u.standardOnSeenTips++;
        }
        for (int n : u.standard)
            u.standardCount += n;
        u.size = u.standardCount;
        for (int n : u.special)
            u.size += n;
        return u;
    }

    private static void remove(UnseenRules u, RuleCard r) {
        if (r.condemned != null)
            u.standard[r.condemned.ordinal()]--;
        else
            u.special[r.special.ordinal()]--;
    }

    /**
     * The expected number of retroactive Citations cards with these attribute counts receive from the next Rule card
     * revealed, if it is equally likely to be any of the unseen rules. Ignores the extra reveals of an Emergency
     * Session.
     */
    public static double expectedCitations(int[] counts, UnseenRules unseen) {
        if (unseen.size == 0)
            return 0.0;
        int total = 0;
        for (int a = 0; a < counts.length; a++)
            total += counts[a] * unseen.standard[a];
        return (double) total / unseen.size;
    }

    /**
     * The most cards, among those with these attribute counts, that one unseen Standard Rule would cite.
     */
    public static int maxExposure(int[] counts, UnseenRules unseen) {
        int retValue = 0;
        for (int a = 0; a < counts.length; a++)
            if (unseen.standard[a] > 0 && counts[a] > retValue)
                retValue = counts[a];
        return retValue;
    }

    /**
     * The probability that the next Rule card revealed takes the player over their Citation limit, if it is equally
     * likely to be any of the unseen rules. committedCounts are the attribute counts of the player's lawn plus the card
     * they have chosen this turn, if any. The next reveal follows a play, so the limit is one higher than now: the
     * chosen card lands, and is cited by the condemned attributes; or, if the player has not chosen, their next card
     * lands, whose attributes are unknown and ignored. Ignores the extra reveals of an Emergency Session.
     */
    public static double bustProbability(LawnAndOrderGameState state, int player, int[] committedCounts,
                                         boolean[] condemned, UnseenRules unseen) {
        if (unseen.size == 0)
            return 0.0;
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();
        int citations = state.getCitations(player);
        Deck<LawnCard> chosen = state.getChosenCard(player);
        if (chosen.getSize() > 0)
            citations += immediateCitations(condemned, chosen.get(0));
        int limit = state.getCitationLimit(player) + 1;
        return bustProbability(citations, limit, state.isZeroTolerance(), committedCounts, unseen, params);
    }

    /**
     * The probability that the next Rule card revealed takes a player with these citations, Citation limit and lawn
     * attribute counts over the limit, if it is equally likely to be any of the unseen rules. zeroTolerance is whether
     * a Zero Tolerance Policy is already in force (so another lowers the limit no further).
     */
    public static double bustProbability(int citations, int limit, boolean zeroTolerance, int[] counts,
                                         UnseenRules unseen, LawnAndOrderParameters params) {
        if (unseen.size == 0)
            return 0.0;
        int busts = 0;
        for (int a = 0; a < counts.length; a++)
            if (unseen.standard[a] > 0 && citations + counts[a] > limit)
                busts += unseen.standard[a];
        int nZeroTolerance = unseen.special[RuleCard.Special.ZERO_TOLERANCE.ordinal()];
        int zeroToleranceLimit = zeroTolerance ? limit : limit - params.zeroToleranceReduction;
        if (citations > zeroToleranceLimit)
            busts += nZeroTolerance;
        // an Administrative Error or Emergency Session adds no Citations of its own
        if (citations > limit)
            busts += unseen.size - unseen.standardCount - nZeroTolerance;
        return (double) busts / unseen.size;
    }
}

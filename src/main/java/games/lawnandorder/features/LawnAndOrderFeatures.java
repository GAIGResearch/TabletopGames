package games.lawnandorder.features;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateFeatureVector;
import games.lawnandorder.LawnAndOrderGameState;
import games.lawnandorder.LawnAndOrderParameters;
import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;

import static games.lawnandorder.LawnAndOrderGameState.Phase.PLAY_OBJECT;
import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.*;
import static games.lawnandorder.LawnAndOrderUtils.*;

/**
 * The detailed feature set: long-term score, this round's lawn, Citation risk, what the player knows of the agenda,
 * their hand, and the other players. Uses only what the player can see. When the player is not active (they have
 * passed, so are immune, or have a Cease &amp; Desist), the risk and hand features take their "nothing more can
 * happen" values: no expected Citations, no chance of a Cease &amp; Desist, the hand size as headroom, and an empty
 * hand.
 * <p>
 * The "lawn" is the player's lawn plus the card they have chosen this turn, if any; the expected Citations and
 * probabilities treat the next Rule card as equally likely to be any the player has not seen (see
 * LawnAndOrderUtils.UnseenRules).
 */
public class LawnAndOrderFeatures implements IStateFeatureVector {

    static final String[] names = new String[]{
            // long-term score
            "TRACK_TYPE",                   // 0: track score / target score
            "TRACK_COLOUR",                 // 1
            "TRACK_FEATURE",                // 2
            "MIN_TRACK",                    // 3: lowest of the three
            "TRACKS_AT_TARGET",             // 4: tracks at or above the target score
            "TOTAL_SCORE",                  // 5: combined track score
            "SCORE_LEAD",                   // 6: combined score less the best opponent's
            "BEST_OPP_MIN_TRACK",           // 7: highest opponent lowest track / target score
            "ROUND",                        // 8
            // this round's lawn
            "LAWN_SIZE",                    // 9
            "LAWN_PTS_TYPE",                // 10: points the lawn would score on each track now
            "LAWN_PTS_COLOUR",              // 11
            "LAWN_PTS_FEATURE",             // 12
            "LAWN_POINTS_USEFUL",           // 13: as the three above, each capped at the shortfall to target
            "LARGEST_GROUP",                // 14: most lawn cards sharing an attribute
            "GROWABLE_GROUPS",              // 15: attributes on the lawn whose group would score more with one more card
            // Citation risk
            "CITATIONS",                    // 16
            "CITATION_LIMIT",               // 17
            "CITATION_HEADROOM",            // 18: limit less Citations; the hand size if not active
            "GOODWILL",                     // 19
            "ZERO_TOLERANCE_ACTIVE",        // 20
            "ZERO_TOLERANCE_UNSEEN",        // 21: Zero Tolerance Policies the player has not seen
            "EXPECTED_CITATIONS_NEXT",      // 22: expected Citations from the next reveal
            "P_BUST_NEXT",                  // 23: probability the next reveal gives a Cease & Desist
            "MAX_EXPOSURE",                 // 24: most lawn cards one unseen Standard Rule would cite
            "CONDEMNED_REVEALED",           // 25: Standard Rules revealed this round
            "SAFE_ATTRIBUTES_KNOWN",        // 26: Standard Rules on the Insider Tips the player sees
            // the agenda
            "AGENDA_REMAINING",             // 27
            "RULES_REVEALED",               // 28
            "UNSEEN_POOL_SIZE",             // 29: Rule cards the player has not seen
            "UNSEEN_STANDARD_RULES",        // 30
            "EMERGENCY_UNSEEN",             // 31
            // the hand
            "HAND_SIZE",                    // 32
            "HAND_BEST_GAIN",               // 33: most LAWN_POINTS_USEFUL gained by playing one card
            "HAND_MEAN_GAIN",               // 34
            "HAND_SAFE_CARDS",              // 35: cards with no condemned attribute
            "HAND_BEST_SAFE_GAIN",          // 36: as HAND_BEST_GAIN, over the safe cards
            "HAND_MIN_IMMEDIATE_CITATIONS", // 37: fewest Citations a card would receive when played
            "HAND_MEAN_EXPOSURE",           // 38: mean EXPECTED_CITATIONS_NEXT if the card were added to the lawn
            "DRAW_DECK_SIZE",               // 39
            // status and opponents
            "PASSED",                       // 40
            "CEASED",                       // 41
            "PHASE_PLAY",                   // 42: 1 when choosing a card, 0 when choosing whether to continue
            "OPP_PASSED",                   // 43: proportion of opponents
            "OPP_CEASED",                   // 44: proportion of opponents
            "BEST_OPP_LAWN_POINTS",         // 45: most points an opponent's lawn would score now
            "MIN_OPP_HEADROOM",             // 46: lowest Citation headroom of an active opponent; the hand size if none
            "OPP_HAND_SIZE_MEAN"            // 47: including any card they have chosen this turn
    };

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        LawnAndOrderGameState state = (LawnAndOrderGameState) gs;
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        double target = params.targetScore;
        LawnAndOrderGameState.PlayerStatus status = state.getStatus(playerID);
        boolean active = status == ACTIVE;
        double[] retValue = new double[names.length];

        // long-term score
        int minTrack = Integer.MAX_VALUE;
        int atTarget = 0;
        for (LawnCard.Category category : CATEGORIES) {
            int score = state.getTrackScore(playerID, category);
            retValue[category.ordinal()] = score / target;
            minTrack = Math.min(minTrack, score);
            if (score >= params.targetScore)
                atTarget++;
        }
        retValue[3] = minTrack / target;
        retValue[4] = atTarget;
        double total = state.getGameScore(playerID);
        retValue[5] = total;

        // opponents
        double bestOtherTotal = Double.NEGATIVE_INFINITY;
        int bestOtherMinTrack = 0;
        int oppPassed = 0, oppCeased = 0;
        int bestOppLawn = 0;
        int minOppHeadroom = Integer.MAX_VALUE;
        int oppHandCards = 0;
        for (int p = 0; p < nPlayers; p++) {
            if (p == playerID) continue;
            bestOtherTotal = Math.max(bestOtherTotal, state.getGameScore(p));
            int oppMin = Integer.MAX_VALUE;
            for (LawnCard.Category category : CATEGORIES)
                oppMin = Math.min(oppMin, state.getTrackScore(p, category));
            bestOtherMinTrack = Math.max(bestOtherMinTrack, oppMin);
            switch (state.getStatus(p)) {
                case PASSED -> oppPassed++;
                case CEASE_AND_DESIST -> oppCeased++;
                case ACTIVE -> minOppHeadroom = Math.min(minOppHeadroom,
                        state.getCitationLimit(p) - state.getCitations(p));
            }
            int[] oppCounts = attributeCounts(state.getLawn(p));
            int oppLawn = 0;
            for (LawnCard.Category category : CATEGORIES)
                oppLawn += categoryScore(oppCounts, category, params);
            bestOppLawn = Math.max(bestOppLawn, oppLawn);
            // a chosen card is face down, but it is known that the player has one
            oppHandCards += state.getHand(p).getSize() + state.getChosenCard(p).getSize();
        }
        retValue[6] = total - bestOtherTotal;
        retValue[7] = bestOtherMinTrack / target;
        retValue[8] = state.getRoundCounter();

        // this round's lawn
        int[] counts = attributeCounts(state.getLawn(playerID));
        Deck<LawnCard> chosen = state.getChosenCard(playerID);
        if (chosen.getSize() > 0)
            addCard(counts, chosen.get(0), 1);
        int[] shortfalls = shortfalls(state, playerID);
        retValue[9] = state.getLawn(playerID).getSize() + chosen.getSize();
        for (LawnCard.Category category : CATEGORIES)
            retValue[10 + category.ordinal()] = categoryScore(counts, category, params);
        int useful = usefulScore(counts, shortfalls, params);
        retValue[13] = useful;
        int largest = 0, growable = 0;
        int maxGroup = LawnAndOrderParameters.GROUP_POINTS.length - 1;
        for (int n : counts) {
            largest = Math.max(largest, n);
            if (n >= 1 && n < maxGroup)
                growable++;
        }
        retValue[14] = largest;
        retValue[15] = growable;

        // Citation risk
        boolean[] condemned = condemned(state);
        UnseenRules unseen = unseenRules(state, playerID);
        int citations = state.getCitations(playerID);
        int limit = state.getCitationLimit(playerID);
        boolean zeroTolerance = state.isZeroTolerance();
        retValue[16] = citations;
        retValue[17] = limit;
        retValue[19] = state.hasGoodwill(playerID) ? 1 : 0;
        retValue[20] = zeroTolerance ? 1 : 0;
        retValue[21] = unseen.special[RuleCard.Special.ZERO_TOLERANCE.ordinal()];
        double expected = 0.0;
        if (active) {
            expected = expectedCitations(counts, unseen);
            retValue[18] = limit - citations;
            retValue[22] = expected;
            retValue[23] = bustProbability(state, playerID, counts, condemned, unseen);
            retValue[24] = maxExposure(counts, unseen);
        } else {
            retValue[18] = params.handSize;
        }
        int condemnedRevealed = 0;
        for (boolean c : condemned)
            if (c) condemnedRevealed++;
        retValue[25] = condemnedRevealed;
        retValue[26] = unseen.standardOnSeenTips;

        // the agenda
        retValue[27] = state.getAgenda().getSize();
        retValue[28] = state.getRevealedRules().getSize();
        retValue[29] = unseen.size;
        retValue[30] = unseen.standardCount;
        retValue[31] = unseen.special[RuleCard.Special.EMERGENCY_SESSION.ordinal()];

        // the hand
        Deck<LawnCard> hand = state.getHand(playerID);
        int handSize = hand.getSize();
        if (active && handSize > 0) {
            int bestGain = Integer.MIN_VALUE, gainTotal = 0;
            int safeCards = 0, bestSafeGain = 0;
            int minImmediate = Integer.MAX_VALUE;
            // each card adds its three attributes' unseen Standard Rules to the expected Citations
            int exposureTotal = 0;
            for (int i = 0; i < handSize; i++) {
                LawnCard card = hand.get(i);
                addCard(counts, card, 1);
                int gain = usefulScore(counts, shortfalls, params) - useful;
                addCard(counts, card, -1);
                bestGain = Math.max(bestGain, gain);
                gainTotal += gain;
                int immediate = immediateCitations(condemned, card);
                minImmediate = Math.min(minImmediate, immediate);
                if (immediate == 0) {
                    bestSafeGain = safeCards == 0 ? gain : Math.max(bestSafeGain, gain);
                    safeCards++;
                }
                exposureTotal += unseen.standard[card.type.ordinal()] + unseen.standard[card.colour.ordinal()]
                        + unseen.standard[card.feature.ordinal()];
            }
            retValue[32] = handSize;
            retValue[33] = bestGain;
            retValue[34] = (double) gainTotal / handSize;
            retValue[35] = safeCards;
            retValue[36] = bestSafeGain;
            retValue[37] = minImmediate;
            retValue[38] = unseen.size == 0 ? 0.0 : expected + (double) exposureTotal / handSize / unseen.size;
        }
        retValue[39] = state.getDrawDeck().getSize();

        // status and opponents
        retValue[40] = status == PASSED ? 1 : 0;
        retValue[41] = status == CEASE_AND_DESIST ? 1 : 0;
        retValue[42] = state.getGamePhase() == PLAY_OBJECT ? 1 : 0;
        retValue[43] = (double) oppPassed / (nPlayers - 1);
        retValue[44] = (double) oppCeased / (nPlayers - 1);
        retValue[45] = bestOppLawn;
        retValue[46] = minOppHeadroom == Integer.MAX_VALUE ? params.handSize : minOppHeadroom;
        retValue[47] = (double) oppHandCards / (nPlayers - 1);
        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }
}

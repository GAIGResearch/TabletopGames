package games.lawnandorder.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import games.lawnandorder.LawnAndOrderGameState;
import games.lawnandorder.LawnAndOrderParameters;
import games.lawnandorder.components.LawnCard;

import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.ACTIVE;
import static games.lawnandorder.LawnAndOrderUtils.*;

/**
 * Ten features: the long-term position, the worth of this round's lawn, and the risk of losing it. Uses only what
 * the player can see. When the player is not active (they have passed, so are immune, or have a Cease &amp; Desist),
 * the risk and hand features take their "nothing more can happen" values.
 */
public class LawnAndOrderFeaturesReduced implements IStateFeatureVector {

    static final String[] names = new String[]{
            "MIN_TRACK",                // lowest track score / target score
            "TOTAL_SCORE",              // combined track score
            "SCORE_LEAD",               // combined score less the best opponent's
            "LAWN_POINTS",              // points the lawn (and chosen card) would score now
            "LAWN_POINTS_USEFUL",       // as LAWN_POINTS, each track capped at the shortfall to target
            "CITATION_HEADROOM",        // Citation limit less Citations; the hand size if not active
            "EXPECTED_CITATIONS_NEXT",  // expected Citations from the next reveal; 0 if not active
            "OPP_INACTIVE",             // proportion of opponents who have passed or have a Cease & Desist
            "AGENDA_REMAINING",         // Rule cards left in the HOA Agenda
            "HAND_SIZE"                 // cards in hand; 0 if not active
    };

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        LawnAndOrderGameState state = (LawnAndOrderGameState) gs;
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        boolean active = state.getStatus(playerID) == ACTIVE;
        double[] retValue = new double[names.length];

        int minTrack = Integer.MAX_VALUE;
        for (LawnCard.Category category : CATEGORIES)
            minTrack = Math.min(minTrack, state.getTrackScore(playerID, category));
        retValue[0] = (double) minTrack / params.targetScore;

        double total = state.getGameScore(playerID);
        double bestOther = Double.NEGATIVE_INFINITY;
        int inactive = 0;
        for (int p = 0; p < nPlayers; p++) {
            if (p == playerID) continue;
            bestOther = Math.max(bestOther, state.getGameScore(p));
            if (state.getStatus(p) != ACTIVE)
                inactive++;
        }
        retValue[1] = total;
        retValue[2] = total - bestOther;

        int[] counts = attributeCounts(state.getLawn(playerID));
        if (state.getChosenCard(playerID).getSize() > 0)
            addCard(counts, state.getChosenCard(playerID).get(0), 1);
        int lawnPoints = 0;
        for (LawnCard.Category category : CATEGORIES)
            lawnPoints += categoryScore(counts, category, params);
        retValue[3] = lawnPoints;
        retValue[4] = usefulScore(counts, shortfalls(state, playerID), params);

        if (active) {
            retValue[5] = state.getCitationLimit(playerID) - state.getCitations(playerID);
            retValue[6] = expectedCitations(counts, unseenRules(state, playerID));
            retValue[9] = state.getHand(playerID).getSize();
        } else {
            retValue[5] = params.handSize;
        }
        retValue[7] = (double) inactive / (nPlayers - 1);
        retValue[8] = state.getAgenda().getSize();
        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }
}

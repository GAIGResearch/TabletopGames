package games.lawnandorder.features;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IActionFeatureVector;
import games.lawnandorder.LawnAndOrderGameState;
import games.lawnandorder.LawnAndOrderParameters;
import games.lawnandorder.actions.Continue;
import games.lawnandorder.actions.PlayObject;
import games.lawnandorder.components.LawnCard;

import static games.lawnandorder.LawnAndOrderUtils.*;

/**
 * Action features for Lawn and Order. A decision is either which card to play (PlayObject) or whether to continue
 * (Continue or Pass), so the PLAY_ features distinguish the cards and the CONTINUE_ features distinguish Continue
 * from Pass, which is all zeros. The features depend on the state as well as the action, so that a linear model
 * without interactions can still weigh the risk of continuing. Uses only what the player can see.
 */
public class LawnAndOrderActionFeatures implements IActionFeatureVector {

    static final String[] names = new String[]{
            "CONTINUE",                 // 0: 1 for Continue
            "CONTINUE_P_BUST",          // 1: probability the reveal after the next play gives a Cease & Desist
            "CONTINUE_LAWN_POINTS",     // 2: the useful lawn points at stake (what passing would bank)
            "PLAY_GAIN",                // 3: useful lawn points the card adds
            "PLAY_IMMEDIATE_CITATIONS", // 4: Citations the card receives when it lands
            "PLAY_MATCHES",             // 5: the card's attributes already on the lawn (0 to 3)
            "PLAY_EXPOSURE",            // 6: expected Citations the card adds from the next reveal
            "PLAY_P_BUST"               // 7: probability of a Cease & Desist this turn if the card is played
    };

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState gs, int playerID) {
        double[] retValue = new double[names.length];
        if (!(action instanceof PlayObject) && !(action instanceof Continue))
            return retValue;  // Pass
        LawnAndOrderGameState state = (LawnAndOrderGameState) gs;
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();

        int[] counts = attributeCounts(state.getLawn(playerID));
        Deck<LawnCard> chosen = state.getChosenCard(playerID);
        if (chosen.getSize() > 0)
            addCard(counts, chosen.get(0), 1);
        int[] shortfalls = shortfalls(state, playerID);
        int useful = usefulScore(counts, shortfalls, params);
        UnseenRules unseen = unseenRules(state, playerID);
        boolean[] condemned = condemned(state);

        if (action instanceof Continue) {
            retValue[0] = 1;
            retValue[1] = bustProbability(state, playerID, counts, condemned, unseen);
            retValue[2] = useful;
            return retValue;
        }

        LawnCard card = ((PlayObject) action).card;
        int matches = 0;
        if (counts[card.type.ordinal()] > 0) matches++;
        if (counts[card.colour.ordinal()] > 0) matches++;
        if (counts[card.feature.ordinal()] > 0) matches++;
        int immediate = immediateCitations(condemned, card);
        addCard(counts, card, 1);
        retValue[3] = usefulScore(counts, shortfalls, params) - useful;
        retValue[4] = immediate;
        retValue[5] = matches;
        retValue[6] = unseen.size == 0 ? 0.0 : (double) (unseen.standard[card.type.ordinal()]
                + unseen.standard[card.colour.ordinal()] + unseen.standard[card.feature.ordinal()]) / unseen.size;
        // the card lands (raising the limit by one) before this turn's reveal
        retValue[7] = bustProbability(state.getCitations(playerID) + immediate, state.getCitationLimit(playerID) + 1,
                state.isZeroTolerance(), counts, unseen, params);
        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }
}

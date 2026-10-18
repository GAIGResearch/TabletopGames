package games.toads.metrics;

import core.AbstractGameState;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IStateFeatureVector;
import games.toads.ToadConstants.ToadCardType;
import games.toads.ToadGameState;
import games.toads.components.ToadCard;

import java.util.Arrays;

import static games.toads.ToadConstants.ToadCardType.*;

/**
 * A small state feature vector for War of the Toads: the position in the game, the Hostage race, the player's hand,
 * and what they know of the opponent's cards; then, for each card type, whether the player holds it, whether they
 * have played it this War, whether it is the opponent's face-up card, and whether the opponent has played it in an
 * earlier Battle of this War. Uses only what the player can see. In particular the opponent's face-up card is used
 * only when the opponent is the Attacker: the Defender's cards are chosen at the same time as the Attacker's hidden card,
 * so are not known to the Attacker.
 */
public class ToadFeaturesSimple implements IStateFeatureVector {

    /** The card types of the Rulebook 3 deck, in order of Strength, for the one-hot features. */
    public static final ToadCardType[] CARD_TYPES = {SIEGE_CANNON, ASSASSIN, SCOUT, SABOTEUR, TRICKSTER, BERSERKER,
            BODYGUARD, GENERAL_ONE, GENERAL_TWO};
    /** The index in CARD_TYPES of each ToadCardType, by ordinal; -1 for a type not in the deck. */
    static final int[] TYPE_INDEX = new int[ToadCardType.values().length];

    static {
        Arrays.fill(TYPE_INDEX, -1);
        for (int i = 0; i < CARD_TYPES.length; i++)
            TYPE_INDEX[CARD_TYPES[i].ordinal()] = i;
    }

    static final String[] baseNames = new String[]{
            "WAR_TWO",              // 0: 1 in the second War
            "HOSTAGE_LEAD",         // 1: our Hostages less theirs in the current War
            "ANGRY",                // 2: 1 if we have fewer Hostages (so a double win keeps both)
            "WAR_ONE_RESULT",       // 3: in War 2, +1 if we won War 1, -1 if we lost it; otherwise 0
            "BATTLES_LEFT",         // 4: Battles still to fight in this War, including the current one
            "ATTACKER",             // 5: 1 if we are the Attacker in the current Battle
            "HAND_MEAN",            // 6: mean Strength of the cards in hand (including a hidden card not yet revealed)
            "OPP_FIELD",            // 7: Strength of the opponent's face-up card, when we can see it
            "OPP_HAND_KNOWN",       // 8: cards in the opponent's hand we have seen (e.g. by Scout)
            "CASUALTY"              // 9: Strength of our Casualty (lower wins a double Stalemate); 0 in War 1
    };
    static final int IN_HAND = baseNames.length;
    static final int PLAYED = IN_HAND + CARD_TYPES.length;
    static final int OPP_FIELD = PLAYED + CARD_TYPES.length;
    static final int OPP_PLAYED = OPP_FIELD + CARD_TYPES.length;
    static final String[] names = new String[OPP_PLAYED + CARD_TYPES.length];

    static {
        System.arraycopy(baseNames, 0, names, 0, baseNames.length);
        for (int i = 0; i < CARD_TYPES.length; i++) {
            // <TYPE>_IN_HAND: held, and not committed as this Battle's hidden card
            names[IN_HAND + i] = CARD_TYPES[i] + "_IN_HAND";
            // <TYPE>_PLAYED: played this War, including to this Battle
            names[PLAYED + i] = CARD_TYPES[i] + "_PLAYED";
            // OPP_<TYPE>_FIELD: the opponent's face-up card this Battle, when we can see it
            names[OPP_FIELD + i] = "OPP_" + CARD_TYPES[i] + "_FIELD";
            // OPP_<TYPE>_PLAYED: played by the opponent in an earlier Battle of this War
            names[OPP_PLAYED + i] = "OPP_" + CARD_TYPES[i] + "_PLAYED";
        }
    }

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        ToadGameState state = (ToadGameState) gs;
        int opp = 1 - playerID;
        int war = state.getRoundCounter();
        double[] retValue = new double[names.length];

        retValue[0] = war;
        int lead = state.getBattlesWon(war, playerID) - state.getBattlesWon(war, opp);
        retValue[1] = lead;
        retValue[2] = lead < 0 ? 1 : 0;
        if (war > 0)
            retValue[3] = Integer.signum(state.getBattlesWon(0, playerID) - state.getBattlesWon(0, opp));

        PartialObservableDeck<ToadCard> hand = state.getPlayerHand(playerID);
        ToadCard field = state.getFieldCard(playerID);
        ToadCard hidden = state.getHiddenFlankCard(playerID);
        // each Battle uses two cards; a hidden card stays in the hand until it is revealed
        int cardsLeft = hand.getSize() + state.getPlayerDeck(playerID).getSize() + (field != null ? 1 : 0);
        retValue[4] = cardsLeft / 2;
        retValue[5] = state.getAttacker() == playerID ? 1 : 0;

        int total = 0;
        for (int i = 0; i < hand.getSize(); i++) {
            ToadCard card = hand.get(i);
            total += card.value;
            // the hidden card is in the hand until it is revealed, but is played
            setOneHot(retValue, card.equals(hidden) ? PLAYED : IN_HAND, card);
        }
        if (hand.getSize() > 0)
            retValue[6] = (double) total / hand.getSize();
        if (field != null)
            setOneHot(retValue, PLAYED, field);
        Deck<ToadCard> discards = state.getDiscards(playerID);
        for (int i = 0; i < discards.getSize(); i++)
            setOneHot(retValue, PLAYED, discards.get(i));

        ToadCard oppField = state.getFieldCard(opp);
        if (oppField != null && state.getAttacker() == opp) {
            retValue[7] = oppField.value;
            setOneHot(retValue, OPP_FIELD, oppField);
        }
        // all the cards played in a Battle are revealed, so the opponent's discards are known
        Deck<ToadCard> oppDiscards = state.getDiscards(opp);
        for (int i = 0; i < oppDiscards.getSize(); i++)
            setOneHot(retValue, OPP_PLAYED, oppDiscards.get(i));
        PartialObservableDeck<ToadCard> oppHand = state.getPlayerHand(opp);
        int known = 0;
        for (int i = 0; i < oppHand.getSize(); i++)
            if (oppHand.isComponentVisible(i, playerID))
                known++;
        retValue[8] = known;
        ToadCard casualty = state.getTieBreaker(playerID);
        if (casualty != null)
            retValue[9] = casualty.value;
        return retValue;
    }

    private static void setOneHot(double[] values, int offset, ToadCard card) {
        int index = TYPE_INDEX[card.type.ordinal()];
        if (index >= 0)
            values[offset + index] = 1;
    }

    @Override
    public String[] names() {
        return names;
    }
}

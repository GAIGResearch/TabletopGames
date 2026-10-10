package games.toads.metrics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import games.toads.ToadGameState;
import games.toads.actions.*;
import games.toads.components.ToadCard;

import static games.toads.ToadConstants.ToadCardType.*;

/**
 * A small action feature vector for War of the Toads, covering the card choices: which cards to play in a Battle,
 * and which card to return at the start of a War (or recycle). The Tactic actions after a Battle (guesses, forced
 * discards and so on) are all zeros.
 * <p>
 * Only the Defender knows the opposing face-up card when they choose, so the FIELD_ comparisons are for the
 * Defender. A Siege Cannon ignores Strength, winning in Attack (except against a Saboteur) and always losing in
 * Defence, so it has its own features. Then a one-hot block, <TYPE>_PLAY, for the card types played (both of them for
 * the Defender).
 */
public class ToadActionFeaturesSimple implements IActionFeatureVector {

    static final String[] baseNames = new String[]{
            "FIELD_VALUE",       // 0: Strength of the face-up card played
            "FLANK_VALUE",       // 1: Strength of the hidden card played
            "FIELD_MARGIN",      // 2: Defender: our face-up Strength less the Attacker's (0 if a Siege Cannon)
            "FIELD_OUTCOME",     // 3: Defender: +1 / 0 / -1 if the face-up lane is won / tied / lost on printed values
            "SIEGE_IN_ATTACK",   // 4: the Attacker plays their Siege Cannon
            "SIEGE_IN_DEFENCE",  // 5: the Defender plays their Siege Cannon
            "RETURN_VALUE"       // 6: Strength of the card returned to the deck at the start of a War, or recycled
    };
    static final int PLAY = baseNames.length;
    static final String[] names = new String[PLAY + ToadFeaturesSimple.CARD_TYPES.length];

    static {
        System.arraycopy(baseNames, 0, names, 0, baseNames.length);
        for (int i = 0; i < ToadFeaturesSimple.CARD_TYPES.length; i++)
            names[PLAY + i] = ToadFeaturesSimple.CARD_TYPES[i] + "_PLAY";
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState gs, int playerID) {
        double[] retValue = new double[names.length];
        if (action instanceof PlayFieldCard pfc) {
            // the Attacker opens the Battle
            retValue[0] = pfc.card.value;
            if (pfc.card.type == SIEGE_CANNON)
                retValue[4] = 1;
            setPlayed(retValue, pfc.card);
        } else if (action instanceof PlayFlankCard pfc) {
            // the Attacker's hidden card
            retValue[1] = pfc.card.value;
            if (pfc.card.type == SIEGE_CANNON)
                retValue[4] = 1;
            setPlayed(retValue, pfc.card);
        } else if (action instanceof PlayDefenderCards pdc) {
            retValue[0] = pdc.fieldCard.value;
            retValue[1] = pdc.flankCard.value;
            if (pdc.fieldCard.type == SIEGE_CANNON || pdc.flankCard.type == SIEGE_CANNON)
                retValue[5] = 1;
            setPlayed(retValue, pdc.fieldCard);
            setPlayed(retValue, pdc.flankCard);
            ToadCard attackerField = ((ToadGameState) gs).getFieldCard(1 - playerID);
            if (attackerField != null) {
                if (pdc.fieldCard.type != SIEGE_CANNON && attackerField.type != SIEGE_CANNON)
                    retValue[2] = pdc.fieldCard.value - attackerField.value;
                retValue[3] = defenderOutcome(pdc.fieldCard, attackerField);
            }
        } else if (action instanceof ReturnCardToDeck rc) {
            retValue[6] = rc.card.value;
        } else if (action instanceof RecycleCard rc && rc.discardedCard != null) {
            retValue[6] = rc.discardedCard.value;
        }
        return retValue;
    }

    /**
     * The result of a lane for the Defender on the printed cards, before any Tactic: a Siege Cannon in Defence always
     * loses, and in Attack wins unless faced by a Saboteur; an Assassin beats a General; otherwise higher Strength
     * wins.
     */
    static int defenderOutcome(ToadCard defender, ToadCard attacker) {
        if (defender.type == SIEGE_CANNON)
            return -1;
        if (attacker.type == SIEGE_CANNON)
            return defender.type == SABOTEUR ? 1 : -1;
        if (defender.type == ASSASSIN && isGeneral(attacker))
            return 1;
        if (attacker.type == ASSASSIN && isGeneral(defender))
            return -1;
        return Integer.signum(defender.value - attacker.value);
    }

    private static void setPlayed(double[] values, ToadCard card) {
        int index = ToadFeaturesSimple.TYPE_INDEX[card.type.ordinal()];
        if (index >= 0)
            values[PLAY + index] = 1;
    }

    private static boolean isGeneral(ToadCard card) {
        return card.type == GENERAL_ONE || card.type == GENERAL_TWO;
    }

    @Override
    public String[] names() {
        return names;
    }
}

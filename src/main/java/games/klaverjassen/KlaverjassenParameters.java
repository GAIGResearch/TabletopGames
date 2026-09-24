package games.klaverjassen;

import core.components.FrenchCard;
import evaluation.optimisation.TunableParameters;
import games.tricktaking.ITrickTakingParameters;

import java.util.Arrays;
import java.util.List;

/**
 * Parameters for Klaverjassen (as described at https://www.pagat.com/jass/klaverjassen.html, Utrecht trump choice and
 * Amsterdam play rules). The defaults follow the RECYCLE description of the game; partnerTrumpRule and
 * tieIsFailure give pagat's versions of the rules where they differ.
 */
public class KlaverjassenParameters extends TunableParameters<KlaverjassenParameters> implements ITrickTakingParameters {

    /**
     * What a player may play when they cannot follow suit and their partner is winning the trick with a trump.
     * DISCARD: any card that is not a trump, or anything if they hold only trumps (RECYCLE).
     * NO_UNDERTRUMP: any card that is not a trump, or a trump higher than partner's; a lower trump only if the hand
     * holds nothing else (pagat).
     */
    public enum PartnerTrumpRule {DISCARD, NO_UNDERTRUMP}

    // Cards dealt to each player
    public int handSize = 8;

    // Hands in the game. The team with more points after the last hand wins
    public int nHands = 1;

    public PartnerTrumpRule partnerTrumpRule = PartnerTrumpRule.DISCARD;

    // If true, the trump-choosing team fails when its points only equal the opponents' (pagat); if false, it fails
    // only with fewer points (RECYCLE)
    public boolean tieIsFailure = false;

    // Bonus points
    public int lastTrickBonus = 10;
    public int pitBonus = 100;         // for one team taking every trick of the hand
    public int runOfThreeBonus = 20;
    public int runOfFourBonus = 50;
    public int stukBonus = 20;         // the King and Queen of trumps in one trick
    public int fourOfAKindBonus = 100; // four Kings, Queens, Aces or Tens
    public int fourJacksBonus = 200;

    // Whether players remember the suits others have failed to follow (used when redeterminising)
    public boolean rememberVoids = true;

    public KlaverjassenParameters() {
        addTunableParameter("handSize", 8);
        addTunableParameter("nHands", 1, Arrays.asList(1, 2, 4, 8, 16));
        addTunableParameter("partnerTrumpRule", PartnerTrumpRule.DISCARD, List.of(PartnerTrumpRule.values()));
        addTunableParameter("tieIsFailure", false, List.of(false, true));
        addTunableParameter("lastTrickBonus", 10);
        addTunableParameter("pitBonus", 100);
        addTunableParameter("runOfThreeBonus", 20);
        addTunableParameter("runOfFourBonus", 50);
        addTunableParameter("stukBonus", 20);
        addTunableParameter("fourOfAKindBonus", 100);
        addTunableParameter("fourJacksBonus", 200);
        addTunableParameter("rememberVoids", true, List.of(false, true));
    }

    @Override
    public void _reset() {
        handSize = (int) getParameterValue("handSize");
        nHands = (int) getParameterValue("nHands");
        partnerTrumpRule = (PartnerTrumpRule) getParameterValue("partnerTrumpRule");
        tieIsFailure = (boolean) getParameterValue("tieIsFailure");
        lastTrickBonus = (int) getParameterValue("lastTrickBonus");
        pitBonus = (int) getParameterValue("pitBonus");
        runOfThreeBonus = (int) getParameterValue("runOfThreeBonus");
        runOfFourBonus = (int) getParameterValue("runOfFourBonus");
        stukBonus = (int) getParameterValue("stukBonus");
        fourOfAKindBonus = (int) getParameterValue("fourOfAKindBonus");
        fourJacksBonus = (int) getParameterValue("fourJacksBonus");
        rememberVoids = (boolean) getParameterValue("rememberVoids");
    }

    /**
     * The card points of the card when the given suit is trumps.
     */
    public int cardPoints(FrenchCard card, FrenchCard.Suite trumps) {
        boolean trump = card.suite == trumps;
        return switch (card.number) {
            case 11 -> trump ? 20 : 2;  // Jack
            case 9 -> trump ? 14 : 0;
            case 14 -> 11;              // Ace
            case 10 -> 10;
            case 13 -> 4;               // King
            case 12 -> 3;               // Queen
            default -> 0;
        };
    }

    @Override
    public boolean rememberVoids() {
        return rememberVoids;
    }

    @Override
    protected KlaverjassenParameters _copy() {
        return new KlaverjassenParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof KlaverjassenParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public KlaverjassenParameters instantiate() {
        return this;
    }
}

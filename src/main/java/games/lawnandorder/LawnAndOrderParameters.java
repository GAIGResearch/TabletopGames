package games.lawnandorder;

import evaluation.optimisation.TunableParameters;
import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parameters for Lawn and Order. The defaults are the rules of the Condensed Rulesheet v3 (Tim Cooper).
 */
public class LawnAndOrderParameters extends TunableParameters<LawnAndOrderParameters> {

    /** The points for a group of 0, 1, ..., 6 cards sharing an attribute; a larger group scores as 6. */
    public static final int[] GROUP_POINTS = {0, 0, 1, 2, 4, 7, 10};

    public int handSize = 5;
    public int nAdministrativeError = 2;
    public int nEmergencySession = 1;
    public int nZeroTolerance = 1;
    public int emergencySessionReveals = 2;
    public int zeroToleranceReduction = 1;
    public int goodwillBonus = 1;
    public int targetScore = 10;

    public LawnAndOrderParameters() {
        addTunableParameter("handSize", 5, Arrays.asList(3, 4, 5, 6, 7));
        addTunableParameter("nAdministrativeError", 2, Arrays.asList(0, 1, 2, 3, 4));
        addTunableParameter("nEmergencySession", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("nZeroTolerance", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("emergencySessionReveals", 2, Arrays.asList(1, 2, 3));
        addTunableParameter("zeroToleranceReduction", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("goodwillBonus", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("targetScore", 10, Arrays.asList(5, 10, 15, 20));
        addTunableParameter("maxRounds", 30);
        _reset();
    }

    @Override
    public void _reset() {
        handSize = (int) getParameterValue("handSize");
        nAdministrativeError = (int) getParameterValue("nAdministrativeError");
        nEmergencySession = (int) getParameterValue("nEmergencySession");
        nZeroTolerance = (int) getParameterValue("nZeroTolerance");
        emergencySessionReveals = (int) getParameterValue("emergencySessionReveals");
        zeroToleranceReduction = (int) getParameterValue("zeroToleranceReduction");
        goodwillBonus = (int) getParameterValue("goodwillBonus");
        targetScore = (int) getParameterValue("targetScore");
        // the framework's round limit, which StandardForwardModel.endRound applies
        setMaxRounds((int) getParameterValue("maxRounds"));
    }

    /**
     * The points for a group of cards sharing an attribute.
     */
    public int groupPoints(int nCards) {
        return GROUP_POINTS[Math.min(nCards, GROUP_POINTS.length - 1)];
    }

    /**
     * The HOA Rule cards of one round: a Standard Rule for each attribute, and the Special Rules.
     */
    public List<RuleCard> ruleCards() {
        List<RuleCard> retValue = new ArrayList<>();
        for (LawnCard.Attribute a : LawnCard.Attribute.values())
            retValue.add(RuleCard.standard(a));
        for (int i = 0; i < nAdministrativeError; i++)
            retValue.add(RuleCard.special(RuleCard.Special.ADMINISTRATIVE_ERROR));
        for (int i = 0; i < nEmergencySession; i++)
            retValue.add(RuleCard.special(RuleCard.Special.EMERGENCY_SESSION));
        for (int i = 0; i < nZeroTolerance; i++)
            retValue.add(RuleCard.special(RuleCard.Special.ZERO_TOLERANCE));
        return retValue;
    }

    @Override
    protected LawnAndOrderParameters _copy() {
        return new LawnAndOrderParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof LawnAndOrderParameters;
    }

    @Override
    public LawnAndOrderParameters instantiate() {
        return this;
    }
}

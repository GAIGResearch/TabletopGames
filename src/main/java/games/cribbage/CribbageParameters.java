package games.cribbage;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * <p>Parameters for two-player six-card Cribbage (https://www.pagat.com/adders/crib6.html).
 * All rule constants are read from here rather than hard-coded in the state or forward model.</p>
 *
 * <p><b>The defaults follow the RECYCLE description of the game where it differs from pagat.</b>
 * The pagat rules are available by changing these parameters:</p>
 * <ul>
 *     <li>playFifteenPoints: a fifteen in the play scores 1 (pagat: 2)</li>
 *     <li>runsIncludeStarter: in the show, runs are counted without the starter card (pagat: true)</li>
 *     <li>cribFlushNeedsStarter: a four-card flush in the crib scores, as in a hand (pagat: true, the crib
 *     only scores a flush if the starter matches too)</li>
 * </ul>
 */
public class CribbageParameters extends TunableParameters<CribbageParameters> {

    public int nCardsDealt = 6;
    public int nCardsToCrib = 2;

    // The brief plays two rounds, so that each player has the crib once
    public int nRounds = 2;
    // The game ends as soon as a player reaches this score (pagat: 121). 0 means no target score.
    public int targetScore = 121;

    // The maximum running total in the play
    public int maxCount = 31;

    // Fifteens: in the show, and in the play (RECYCLE: 1; pagat: 2)
    public int fifteenPoints = 2;
    public int playFifteenPoints = 1;
    // Points for pairs, in both the play and the show
    public int pairPoints = 2;
    public int pairRoyalPoints = 6;
    public int doublePairRoyalPoints = 12;

    // Points scored only in the play
    public int thirtyOnePoints = 2;
    public int lastCardPoints = 1;

    // Points scored for the starter card and in the show
    public int hisHeelsPoints = 2;
    public int hisNobsPoints = 1;
    public int flushPoints = 4;   // four hand cards of one suit; +1 if the starter matches too
    // RECYCLE: false, a four-card crib flush counts; pagat: true, a crib flush needs the starter to match
    public boolean cribFlushNeedsStarter = false;
    // RECYCLE: false, runs in the show are counted on the hand or crib alone; pagat: true
    public boolean runsIncludeStarter = false;

    public CribbageParameters() {
        addTunableParameter("nCardsDealt", 6);
        addTunableParameter("nCardsToCrib", 2);
        addTunableParameter("nRounds", 2, Arrays.asList(2, 4, 6, 8));
        addTunableParameter("targetScore", 121, Arrays.asList(0, 61, 121));
        addTunableParameter("maxCount", 31);
        addTunableParameter("fifteenPoints", 2);
        addTunableParameter("playFifteenPoints", 1, Arrays.asList(1, 2));
        addTunableParameter("pairPoints", 2);
        addTunableParameter("pairRoyalPoints", 6);
        addTunableParameter("doublePairRoyalPoints", 12);
        addTunableParameter("thirtyOnePoints", 2);
        addTunableParameter("lastCardPoints", 1);
        addTunableParameter("hisHeelsPoints", 2);
        addTunableParameter("hisNobsPoints", 1);
        addTunableParameter("flushPoints", 4);
        addTunableParameter("cribFlushNeedsStarter", false, Arrays.asList(false, true));
        addTunableParameter("runsIncludeStarter", false, Arrays.asList(false, true));
    }

    @Override
    public void _reset() {
        nCardsDealt = (int) getParameterValue("nCardsDealt");
        nCardsToCrib = (int) getParameterValue("nCardsToCrib");
        nRounds = (int) getParameterValue("nRounds");
        targetScore = (int) getParameterValue("targetScore");
        maxCount = (int) getParameterValue("maxCount");
        fifteenPoints = (int) getParameterValue("fifteenPoints");
        playFifteenPoints = (int) getParameterValue("playFifteenPoints");
        pairPoints = (int) getParameterValue("pairPoints");
        pairRoyalPoints = (int) getParameterValue("pairRoyalPoints");
        doublePairRoyalPoints = (int) getParameterValue("doublePairRoyalPoints");
        thirtyOnePoints = (int) getParameterValue("thirtyOnePoints");
        lastCardPoints = (int) getParameterValue("lastCardPoints");
        hisHeelsPoints = (int) getParameterValue("hisHeelsPoints");
        hisNobsPoints = (int) getParameterValue("hisNobsPoints");
        flushPoints = (int) getParameterValue("flushPoints");
        cribFlushNeedsStarter = (boolean) getParameterValue("cribFlushNeedsStarter");
        runsIncludeStarter = (boolean) getParameterValue("runsIncludeStarter");
    }

    @Override
    protected CribbageParameters _copy() {
        return new CribbageParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CribbageParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public CribbageParameters instantiate() {
        return this;
    }
}

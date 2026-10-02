package games.pitch;

import core.AbstractParameters;
import core.components.FrenchCard;
import evaluation.optimisation.TunableParameters;
import games.tricktaking.ITrickTakingParameters;

import java.util.Arrays;

/**
 * Rules of Pitch (Auction Pitch / Setback), as described at https://www.pagat.com/allfours/pitch.html.
 * data/pitch/Pitch_Valet.json gives the RECYCLE code at https://mgoadric.github.io/valet/post/pitch.html: a single
 * deal, with each trump card counted once.
 */
public class PitchParameters extends TunableParameters<PitchParameters> implements ITrickTakingParameters {

    // 1 = a single deal (RECYCLE). Otherwise deals continue until the pitching team makes its bid and has at
    // least this score (pagat: 21).
    public int targetScore = 21;
    public int handSize = 6;
    public int minBid = 2;
    // the highest bid; bidding it is a smudge (all six tricks and all four points)
    public int smudgeBid = 5;
    public int smudgePoints = 5;
    // RECYCLE counts each trump card once, so one card that is both High and Low (or also the Jack) scores 1.
    // Pagat scores High, Low and Jack as separate points.
    public boolean countHighLowSeparately = true;
    // values of the cards for the Game point
    public int gameValueAce = 4;
    public int gameValueKing = 3;
    public int gameValueQueen = 2;
    public int gameValueJack = 1;
    public int gameValueTen = 10;
    // if true then a player who shows a void is remembered as void, and redeterminisation respects it
    public boolean rememberVoids = true;

    public PitchParameters() {
        super();
        // a safety limit on the number of deals when playing to a target score
        setMaxRounds(100);
        addTunableParameter("targetScore", 21, Arrays.asList(1, 7, 11, 21));
        addTunableParameter("handSize", 6);
        addTunableParameter("minBid", 2);
        addTunableParameter("smudgeBid", 5);
        addTunableParameter("smudgePoints", 5);
        addTunableParameter("countHighLowSeparately", true, Arrays.asList(false, true));
        addTunableParameter("gameValueAce", 4);
        addTunableParameter("gameValueKing", 3);
        addTunableParameter("gameValueQueen", 2);
        addTunableParameter("gameValueJack", 1);
        addTunableParameter("gameValueTen", 10);
        addTunableParameter("rememberVoids", true, Arrays.asList(false, true));
    }

    @Override
    public void _reset() {
        targetScore = (int) getParameterValue("targetScore");
        handSize = (int) getParameterValue("handSize");
        minBid = (int) getParameterValue("minBid");
        smudgeBid = (int) getParameterValue("smudgeBid");
        smudgePoints = (int) getParameterValue("smudgePoints");
        countHighLowSeparately = (boolean) getParameterValue("countHighLowSeparately");
        gameValueAce = (int) getParameterValue("gameValueAce");
        gameValueKing = (int) getParameterValue("gameValueKing");
        gameValueQueen = (int) getParameterValue("gameValueQueen");
        gameValueJack = (int) getParameterValue("gameValueJack");
        gameValueTen = (int) getParameterValue("gameValueTen");
        rememberVoids = (boolean) getParameterValue("rememberVoids");
    }

    @Override
    public boolean rememberVoids() {
        return rememberVoids;
    }

    /**
     * @return the value of the card towards the Game point
     */
    public int gameValue(FrenchCard card) {
        return switch (card.type) {
            case Ace -> gameValueAce;
            case King -> gameValueKing;
            case Queen -> gameValueQueen;
            case Jack -> gameValueJack;
            default -> card.number == 10 ? gameValueTen : 0;
        };
    }

    @Override
    protected AbstractParameters _copy() {
        return new PitchParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof PitchParameters;
    }

    @Override
    public PitchParameters instantiate() {
        return this;
    }
}

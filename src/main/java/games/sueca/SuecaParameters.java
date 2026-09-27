package games.sueca;

import core.components.FrenchCard;
import evaluation.optimisation.TunableParameters;
import games.tricktaking.ITrickTakingParameters;

import java.util.Arrays;
import java.util.List;

/**
 * Parameters for Sueca (as described at https://mgoadric.github.io/valet/post/sueca.html and
 * https://www.pagat.com/aceten/sueca.html). The defaults follow the RECYCLE description of the game, a single deal;
 * playRubber gives pagat's rubber of several deals.
 */
public class SuecaParameters extends TunableParameters<SuecaParameters> implements ITrickTakingParameters {

    // Cards dealt to each player: the whole 40-card pack among four players
    public int handSize = 10;

    // If false, the game is a single deal, won by the team with more card points (RECYCLE). If true, deals are
    // played until a team has won targetGames games (pagat)
    public boolean playRubber = false;

    // Games needed to win the rubber (used only with playRubber)
    public int targetGames = 4;

    // Whether players remember the suits others have failed to follow (used when redeterminising)
    public boolean rememberVoids = true;

    public SuecaParameters() {
        addTunableParameter("handSize", 10);
        addTunableParameter("playRubber", false, List.of(false, true));
        addTunableParameter("targetGames", 4, Arrays.asList(1, 2, 4, 8));
        addTunableParameter("rememberVoids", true, List.of(false, true));
        _reset();
    }

    @Override
    public void _reset() {
        handSize = (int) getParameterValue("handSize");
        playRubber = (boolean) getParameterValue("playRubber");
        targetGames = (int) getParameterValue("targetGames");
        rememberVoids = (boolean) getParameterValue("rememberVoids");
    }

    public int cardPoints(FrenchCard card) {
        // 120 in the pack
        return switch (card.number) {
            case 14 -> 11;  // Ace
            case 7 -> 10;
            case 13 -> 4;   // King
            case 11 -> 3;   // Jack
            case 12 -> 2;   // Queen
            default -> 0;
        };
    }

    @Override
    public boolean rememberVoids() {
        return rememberVoids;
    }

    @Override
    protected SuecaParameters _copy() {
        return new SuecaParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof SuecaParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public SuecaParameters instantiate() {
        return this;
    }
}

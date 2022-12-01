package games.pandemic;

import core.AbstractGameState;
import core.Game;
import core.components.BoardNode;
import core.components.Counter;
import core.interfaces.IGameMetric;
import core.properties.PropertyIntArray;
import evaluation.GameListener;
import evaluation.metrics.Event;
import utilities.Hash;

import java.util.function.BiFunction;
import java.util.function.Function;

import static games.pandemic.PandemicConstants.colors;
import static games.pandemic.PandemicConstants.infectionHash;
import static utilities.Utils.GameResult.WIN;

public enum PandemicCompetitionRankingAttributes implements IGameMetric {
    GAME_WIN((l, e) -> Math.max(0,e.state.getGameStatus().value)),
    // Exception: game ticks lower are better if win rate > winNeutralRange[1],
    //              and higher are better if win rate < winNeutralRange[0] (otherwise considered equal)
    // ~ reward finishing winning games quickly, but finishing losing games slowly (with neutral range allowance)
    GAME_TICKS((l, e) -> e.state.getGameStatus()==WIN? e.state.getGameTick() : -e.state.getGameTick()),
    GAME_TICKS_RAW((l,e) -> e.state.getGameTick()),
    N_DISEASE_CURED((l, e) -> countDisease(e.state, 1, false)+countDisease(e.state, 2, false)),
    N_OUTBREAKS((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(PandemicConstants.outbreaksHash)).getValue()),
    N_CITY_DANGER(PandemicCompetitionRankingAttributes::countCityDanger),
    N_DISEASE_CUBES_LEFT((l, e)-> countDisease(e.state, 0, true)),
    N_DISEASE_ERADICATED((l, e) -> countDisease(e.state, 2, false));

    private final BiFunction<PandemicCompetitionListener, Event, Object> lambda;

    PandemicCompetitionRankingAttributes(BiFunction<PandemicCompetitionListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event)
    {
        return lambda.apply((PandemicCompetitionListener) listener, event);
    }

    static int countDisease(AbstractGameState state, int targetValue, boolean cubes) {
        PandemicGameState pgs = (PandemicGameState) state;
        int count = 0;
        for (String color: colors) {
            if (cubes) {
                count += ((Counter) pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + color))).getValue();
            } else {
                if (((Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + color))).getValue() == targetValue)
                    count++;
            }
        }
        return count;
    }

    static int countCityDanger(PandemicCompetitionListener listener, Event event) {
        PandemicGameState pgs = (PandemicGameState) event.state;
        PandemicParameters pp = (PandemicParameters) pgs.getGameParameters();
        int count = 0;

        for (BoardNode bn: pgs.world.getBoardNodes()) {
            PropertyIntArray infectionArray = (PropertyIntArray) bn.getProperty(infectionHash);
            int[] array = infectionArray.getValues();
            for (int a: array) {
                if (a >= pp.maxCubesPerCity-1) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

}

package games.pandemic;

import core.Game;
import core.components.BoardNode;
import core.components.Counter;
import core.interfaces.IGameAttribute;
import core.properties.PropertyIntArray;
import utilities.Hash;

import java.util.function.Function;

import static games.pandemic.PandemicConstants.colors;
import static games.pandemic.PandemicConstants.infectionHash;
import static utilities.Utils.GameResult.WIN;

public enum PandemicCompetitionRankingAttributes implements IGameAttribute {
    GAME_WIN((s) -> Math.max(0,s.getGameState().getGameStatus().value)),
    // Exception: game ticks lower are better if win rate > winNeutralRange[1],
    //              and higher are better if win rate < winNeutralRange[0] (otherwise considered equal)
    // ~ reward finishing winning games quickly, but finishing losing games slowly (with neutral range allowance)
    GAME_TICKS((s) -> s.getGameState().getGameStatus()==WIN? s.getTick() : -s.getTick()),
    GAME_TICKS_RAW(Game::getTick),
    N_DISEASE_CURED((s) -> countDisease(s, 1, false)+countDisease(s, 2, false)),
    N_OUTBREAKS((s) -> ((Counter) ((PandemicGameState)s.getGameState()).getComponent(PandemicConstants.outbreaksHash)).getValue()),
    N_CITY_DANGER(PandemicCompetitionRankingAttributes::countCityDanger),
    N_DISEASE_CUBES_LEFT((s)-> countDisease(s, 0, true)),
    N_DISEASE_ERADICATED((s) -> countDisease(s, 2, false));

    private final Function<Game, Object> lambda;

    PandemicCompetitionRankingAttributes(Function<Game, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(Game game) {
        return lambda.apply(game);
    }

    static int countDisease(Game game, int targetValue, boolean cubes) {
        PandemicGameState pgs = (PandemicGameState) game.getGameState();
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

    static int countCityDanger(Game game) {
        PandemicGameState pgs = (PandemicGameState) game.getGameState();
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

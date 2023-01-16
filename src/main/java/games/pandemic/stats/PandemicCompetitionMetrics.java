package games.pandemic.stats;
import core.AbstractGameState;
import core.components.BoardNode;
import core.components.Counter;
import core.properties.PropertyIntArray;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import evaluation.metrics.IMetricsCollection;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import utilities.Hash;

import static games.pandemic.PandemicConstants.*;
import static core.CoreConstants.GameResult.WIN;
public class PandemicCompetitionMetrics implements IMetricsCollection {

    public static class GameWin extends AbstractMetric {

        public GameWin() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return Math.max(0,e.state.getGameStatus().value);
        }
    }
    
    public static class GameTicks extends AbstractMetric {

        public GameTicks() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return e.state.getGameStatus()==WIN? e.state.getGameTick() : -e.state.getGameTick();
        }
    }

    public static class GameTicksRaw extends AbstractMetric {

        public GameTicksRaw() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return e.state.getGameTick();
        }
    }

    public static class NumCuredDiseases extends AbstractMetric {

        public NumCuredDiseases() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return countDisease(e.state, 1, false)+countDisease(e.state, 2, false);
        }
    }

    public static class NumOutbreaks extends AbstractMetric {

        public NumOutbreaks() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((Counter) ((PandemicGameState)e.state).getComponent(PandemicConstants.outbreaksHash)).getValue();
        }
    }

    public static class NumEndangeredCities extends AbstractMetric {

        public NumEndangeredCities() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return countCityDanger(listener, e);
        }
    }

    public static class NumDiseaseCubesLeft extends AbstractMetric {

        public NumDiseaseCubesLeft() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return countDisease(e.state, 0, true);
        }
    }

    public static class NumErradicatedDiseases extends AbstractMetric {

        public NumErradicatedDiseases() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return countDisease(e.state, 2, false);
        }
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

    static int countCityDanger(GameListener listener, Event event) {
        PandemicGameState pgs = (PandemicGameState) event.state;
        PandemicParameters pp = (PandemicParameters) pgs.getGameParameters();
        int count = 0;

        for (BoardNode bn: pgs.getWorld().getBoardNodes()) {
            PropertyIntArray infectionArray = (PropertyIntArray) bn.getProperty(infectionHash);
            int[] array = infectionArray.getValues();
            for (int a: array) {
                if (a >= pp.getMaxCubesPerCity() -1) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }
}

package evaluation.metrics;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.Game;
import core.components.Component;
import core.components.Counter;
import core.interfaces.IComponentContainer;
import evaluation.summarisers.TAGStatSummary;
import evaluation.summarisers.TAGSummariser;
import games.pandemic.PandemicGameState;
import utilities.Hash;
import utilities.Pair;

import java.util.*;
public class GameMetrics
{
    public static class GameID extends AbstractMetric{

        public GameID() {addEventType(Event.GameEvent.ABOUT_TO_START);}

        @Override
        public Object run(GameListener listener, Event e) {
            return e.state.getGameID();
        }
    }


    public static class GameName extends AbstractMetric{

        public GameName() {addEventType(Event.GameEvent.ABOUT_TO_START);}

        @Override
        public Object run(GameListener listener, Event e) {
            return listener.getGame().getGameType().name();
        }
    }


    public static class GameSeeds extends AbstractMetric{

        public GameSeeds() {addEventType(Event.GameEvent.ABOUT_TO_START);}

        @Override
        public Object run(GameListener listener, Event e) {
            return e.state.getGameParameters().getRandomSeed();
        }
    }

    public static class GameStatus extends AbstractMetric{

        public GameStatus() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return e.state.getGameStatus();
        }
    }

    public static class GameScore extends AbstractMetric{

        public GameScore() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

        @Override
        public Object run(GameListener listener, Event e) {
            int player = e.state.getCurrentPlayer();
            return e.state.getGameScore(player);
        }
    }

    public static class ActionSpace extends AbstractMetric{

        public ActionSpace() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

        @Override
        public Object run(GameListener listener, Event e) {
            Game g = listener.getGame();
            AbstractForwardModel fm = g.getForwardModel();
            return fm.computeAvailableActions(g.getGameState()).size();
        }
    }

    public static class StateSize extends AbstractMetric{

        public StateSize() {
            addEventType(Event.GameEvent.ACTION_CHOSEN);
            addEventType(Event.GameEvent.ABOUT_TO_START);
        }

        @Override
        public Object run(GameListener listener, Event e) {
            int components = countComponents(e.state).a;
            return (double) components;
        }
    }

    public static class CurrentPlayer extends AbstractMetric{

        public CurrentPlayer() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return e.state.getCurrentPlayer();
        }
    }

    public static class CurrentPlayerVisibility extends AbstractMetric{

        public CurrentPlayerVisibility() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

        @Override
        public Object run(GameListener listener, Event e) {
            AbstractGameState gs = e.state;
            int player = gs.getCurrentPlayer();
            Pair<Integer, int[]> allComp = countComponents(gs);
            return (allComp.b[player] / (double) allComp.a);
        }
    }

    public static class ComputationTimes extends AbstractMetric{

        public ComputationTimes() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            Map<String, Object> collectedData = new LinkedHashMap<>();
            collectedData.put("TimeNext", listener.getGame().getNextTime() / 1e3);
            collectedData.put("TimeCopy", listener.getGame().getCopyTime() / 1e3);
            collectedData.put("TimeActionCompute", listener.getGame().getActionComputeTime() / 1e3);
            collectedData.put("TimeAgent", listener.getGame().getAgentTime() / 1e3);
            return collectedData;
        }
    }

    public static class GameDuration extends AbstractMetric{

        public GameDuration() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            Map<String, Object> collectedData = new LinkedHashMap<>();
            collectedData.put("Turns", listener.getGame().getGameState().getTurnOrder().getTurnCounter());
            collectedData.put("Ticks", listener.getGame().getTick());
            collectedData.put("Rounds", listener.getGame().getGameState().getTurnOrder().getRoundCounter());
            return collectedData;
        }
    }

    public static class OrdinalPosition extends AbstractMetric{

        public OrdinalPosition() {addEventType(Event.GameEvent.GAME_OVER);recordPerPlayer = true;}

        @Override
        public Object run(GameListener listener, Event e) {
            return e.state.getOrdinalPosition(e.playerID);
        }
    }

    public static class PlayerType extends AbstractMetric{

        public PlayerType() {addEventType(Event.GameEvent.GAME_OVER);recordPerPlayer = true;}

        @Override
        public Object run(GameListener listener, Event e) {
            return listener.game.getPlayers().get(e.playerID).toString();
        }
    }

    public static class Decisions extends AbstractMetric{

        public Decisions() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            List<Pair<Integer, Integer>> actionSpaceRecord = listener.getGame().getActionSpaceSize();
            TAGStatSummary statsDecisionsAll = actionSpaceRecord.stream()
                    .map(r -> r.b)
                    .collect(new TAGSummariser());
            TAGStatSummary statsDecisions = actionSpaceRecord.stream()
                    .map(r -> r.b)
                    .filter(size -> size > 1)
                    .collect(new TAGSummariser());

            Map<String, Object> collectedData = new LinkedHashMap<>();
            collectedData.put("ActionsPerTurnSum", listener.getGame().getNActionsPerTurn());
            collectedData.put("Decisions", statsDecisions.n());
            collectedData.put("DecisionPointsMean", statsDecisions.n() * 1.0 / statsDecisionsAll.n());
            return collectedData;
        }
    }

    public static class ActionTypes extends AbstractMetric {

        public ActionTypes() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

        @Override
        public Object run(GameListener listener, Event e) {

            Map<String, Object> collectedData = new LinkedHashMap<>();
            collectedData.put("ActionsType", e.action == null ? "NONE" : e.action.getClass().getSimpleName());
            collectedData.put("ActionsDescription", e.action == null ? "NONE" : e.action.getString(e.state));
            return collectedData;
        }
    }


    /**
     * Returns the total number of components in the state as the first element of the returned value
     * and an array of the counts that are hidden to each player
     * <p>
     *
     * @param state where components are to be counted.
     * @return The total number of components
     */
    public static Pair<Integer, int[]> countComponents(AbstractGameState state) {
        int[] hiddenByPlayer = new int[state.getNPlayers()];
        // we do not include containers in the count...just the lowest-level items
        // open to debate on this. But we are consistent across State Size and Hidden Information stats
        int total = (int) state.getAllComponents().stream().filter(c -> !(c instanceof IComponentContainer)).count();
        for (int p = 0; p < hiddenByPlayer.length; p++)
            hiddenByPlayer[p] = state.getUnknownComponentsIds(p).size();
        return new Pair<>(total, hiddenByPlayer);
    }
}

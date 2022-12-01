package evaluation.metrics;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IComponentContainer;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import utilities.Pair;

import java.util.List;
import java.util.function.BiFunction;

public enum GameStatisticsAttributes implements IGameMetric {
    DECISION_POINTS((l, e) ->
    {
        AbstractForwardModel fm = e.game.getForwardModel();
        List<AbstractAction> allActions = fm.computeAvailableActions(e.game.getGameState());
        if (allActions.size() < 2)
            return 0.0;
        else
            return 1.0;

    }),
    SCORES((l, e) ->
    {
        AbstractGameState gs = e.game.getGameState();
        int player = gs.getCurrentPlayer();
        return gs.getGameScore(player);
    }),
    COMPONENTS ((l, e) -> {
        Pair<Integer, int[]> allComp = countComponents(e.game.getGameState());
        return (double) allComp.a;
    }),
    VISIBILITY((l, e) ->
    {
        AbstractGameState gs = e.game.getGameState();
        int player = gs.getCurrentPlayer();
        Pair<Integer, int[]> allComp = countComponents(gs);
         return (allComp.b[player] / (double) allComp.a);
    });

    static Pair<Integer, int[]> countComponents(AbstractGameState state)
    {
        int[] hiddenByPlayer = new int[state.getNPlayers()];
        int total = (int) state.getAllComponents().stream().filter(c -> !(c instanceof IComponentContainer)).count();
        for (int p = 0; p < hiddenByPlayer.length; p++)
            hiddenByPlayer[p] = state.getUnknownComponentsIds(p).size();
        return new Pair<>(total, hiddenByPlayer);
    }

    private final BiFunction<GameStatisticsListener, Event, Double> lambda;
    GameStatisticsAttributes(BiFunction<GameStatisticsListener, Event, Double> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply((GameStatisticsListener) listener, event);
    }
}

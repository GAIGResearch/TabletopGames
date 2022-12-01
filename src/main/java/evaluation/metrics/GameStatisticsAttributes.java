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
        AbstractForwardModel fm = l.getForwardModel();
        List<AbstractAction> allActions = fm.computeAvailableActions(e.state);
        int decision = allActions.size() < 2 ? 0 : 1;
        l.addDecision(decision);
        return (double)decision;
    }),
    SCORES((l, e) ->
    {
        int player = e.state.getCurrentPlayer();
        double score = e.state.getGameScore(player);
        l.addScore(score);
        return score;
    }),
    COMPONENTS ((l, e) -> {
        int components = countComponents(e.state).a;
        l.addComponent(components);
        return (double) components;
    }),
    VISIBILITY((l, e) ->
    {
        AbstractGameState gs = e.state;
        int player = gs.getCurrentPlayer();
        Pair<Integer, int[]> allComp = countComponents(gs);
        double visibilityPerc = (allComp.b[player] / (double) allComp.a);
        l.addVisibility(visibilityPerc);
        return visibilityPerc;
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

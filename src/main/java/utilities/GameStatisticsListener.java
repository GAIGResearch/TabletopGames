package utilities;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IComponentContainer;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;

import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.GameEvents.*;

public class GameStatisticsListener implements IGameListener {

    List<Double> scores = new ArrayList<>();
    List<Double> visibilityOnTurn = new ArrayList<>();
    List<Integer> components = new ArrayList<>();
    Map<String, Object> collectedData = new LinkedHashMap<>();
    AbstractForwardModel fm;
    IStatisticLogger logger;

    public GameStatisticsListener(IStatisticLogger logger) {
        this.logger = logger;
    }
    public GameStatisticsListener() {
        this.logger = null;
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction actionChosen) {
        if (type == ACTION_CHOSEN) {
            // each action taken, we record branching factor and states (this is triggered when the decision is made,
            // so before it is executed
            int player = state.getCurrentPlayer();
            if (fm == null) {
                throw new AssertionError("We have not yet received an ABOUT_TO_START event to initialise the required ForwardModel");
            }
            List<AbstractAction> allActions = fm.computeAvailableActions(state);
            if (allActions.size() < 2) return;

            scores.add(state.getGameScore(player));
            Pair<Integer, int[]> allComp = countComponents(state);
            components.add(allComp.a);
            visibilityOnTurn.add(allComp.b[player] / (double) allComp.a);
        }
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == ABOUT_TO_START) {
            AbstractGameState state = game.getGameState();
            long s = System.nanoTime();
            fm = game.getForwardModel();

            collectedData.put("Game", game.getGameState().getGameID());
            collectedData.put("Players", String.valueOf(game.getGameState().getNPlayers()));
            collectedData.put("PlayerType", game.getPlayers().get(0).toString());
            fm.setup(state);
            collectedData.put("TimeSetup", (System.nanoTime() - s) / 1e3);

            Pair<Integer, int[]> components = countComponents(state);
            collectedData.put("StateSizeStart", components.a);
            collectedData.put("HiddenInfoStart", Arrays.stream(components.b).sum() / (double) components.a / state.getNPlayers());
        } else if (type == GAME_OVER) {
            //    Retrieves a list with one entry per game tick, each a pair (active player ID, # actions)
            List<Pair<Integer, Integer>> actionSpaceRecord = game.getActionSpaceSize();
            TAGStatSummary stats = actionSpaceRecord.stream()
                    .map(r -> r.b)
                    .filter(size -> size > 1)
                    .collect(new TAGSummariser());
            collectedData.put("ActionSpaceMean", stats.mean());
            collectedData.put("ActionSpaceMin", stats.min());
            collectedData.put("ActionSpaceMedian", stats.median());
            collectedData.put("ActionSpaceMax", stats.max());
            collectedData.put("ActionSpaceSkew", stats.skew());
            collectedData.put("ActionSpaceKurtosis", stats.kurtosis());
            collectedData.put("ActionSpaceVarCoeff", Math.abs(stats.sd() / stats.mean()));
            collectedData.put("Decisions", stats.n());
            collectedData.put("TimeNext", game.getNextTime() / 1e3);
            collectedData.put("TimeCopy", game.getCopyTime() / 1e3);
            collectedData.put("TimeActionCompute", game.getActionComputeTime() / 1e3);
            collectedData.put("TimeAgent", game.getAgentTime() / 1e3);

            collectedData.put("Ticks", game.getTick());
            collectedData.put("Rounds", game.getGameState().getTurnOrder().getRoundCounter());
            collectedData.put("ActionsPerTurn", game.getNActionsPerTurn());

            TAGStatSummary sc = scores.stream().collect(new TAGSummariser());
            collectedData.put("ScoreMedian", sc.median());
            collectedData.put("ScoreMean", sc.mean());
            collectedData.put("ScoreMax", sc.max());
            collectedData.put("ScoreMin", sc.min());
            collectedData.put("ScoreVarCoeff", Math.abs(sc.sd() / sc.mean()));
            TAGStatSummary scoreDelta = scores.size() > 1 ?
                    IntStream.range(0, scores.size() - 1)
                            .mapToObj(i -> !scores.get(i + 1).equals(scores.get(i)) ? 1.0 : 0.0)
                            .collect(new TAGSummariser())
                    : new TAGStatSummary();
            collectedData.put("ScoreDelta", scoreDelta.mean()); // percentage of actions that lead to a change in score

            TAGStatSummary stateSize = components.stream().collect(new TAGSummariser());
            collectedData.put("StateSizeMedian", stateSize.median());
            collectedData.put("StateSizeMean", stateSize.mean());
            collectedData.put("StateSizeMax", stateSize.max());
            collectedData.put("StateSizeMin", stateSize.min());
            collectedData.put("StateSizeVarCoeff", Math.abs(stateSize.sd() / stateSize.mean()));

            TAGStatSummary visibility = visibilityOnTurn.stream().collect(new TAGSummariser());
            collectedData.put("HiddenInfoMedian", visibility.median());
            collectedData.put("HiddenInfoMean", visibility.mean());
            collectedData.put("HiddenInfoMax", visibility.max());
            collectedData.put("HiddenInfoMin", visibility.min());
            collectedData.put("HiddenInfoVarCoeff", Math.abs(visibility.sd() / visibility.mean()));

            logger.record(collectedData);
            collectedData = new HashMap<>();
        }
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }

    /**
     * Returns the total number of components in the state as the first element of the returned value
     * and an array of the counts that are hidden to each player
     * <p>
     *
     * @param state
     * @return The total number of components
     */
    private Pair<Integer, int[]> countComponents(AbstractGameState state) {
        int[] hiddenByPlayer = new int[state.getNPlayers()];
        // we do not include containers in the count...just the lowest-level items
        // open to debate on this. But we are consistent across State Size and Hidden Information stats
        int total = (int) state.getAllComponents().stream().filter(c -> !(c instanceof IComponentContainer)).count();
        for (int p = 0; p < hiddenByPlayer.length; p++)
            hiddenByPlayer[p] = state.getUnknownComponentsIds(p).size();
        return new Pair<>(total, hiddenByPlayer);
    }

}

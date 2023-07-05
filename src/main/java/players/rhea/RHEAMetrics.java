package players.rhea;

import core.AbstractPlayer;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;

import java.util.*;

public class RHEAMetrics implements IMetricsCollection {

    public static class RHEAStats extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> stats) {
            AbstractPlayer player = listener.getGame().getPlayers().get(e.state.getCurrentPlayer());
            if (player instanceof RHEAPlayer) {
                RHEAPlayer rheaPlayer = (RHEAPlayer) player;
                stats.put("iterations", rheaPlayer.numIters);
                stats.put("fmCalls", rheaPlayer.numIters == 0 ? 0 : rheaPlayer.fmCalls / rheaPlayer.numIters);
                stats.put("copyCalls", rheaPlayer.numIters == 0 ? 0 : rheaPlayer.copyCalls / rheaPlayer.numIters);
                stats.put("time", rheaPlayer.timeTaken);
                stats.put("timePerIteration", rheaPlayer.timePerIteration);
                stats.put("initTime", rheaPlayer.initTime);
                stats.put("hiReward", rheaPlayer.numIters == 0 ? 0 : rheaPlayer.population.get(0).value);
                stats.put("loReward", rheaPlayer.numIters == 0 ? 0 : rheaPlayer.population.get(rheaPlayer.population.size() - 1).value);
                stats.put("medianReward", rheaPlayer.numIters == 0 ? 0 : rheaPlayer.population.size() == 1 ?
                        rheaPlayer.population.get(0).value :
                        rheaPlayer.population.get(rheaPlayer.population.size() / 2 - 1).value);
                stats.put("repairProportion", rheaPlayer.repairCount == 0 ? 0.0 : rheaPlayer.repairCount / (double) (rheaPlayer.repairCount + rheaPlayer.nonRepairCount));
                stats.put("repairsPerIteration", rheaPlayer.repairCount == 0 ? 0.0 : rheaPlayer.repairCount / (double) rheaPlayer.numIters);
                return true;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.ACTION_CHOSEN));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> stats = new LinkedHashMap<>();
            stats.put("iterations", Integer.class);
            stats.put("fmCalls", Integer.class);
            stats.put("copyCalls", Integer.class);
            stats.put("time", Double.class);
            stats.put("timePerIteration", Double.class);
            stats.put("initTime", Double.class);
            stats.put("hiReward", Double.class);
            stats.put("loReward", Double.class);
            stats.put("medianReward", Double.class);
            stats.put("repairProportion", Double.class);
            stats.put("repairsPerIteration", Double.class);
            return stats;
        }
    }
}

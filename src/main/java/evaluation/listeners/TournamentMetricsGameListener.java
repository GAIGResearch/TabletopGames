package evaluation.listeners;

import core.AbstractPlayer;
import core.Game;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.TournamentMetric;
import evaluation.metrics.IDataLogger;

import java.util.Arrays;
import java.util.Set;


import static evaluation.metrics.IDataLogger.ReportDestination.ToBoth;
import static evaluation.metrics.IDataLogger.ReportType.*;

/**
 * Use for tournaments when you want to record all metrics separately for each combination of players
 */
public class TournamentMetricsGameListener extends MetricsGameListener {

    public TournamentMetricsGameListener(AbstractMetric[] metrics) {
        this(ToBoth, metrics);
    }

    public TournamentMetricsGameListener(IDataLogger.ReportDestination logTo, AbstractMetric[] metrics) {
        this(logTo, new IDataLogger.ReportType[]{RawData, Summary, Plot}, metrics);
    }

    public TournamentMetricsGameListener(IDataLogger.ReportDestination logTo, IDataLogger.ReportType[] dataTypes, AbstractMetric[] metrics) {
        super(logTo, dataTypes, Arrays.stream(metrics).map(TournamentMetric::new).toArray(AbstractMetric[]::new));
    }

    public void tournamentInit(Game game, int nPlayersPerGame, Set<String> playerNames, Set<AbstractPlayer> matchup) {
        for (AbstractMetric metric : metrics.values()) {
            TournamentMetric tournamentMetric = (TournamentMetric) metric;
            tournamentMetric.tournamentInit(game, nPlayersPerGame, playerNames, matchup);
        }
    }

}

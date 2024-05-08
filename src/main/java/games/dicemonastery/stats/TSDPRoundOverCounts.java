package games.dicemonastery.stats;

import evaluation.metrics.AbstractMetric;
import evaluation.metrics.tablessaw.TableSawDataProcessor;
import evaluation.summarisers.TAGNumericStatSummary;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.traces.BoxTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.util.*;

/**
 * on Round Over:
 columns.put("Writing Sets - Player " + i , Integer.class);
 columns.put("Pigments - Player " + i, Integer.class);
 columns.put("Inks - Player " + i, Integer.class);
 columns.put("Monks Count - Player " + i, Integer.class);
 columns.put("Piety Sum - Player " + i, Integer.class);
 columns.put("Victory Points - Player " + i, Integer.class);
 columns.put("Treasure Sum - Player " + i, Integer.class);
 */
public class TSDPRoundOverCounts extends TableSawDataProcessor {

    @Override
    protected Map<String, Figure> plotDataProgression(AbstractMetric metric, Table data) {
        String[] colors = new String[] {
                "rgb(31, 119, 180)",
                "rgb(255, 127, 14)",
                "rgb(44, 160, 44)",
                "rgb(214, 39, 40)"
        };

        DiceMonasteryMetrics.RoundOverCounts metricCounts = (DiceMonasteryMetrics.RoundOverCounts) metric;
        List<String> categories = new ArrayList<>(metricCounts.allCategories.keySet());

        Object[] gameIds = data.column("GameID").unique().asObjectArray();
        int nGames = gameIds.length;
        int maxRound = 0;
        Table[] tablesPerGame = new Table[nGames];

        int i = 0;
        for (Object id: gameIds) {
            tablesPerGame[i] = data.where(data.stringColumn("GameID").isEqualTo((String) id));
            int maxRoundsThisGame = tablesPerGame[i].intColumn("Round").countUnique();
            if (maxRoundsThisGame > maxRound) {
                maxRound = maxRoundsThisGame;
            }
            i++;
        }

        Map<String, Figure> figures = new HashMap<>();

        // One plot per category including all players, progression through game at each end of round
        for (i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            Trace[] traces = new Trace[metricCounts.nPlayers*3];
            int t = 0;

            for (int p = 0; p < metricCounts.nPlayers; p++) {
                String columnName = category + " - Player " + p;

                double[] x = new double[maxRound];
                for (int j = 0; j < maxRound; j++) {
                    x[j] = j;
                }
                double[] yMean = new double[maxRound];
                double[] yMeanSdMinus = new double[maxRound];
                double[] yMeanSdPlus = new double[maxRound];
                for (int j = 0; j < maxRound; j++) {
                    TAGNumericStatSummary ss = new TAGNumericStatSummary();
                    for (int k = 0; k < nGames; k++) {
                        Column<?> columnThisGame = tablesPerGame[k].column(columnName);
                        if (columnThisGame.size() > j) {
                            ss.add(Double.parseDouble(String.valueOf(columnThisGame.get(j))));
                        }
                    }

                    if (ss.n() > 0) {
                        double err = 0;
                        if (ss.n() > 1) err = ss.stdErr();
                        yMean[j] = ss.mean();
                        yMeanSdMinus[j] = yMean[j] - err;
                        yMeanSdPlus[j] = yMean[j] + err;
                    } else {
                        yMean[j] = 0;
                        yMeanSdMinus[j] = 0;
                        yMeanSdPlus[j] = 0;
                    }
                }

                traces[t++] = ScatterTrace.builder(x, yMeanSdPlus)
                        .name("P" + p + " Mean + SD")
                        .opacity(0.05)
                        .legendGroup("Player " + p)
                        .line(Line.builder().simplify(true).dash(Line.Dash.DASH_DOT).color(colors[p]).build())
                        .mode(ScatterTrace.Mode.LINE).build();
                traces[t++] = ScatterTrace.builder(x, yMean)
                        .name("P" + p + " Mean")
//                        .name("Player " + p)
                        .legendGroup("Player " + p)
                        .mode(ScatterTrace.Mode.LINE)
                        .line(Line.builder().width(2).color(colors[p]).build())
                        .build();
                traces[t++] = ScatterTrace.builder(x, yMeanSdMinus)
                        .name("P" + p + " Mean - SD")
                        .opacity(0.05)
                        .legendGroup("Player " + p)
                        .line(Line.builder().simplify(true).dash(Line.Dash.DASH_DOT).color(colors[p]).build())
                        .mode(ScatterTrace.Mode.LINE).build();
            }

            Layout layout = Layout.builder().title(data.name())
                    .height(600).width(800)
                    .yAxis(Axis.builder().title(category).build())
                    .xAxis(Axis.builder().title(getLabel(metric)).build())
                    .build();

            Figure figure = new Figure(layout, traces);
            figures.put(category, figure);
        }


        // One boxplot per player, showing distribution for each category. Plus one boxplot overlapping all players.
        Trace[] traces = new Trace[metricCounts.nPlayers];
        for (int p = 0; p < metricCounts.nPlayers; p++) {
            String playerName = "Player " + p;
            List<Integer> yInt = new ArrayList<>();
            List<Object> xCateg = new ArrayList<>();

            for (i = 0; i < categories.size(); i++) {
                String category = categories.get(i);
                String columnName = category + " - Player " + p;
                yInt.addAll(data.intColumn(columnName).asList());
                for (int j = 0; j < data.column(columnName).size(); j++) {
                    xCateg.add(category);
                }
            }

            double[] y = new double[yInt.size()];
            for (i = 0; i < yInt.size(); i++) {
                y[i] = yInt.get(i);
            }

            Trace t = BoxTrace.builder(xCateg.toArray(), y)
                    .name(playerName)
                    .build();
            traces[p] = t;

            Layout layout = Layout.builder().title(metric.getName() + " " + playerName)
                    .height(600).width(800)
                    .yAxis(Axis.builder().title("Count").build())
                    .xAxis(Axis.builder().title("Item").build())
                    .build();

            figures.put(playerName, new Figure(layout, t));
        }

        Layout layout = Layout.builder().title(metric.getName() + " Summary")
                .height(600).width(800)
                .yAxis(Axis.builder().title("Count").build())
                .xAxis(Axis.builder().title("Item").build())
                .build();

        Figure figure = new Figure(layout, traces);
        figures.put(metric.getName() + " Summary", figure);

        return figures;
    }
}

package evaluation.tournaments;

import utilities.Pair;

import java.util.*;
import java.util.stream.DoubleStream;

/**
 * Standard errors for tournaments that play more than one game on each seed: exhaustive and onevsall modes, and any
 * mode with distinctRandomSeeds / seedFile.
 * <p>
 * Games on the same seed are not independent, so the Normal approximation (all games independent) can be too wide
 * or too narrow. The seed-clustered SE treats the seed as the independent unit, and is valid whatever the dependence
 * between games on the same seed.
 * <p>
 * Notation: n seeds; g cells per seed (a cell is a seating plus the player position of interest); r games per cell
 * per seed (r > 1 when distinctRandomSeeds is used, as each seating then plays all its games on the one seed).
 * The design is balanced when every cell has r games on every seed (exhaustive mode, or onevsall with two agents).
 * The clustered SE does not need balance; the stratified SE and the within-seed correlation rho do.
 * <p>
 * As an IResultsAnalysis this ranks agents as WinRateAnalysis / OrdinalAnalysis, but reports the
 * seed-clustered SE. It falls back to the Normal SE for any agent with fewer than two seeds.
 */
public class SeedClusteredAnalysis implements IResultsAnalysis {

    // the head-to-head tables (pairwise score, and 2P position effect for each pair) are long; set true to report them
    private static final boolean REPORT_PAIRS = false;

    private final boolean ordinal;

    /**
     * @param ordinal - if true, analyse the ordinal position, otherwise points (1 for a win, 1/k for a k-way tie)
     */
    public SeedClusteredAnalysis(boolean ordinal) {
        this.ordinal = ordinal;
    }

    /**
     * Seed-clustered statistics for the mean of y over one set of games.
     */
    public static class ClusterStats {
        public final double mean;
        public final int nObs, seeds, cells, replicates;
        public final boolean balanced;  // every cell has the same number of games on every seed
        public final double seNormal;   // all games independent
        public final double seStrat;    // balanced over cells, but games independent
        public final double seClu;      // seed is the independent unit
        public final double rhoBar;     // implied average within-seed correlation between cells
        final double rhoNum, rhoDen;    // rhoBar = rhoNum / rhoDen; kept to pool across pairs

        ClusterStats(double mean, int nObs, int seeds, int cells, int replicates, boolean balanced, double seNormal,
                     double seStrat, double seClu, double rhoNum, double rhoDen) {
            this.mean = mean;
            this.nObs = nObs;
            this.seeds = seeds;
            this.cells = cells;
            this.replicates = replicates;
            this.balanced = balanced;
            this.seNormal = seNormal;
            this.seStrat = seStrat;
            this.seClu = seClu;
            this.rhoNum = rhoNum;
            this.rhoDen = rhoDen;
            this.rhoBar = rhoDen > 0 ? rhoNum / rhoDen : Double.NaN;
        }

        // the clustered SE is available (the stratified SE and rho also need the design to be balanced)
        public boolean valid() {
            return seeds >= 2 && !Double.isNaN(seClu);
        }

        public double cluOverStrat() {
            return seStrat > 0 ? seClu / seStrat : Double.NaN;
        }

        public double cluOverNormal() {
            return seNormal > 0 ? seClu / seNormal : Double.NaN;
        }
    }

    /**
     * Two-player head-to-head breakdown for agent A against agent B, from A's perspective.
     * delta = p0 - p1, A's win rate in position 0 minus in position 1.
     */
    public static class TwoPlayerPair {
        public final double p0, p1, rho, delta, deltaSeNormal, deltaSePaired;

        TwoPlayerPair(double p0, double p1, double rho, double deltaSeNormal, double deltaSePaired) {
            this.p0 = p0;
            this.p1 = p1;
            this.rho = rho;
            this.delta = p0 - p1;
            this.deltaSeNormal = deltaSeNormal;
            this.deltaSePaired = deltaSePaired;
        }
    }

    public static class Report {
        public final Map<String, ClusterStats> agentWin = new LinkedHashMap<>();
        public final Map<String, ClusterStats> agentOrdinal = new LinkedHashMap<>();
        public final Map<Pair<String, String>, ClusterStats> pairs = new LinkedHashMap<>();
        public final Map<Pair<String, String>, TwoPlayerPair> pairs2P = new LinkedHashMap<>();
        public final List<ClusterStats> positions = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public int seeds, games;
        public int replicates;    // games per cell per seed; 0 if any agent's design is not balanced
        public double gameRho = Double.NaN;          // pooled over all head-to-head pairs
        public double medianCluOverStrat = Double.NaN;  // over agent win rates
        public double medianCluOverNormal = Double.NaN; // over agent win rates
        public boolean recommendRandom;

        public double positionDelta(int position) {
            double meanShare = positions.stream().mapToDouble(s -> s.mean).average().orElse(Double.NaN);
            return positions.get(position).mean - meanShare;
        }

        public List<String> toLines() {
            return toLines("Use mode=random for future tournaments.");
        }

        /**
         * @param advice - what to change if repeating seeds is found to lose information
         */
        public List<String> toLines(String advice) {
            List<String> lines = new ArrayList<>();
            lines.add(String.format("---- Seed-clustered error analysis: %d games on %d seeds, %s ----%n", games, seeds,
                    replicates > 0 ? replicates + " game(s) per seating per seed" : "seatings not balanced across seeds"));
            lines.add("SE Normal: all games independent. SE Strat: balanced over seatings, games independent.\n");
            lines.add("SE Clust: the seed is the independent unit (used for the ranking above). rho: implied within-seed correlation.\n");
            for (String w : warnings)
                lines.add("WARNING: " + w + "\n");

            for (int k = 0; k < 2; k++) {
                Map<String, ClusterStats> stats = k == 0 ? agentWin : agentOrdinal;
                lines.add(k == 0 ? "\nAgent win rate:\n" : "\nAgent mean ordinal:\n");
                List<String[]> rows = new ArrayList<>();
                rows.add(new String[]{"Agent", "Mean", "SE Normal", "SE Strat", "SE Clust", "Clu/Strat", "rho", "g"});
                for (Map.Entry<String, ClusterStats> e : stats.entrySet()) {
                    ClusterStats s = e.getValue();
                    rows.add(new String[]{e.getKey(), String.format("%.3f", s.mean), String.format("%.4f", s.seNormal),
                            String.format("%.4f", s.seStrat), String.format("%.4f", s.seClu),
                            String.format("%.2f", s.cluOverStrat()), String.format("%.3f", s.rhoBar),
                            String.valueOf(s.cells)});
                }
                lines.addAll(AnalysisFormat.tabTable(rows));
            }

            if (REPORT_PAIRS && !pairs.isEmpty()) {
                lines.add("\nHead-to-head pairwise score of A against B (1 = A finishes above B, 0.5 = same rank, 0 = below):\n");
                List<String[]> rows = new ArrayList<>();
                rows.add(new String[]{"A", "B", "Mean", "SE Normal", "SE Strat", "SE Clust", "Clu/Strat", "rho", "g"});
                for (Map.Entry<Pair<String, String>, ClusterStats> e : pairs.entrySet()) {
                    ClusterStats s = e.getValue();
                    rows.add(new String[]{e.getKey().a, e.getKey().b, String.format("%.3f", s.mean),
                            String.format("%.4f", s.seNormal), String.format("%.4f", s.seStrat),
                            String.format("%.4f", s.seClu), String.format("%.2f", s.cluOverStrat()),
                            String.format("%.3f", s.rhoBar), String.valueOf(s.cells)});
                }
                lines.addAll(AnalysisFormat.tabTable(rows));
            }
            if (REPORT_PAIRS && !pairs2P.isEmpty()) {
                lines.add("\nHead-to-head position effect (2P): A's win rate in position 0 (p0) and position 1 (p1); Delta = p0 - p1.\n");
                lines.add("Paired SE uses the seed pairing; Paired/Normal is about sqrt(1 - rho).\n");
                List<String[]> rows = new ArrayList<>();
                rows.add(new String[]{"A", "B", "p0", "p1", "rho", "Delta", "SE Normal", "SE Paired"});
                for (Map.Entry<Pair<String, String>, TwoPlayerPair> e : pairs2P.entrySet()) {
                    TwoPlayerPair t = e.getValue();
                    rows.add(new String[]{e.getKey().a, e.getKey().b, String.format("%.3f", t.p0),
                            String.format("%.3f", t.p1), String.format("%.3f", t.rho), String.format("%.3f", t.delta),
                            String.format("%.4f", t.deltaSeNormal), String.format("%.4f", t.deltaSePaired)});
                }
                lines.addAll(AnalysisFormat.tabTable(rows));
            }

            lines.add("\nPosition effects (share of points by player position; delta = share - mean share over positions):\n");
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Position", "Share", "Delta", "SE Normal", "SE Clust", "Clu/Normal", "rho"});
            for (int p = 0; p < positions.size(); p++) {
                ClusterStats s = positions.get(p);
                rows.add(new String[]{String.valueOf(p), String.format("%.3f", s.mean),
                        String.format("%.3f", positionDelta(p)), String.format("%.4f", s.seNormal),
                        String.format("%.4f", s.seClu),
                        String.format("%.2f", s.seNormal > 0 ? s.seClu / s.seNormal : Double.NaN),
                        String.format("%.3f", s.rhoBar)});
            }
            lines.addAll(AnalysisFormat.tabTable(rows));

            lines.add(String.format("%nGame rho (pooled within-seed correlation over all head-to-head pairs): %.3f%n", gameRho));
            if (!Double.isNaN(medianCluOverNormal)) {
                lines.add(String.format("The games played are worth %.2f x as many games with independent seeds and random seating (as in mode=random).%n",
                        1.0 / (medianCluOverNormal * medianCluOverNormal)));
                if (recommendRandom)
                    lines.add("RECOMMENDATION: Repeating seeds loses information for this game (games on the same seed are positively correlated). " +
                            advice + "\n");
                else
                    lines.add("Repeating seeds gains information for this game; the current seed design is a good choice.\n");
            }
            return lines;
        }
    }

    record Obs(long seed, String cell, double y) {
    }

    @Override
    public LinkedHashMap<String, Pair<Double, Double>> getRanking(TournamentResults results) {
        // ranking and means are the same as the standard analysis; only the standard error changes
        LinkedHashMap<String, Pair<Double, Double>> ranking = ordinal ?
                new OrdinalAnalysis().getRanking(results) : new WinRateAnalysis().getRanking(results);
        for (Map.Entry<String, Pair<Double, Double>> entry : ranking.entrySet()) {
            ClusterStats stats = agentStats(results.getGameRecords(), entry.getKey(), ordinal);
            if (usesClustered(stats, results, entry.getKey()))
                entry.setValue(new Pair<>(entry.getValue().a, stats.seClu));
        }
        return ranking;
    }

    /**
     * Seeds when getRanking reports the clustered SE for this agent, otherwise games (the Normal SE).
     */
    @Override
    public int sampleSize(TournamentResults results, String agent) {
        ClusterStats stats = agentStats(results.getGameRecords(), agent, ordinal);
        return usesClustered(stats, results, agent) ? stats.seeds : results.getPlayerResults(agent).size();
    }

    private static boolean usesClustered(ClusterStats stats, TournamentResults results, String agent) {
        return stats.valid() && stats.nObs == results.getPlayerResults(agent).size();
    }

    /**
     * Full analysis of agent, head-to-head and position statistics.
     */
    public static Report analyse(TournamentResults results) {
        List<TournamentResults.GameRecord> records = results.getGameRecords();
        Report report = new Report();
        // games each agent played in, so that pairs only scan the games of one agent
        Map<String, List<TournamentResults.GameRecord>> byAgent = new TreeMap<>();
        for (TournamentResults.GameRecord r : records)
            for (String agent : new HashSet<>(Arrays.asList(r.agents)))
                byAgent.computeIfAbsent(agent, k -> new ArrayList<>()).add(r);
        List<String> agents = new ArrayList<>(byAgent.keySet());
        report.games = records.size();
        report.seeds = (int) records.stream().mapToLong(r -> r.seed).distinct().count();

        for (String agent : agents) {
            report.agentWin.put(agent, agentStats(byAgent.get(agent), agent, false));
            report.agentOrdinal.put(agent, agentStats(byAgent.get(agent), agent, true));
        }
        boolean allBalanced = report.agentWin.values().stream().allMatch(s -> s.balanced);
        report.replicates = allBalanced ? report.agentWin.values().stream().mapToInt(s -> s.replicates).max().orElse(0) : 0;

        double rhoNum = 0, rhoDen = 0;
        for (int i = 0; i < agents.size(); i++) {
            for (int j = i + 1; j < agents.size(); j++) {
                Pair<String, String> key = new Pair<>(agents.get(i), agents.get(j));
                List<TournamentResults.GameRecord> games = byAgent.get(key.a).size() <= byAgent.get(key.b).size() ?
                        byAgent.get(key.a) : byAgent.get(key.b);
                ClusterStats s = pairStats(games, key.a, key.b);
                if (s.nObs == 0) continue;
                report.pairs.put(key, s);
                if (s.balanced && s.rhoDen > 0) {
                    rhoNum += s.rhoNum;
                    rhoDen += s.rhoDen;
                }
                TwoPlayerPair t = twoPlayerPair(games, key.a, key.b);
                if (t != null) report.pairs2P.put(key, t);
            }
        }
        report.gameRho = rhoDen > 0 ? rhoNum / rhoDen : Double.NaN;

        int nPositions = records.isEmpty() ? 0 : records.get(0).agents.length;
        for (int p = 0; p < nPositions; p++) {
            final int position = p;
            report.positions.add(compute(records.stream()
                    .filter(r -> r.agents.length > position)
                    .map(r -> new Obs(r.seed, r.seating(), r.points[position])).toList()));
        }

        List<String> invalid = report.agentWin.entrySet().stream().filter(e -> !e.getValue().valid())
                .map(Map.Entry::getKey).toList();
        if (!invalid.isEmpty())
            report.warnings.add("Fewer than 2 seeds for " + invalid + "; the Normal SE is used for these agents.");
        if (!allBalanced)
            report.warnings.add("Seatings are not balanced across seeds, so SE Strat and rho are not available. " +
                    "SE Clust is still valid.");
        if (records.stream().anyMatch(r -> r.agents.length != nPositions))
            report.warnings.add("Games have different numbers of players; position effects are not meaningful.");

        List<ClusterStats> validWin = report.agentWin.values().stream().filter(ClusterStats::valid).toList();
        report.medianCluOverStrat = median(validWin.stream().mapToDouble(ClusterStats::cluOverStrat));
        report.medianCluOverNormal = median(validWin.stream().mapToDouble(ClusterStats::cluOverNormal));
        // mode=random has independent seeds but does not balance seats, so its SE is close to the Normal SE
        report.recommendRandom = report.medianCluOverNormal > 1.0;
        return report;
    }

    private static double median(DoubleStream values) {
        double[] sorted = values.filter(d -> !Double.isNaN(d)).sorted().toArray();
        if (sorted.length == 0) return Double.NaN;
        return sorted.length % 2 == 1 ? sorted[sorted.length / 2] :
                (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2.0;
    }

    /**
     * One observation per player position the agent occupies in each game.
     * The cell is the seating plus that position, so self-play seatings give one cell per position.
     */
    static ClusterStats agentStats(List<TournamentResults.GameRecord> records, String agent, boolean ordinal) {
        List<Obs> obs = new ArrayList<>();
        for (TournamentResults.GameRecord r : records) {
            for (int p = 0; p < r.agents.length; p++) {
                if (r.agents[p].equals(agent))
                    obs.add(new Obs(r.seed, r.seating() + "#" + p, ordinal ? r.ordinals[p] : r.points[p]));
            }
        }
        return compute(obs);
    }

    /**
     * Pairwise score of a against b in every game that contains both.
     */
    static ClusterStats pairStats(List<TournamentResults.GameRecord> records, String a, String b) {
        List<Obs> obs = new ArrayList<>();
        for (TournamentResults.GameRecord r : records) {
            for (int i = 0; i < r.agents.length; i++) {
                if (!r.agents[i].equals(a)) continue;
                for (int j = 0; j < r.agents.length; j++) {
                    if (r.agents[j].equals(b))
                        obs.add(new Obs(r.seed, r.seating() + "#" + i + "#" + j, pairwiseScore(r.ordinals[i], r.ordinals[j])));
                }
            }
        }
        return compute(obs);
    }

    private static double pairwiseScore(int ordinalA, int ordinalB) {
        return ordinalA < ordinalB ? 1.0 : (ordinalA == ordinalB ? 0.5 : 0.0);
    }

    /**
     * Only defined when all games between a and b are two-player, and each seed has both seatings.
     */
    static TwoPlayerPair twoPlayerPair(List<TournamentResults.GameRecord> records, String a, String b) {
        Map<Long, List<Double>> x0 = new HashMap<>(), x1 = new HashMap<>();
        for (TournamentResults.GameRecord r : records) {
            List<String> names = Arrays.asList(r.agents);
            if (!names.contains(a) || !names.contains(b)) continue;
            if (r.agents.length != 2) return null;
            double y = pairwiseScore(r.ordinals[names.indexOf(a)], r.ordinals[names.indexOf(b)]);
            (r.agents[0].equals(a) ? x0 : x1).computeIfAbsent(r.seed, k -> new ArrayList<>()).add(y);
        }
        if (x0.size() < 2 || !x0.keySet().equals(x1.keySet())) return null;
        List<Long> seeds = new ArrayList<>(x0.keySet());
        int games0 = x0.values().stream().mapToInt(List::size).sum();
        int games1 = x1.values().stream().mapToInt(List::size).sum();
        double[] m0 = seeds.stream().mapToDouble(s -> mean(x0.get(s))).toArray();
        double[] m1 = seeds.stream().mapToDouble(s -> mean(x1.get(s))).toArray();
        double[] diff = new double[seeds.size()];
        for (int i = 0; i < diff.length; i++) diff[i] = m0[i] - m1[i];
        double p0 = mean(m0), p1 = mean(m1);
        double seNormal = Math.sqrt(p0 * (1 - p0) / games0 + p1 * (1 - p1) / games1);
        double sePaired = Math.sqrt(sampleVar(diff) / seeds.size());
        return new TwoPlayerPair(p0, p1, correlation(m0, m1), seNormal, sePaired);
    }

    static ClusterStats compute(List<Obs> obs) {
        int nObs = obs.size();
        if (nObs == 0)
            return new ClusterStats(Double.NaN, 0, 0, 0, 0, false, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        double sum = 0, sumSq = 0;
        Map<String, Map<Long, List<Double>>> byCell = new HashMap<>();
        Map<Long, double[]> bySeed = new HashMap<>(); // {total, count} of y on each seed
        for (Obs o : obs) {
            sum += o.y;
            sumSq += o.y * o.y;
            byCell.computeIfAbsent(o.cell, k -> new HashMap<>()).computeIfAbsent(o.seed, k -> new ArrayList<>()).add(o.y);
            double[] seedTotal = bySeed.computeIfAbsent(o.seed, k -> new double[2]);
            seedTotal[0] += o.y;
            seedTotal[1]++;
        }
        double mean = sum / nObs;
        // the same formula as WinRateAnalysis / OrdinalAnalysis
        double seNormal = Math.sqrt(Math.max(0, sumSq / nObs - mean * mean) / nObs);
        int n = bySeed.size();
        int g = byCell.size();
        int r = byCell.values().iterator().next().values().iterator().next().size();
        boolean balanced = byCell.values().stream().allMatch(m -> m.size() == n &&
                m.values().stream().allMatch(l -> l.size() == r));

        // Cluster-robust SE of the overall mean, with seeds as clusters: sqrt(n / (n-1) * sum_s (T_s - mean * k_s)^2) / N
        // for seed totals T_s over k_s games. This needs no balance; when balanced it equals sd(m_s) / sqrt(n).
        double seClu = Double.NaN;
        if (n >= 2) {
            double ss = 0;
            for (double[] seedTotal : bySeed.values())
                ss += Math.pow(seedTotal[0] - mean * seedTotal[1], 2);
            seClu = Math.sqrt(n / (n - 1.0) * ss) / nObs;
        }
        if (!balanced || n < 2)
            return new ClusterStats(mean, nObs, n, g, r, balanced, seNormal, Double.NaN, seClu, Double.NaN, Double.NaN);

        // s_c^2: variance of the results in cell c over all its n * r games
        double sumV = 0, sumS = 0;
        for (Map<Long, List<Double>> cell : byCell.values()) {
            double v = sampleVar(cell.values().stream().flatMap(List::stream).mapToDouble(d -> d).toArray());
            sumV += v;
            sumS += Math.sqrt(v);
        }
        double varM = n * seClu * seClu; // variance over seeds of m_s, the mean over cells on seed s
        double seStrat = Math.sqrt(sumV / r) / (g * Math.sqrt(n));
        double rhoNum, rhoDen;
        if (r == 1) {
            rhoNum = g * g * varM - sumV;
            rhoDen = sumS * sumS - sumV;
        } else {
            // replicates within a cell on the same seed: use the design effect over all g * r games on a seed
            rhoNum = seClu * seClu - seStrat * seStrat;
            rhoDen = (g * r - 1) * seStrat * seStrat;
        }
        return new ClusterStats(mean, nObs, n, g, r, true, seNormal, seStrat, seClu, rhoNum, rhoDen);
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(d -> d).average().orElse(Double.NaN);
    }

    private static double mean(double[] values) {
        return Arrays.stream(values).average().orElse(Double.NaN);
    }

    private static double sampleVar(double[] values) {
        if (values.length < 2) return 0.0;
        double m = mean(values);
        return Arrays.stream(values).map(v -> (v - m) * (v - m)).sum() / (values.length - 1);
    }

    private static double correlation(double[] x, double[] y) {
        double mx = mean(x), my = mean(y), sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < x.length; i++) {
            sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
            syy += (y[i] - my) * (y[i] - my);
        }
        return sxx > 0 && syy > 0 ? sxy / Math.sqrt(sxx * syy) : Double.NaN;
    }
}

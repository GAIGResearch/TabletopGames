package evaluation.tournaments;

import core.AbstractPlayer;
import evaluation.RunArg;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.*;

import static org.junit.Assert.*;

public class SeedClusteredAnalysisTest {

    private final Map<String, AbstractPlayer> players = new HashMap<>();

    private void addGame(TournamentResults tr, long seed, String[] agents, int[] ordinals) {
        double[] points = new double[agents.length];
        long winners = Arrays.stream(ordinals).filter(o -> o == 1).count();
        for (int p = 0; p < agents.length; p++) {
            points[p] = ordinals[p] == 1 ? 1.0 / winners : 0.0;
            AbstractPlayer player = players.computeIfAbsent(agents[p], name -> {
                RandomPlayer rp = new RandomPlayer();
                rp.setName(name);
                return rp;
            });
            tr.addResult(player, points[p], ordinals[p], 0.0, ordinals[p] == 1 ? 1 : 0);
        }
        tr.gameRecords.add(new TournamentResults.GameRecord(seed, agents, points, ordinals));
    }

    /**
     * Two agents, both seatings on each seed. seatWins decides whether the seed decides the winning seat
     * (true) or the winning agent (false). A wins on even seeds in the agent case, seat 0 wins on even seeds otherwise.
     */
    private TournamentResults twoPlayer(int nSeeds, int replicates, boolean seatWins) {
        TournamentResults tr = new TournamentResults();
        for (long s = 0; s < nSeeds; s++) {
            boolean even = s % 2 == 0;
            for (int rep = 0; rep < replicates; rep++) {
                // A in seat 0
                boolean aWins = even;
                addGame(tr, s, new String[]{"A", "B"}, aWins ? new int[]{1, 2} : new int[]{2, 1});
                // A in seat 1
                aWins = seatWins != even;
                addGame(tr, s, new String[]{"B", "A"}, aWins ? new int[]{2, 1} : new int[]{1, 2});
            }
        }
        return tr;
    }

    @Test
    public void seedDecidingTheSeatGivesNegativeRho() {
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(twoPlayer(10, 1, true));
        SeedClusteredAnalysis.ClusterStats a = report.agentWin.get("A");
        assertEquals(0.5, a.mean, 1e-9);
        assertEquals(2, a.cells);
        assertEquals(1, a.replicates);
        assertEquals(0.0, a.seClu, 1e-9);
        assertEquals(-1.0, a.rhoBar, 1e-9);
        // s_0^2 = s_1^2 = 10/36, SE_strat = 0.5 * sqrt(2 * s^2 / n)
        assertEquals(0.5 * Math.sqrt(2 * (10.0 / 36.0) / 10), a.seStrat, 1e-9);
        assertEquals(-1.0, report.gameRho, 1e-9);
        assertFalse(report.recommendRandom);

        SeedClusteredAnalysis.TwoPlayerPair pair = report.pairs2P.get(new Pair<>("A", "B"));
        assertEquals(-1.0, pair.rho, 1e-9);
        assertEquals(0.0, pair.delta, 1e-9);
        // D_s = +/- 1 on alternate seeds
        assertEquals(Math.sqrt(10.0 / 9.0 / 10), pair.deltaSePaired, 1e-9);
        assertTrue(pair.deltaSePaired > pair.deltaSeNormal);

        // position 0 wins on every even seed in both seatings, so the two seatings are perfectly correlated
        SeedClusteredAnalysis.ClusterStats seat0 = report.positions.get(0);
        assertEquals(0.5, seat0.mean, 1e-9);
        assertEquals(0.0, report.positionDelta(0), 1e-9);
        assertEquals(1.0, seat0.rhoBar, 1e-9);
        assertEquals(Math.sqrt(10.0 / 36.0 / 10), seat0.seClu, 1e-9);
    }

    @Test
    public void seedDecidingTheAgentGivesPositiveRhoAndRecommendsRandom() {
        TournamentResults tr = twoPlayer(10, 1, false);
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(tr);
        SeedClusteredAnalysis.ClusterStats a = report.agentWin.get("A");
        assertEquals(1.0, a.rhoBar, 1e-9);
        assertEquals(Math.sqrt(2.0), a.cluOverStrat(), 1e-9);
        assertEquals(1.0, report.gameRho, 1e-9);
        assertTrue(report.recommendRandom);
        assertEquals(1.0, report.pairs2P.get(new Pair<>("A", "B")).rho, 1e-9);

        // the ranking keeps the means but uses the clustered SE
        LinkedHashMap<String, Pair<Double, Double>> ranking = new SeedClusteredAnalysis(false).getRanking(tr);
        assertEquals(0.5, ranking.get("A").a, 1e-9);
        assertEquals(Math.sqrt(10.0 / 36.0 / 10), ranking.get("A").b, 1e-9);
        assertTrue(String.join("", report.toLines()).contains("RECOMMENDATION"));
    }

    @Test
    public void strongSeatEffectWithWeakPositiveRhoKeepsExhaustive() {
        // A wins from seat 0 except on seeds 0-3, and from seat 1 on seeds 3-22: p0 = 0.9, p1 = 0.5, cov = 1/39
        TournamentResults tr = new TournamentResults();
        for (long s = 0; s < 40; s++) {
            boolean aWins0 = s > 3;
            boolean aWins1 = s >= 3 && s <= 22;
            addGame(tr, s, new String[]{"A", "B"}, aWins0 ? new int[]{1, 2} : new int[]{2, 1});
            addGame(tr, s, new String[]{"B", "A"}, aWins1 ? new int[]{2, 1} : new int[]{1, 2});
        }
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(tr);
        SeedClusteredAnalysis.ClusterStats a = report.agentWin.get("A");
        assertEquals(0.05, a.seClu, 1e-9);
        assertEquals(Math.sqrt(0.21 / 80), a.seNormal, 1e-9);
        // clustering loses information relative to stratification, but stratification gains more than that
        assertTrue(a.seStrat < a.seClu && a.seClu < a.seNormal);
        assertFalse(report.recommendRandom);
    }

    @Test
    public void replicatesPerSeedUseTheDesignEffect() {
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(twoPlayer(100, 2, false));
        SeedClusteredAnalysis.ClusterStats a = report.agentWin.get("A");
        assertEquals(2, a.replicates);
        assertEquals(2, a.cells);
        assertTrue(a.valid());
        // all four games on a seed have the same result for A
        assertEquals(1.0, a.rhoBar, 0.02);
        assertEquals(2.0, a.cluOverStrat(), 0.02);
    }

    @Test
    public void unbalancedGamesStillGetClusteredSE() {
        TournamentResults tr = twoPlayer(10, 1, false);
        addGame(tr, 0, new String[]{"A", "B"}, new int[]{1, 2});
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(tr);
        SeedClusteredAnalysis.ClusterStats a = report.agentWin.get("A");
        assertFalse(a.balanced);
        assertTrue(a.valid());
        assertTrue(Double.isNaN(a.seStrat));
        assertTrue(Double.isNaN(a.rhoBar));
        assertEquals(0, report.replicates);
        assertFalse(report.warnings.isEmpty());

        // A's results: seed 0 has 3 wins, other even seeds 2 wins, odd seeds 0 wins; 21 games, mean 11/21
        double mean = 11.0 / 21.0;
        double ss = Math.pow(3 - 3 * mean, 2) + 4 * Math.pow(2 - 2 * mean, 2) + 5 * Math.pow(0 - 2 * mean, 2);
        double expected = Math.sqrt(10.0 / 9.0 * ss) / 21.0;
        assertEquals(expected, a.seClu, 1e-12);
        assertEquals(expected, new SeedClusteredAnalysis(false).getRanking(tr).get("A").b, 1e-12);
    }

    @Test
    public void fewerThanTwoSeedsFallsBackToNormalSE() {
        TournamentResults tr = twoPlayer(1, 3, false);
        assertFalse(SeedClusteredAnalysis.analyse(tr).agentWin.get("A").valid());
        assertEquals(new WinRateAnalysis().getRanking(tr).get("A").b,
                new SeedClusteredAnalysis(false).getRanking(tr).get("A").b, 1e-12);
    }

    @Test
    public void selfPlaySeatingsGiveOneCellPerPosition() {
        TournamentResults tr = new TournamentResults();
        for (long s = 0; s < 5; s++) {
            addGame(tr, s, new String[]{"A", "A"}, new int[]{1, 2});
            addGame(tr, s, new String[]{"A", "B"}, new int[]{1, 2});
            addGame(tr, s, new String[]{"B", "A"}, new int[]{2, 1});
            addGame(tr, s, new String[]{"B", "B"}, new int[]{2, 1});
        }
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(tr);
        assertEquals(4, report.agentWin.get("A").cells);
        assertTrue(report.agentWin.get("A").valid());
        assertEquals(2, report.pairs.get(new Pair<>("A", "B")).cells);
    }

    private RoundRobinTournament tournament(String mode, int nPlayers, int matchups, GameType gameType) {
        return tournament(mode, nPlayers, matchups, gameType, 3, 0);
    }

    private RoundRobinTournament tournament(String mode, int nPlayers, int matchups, GameType gameType,
                                            int nAgents, int distinctSeeds) {
        List<AbstractPlayer> agents = new ArrayList<>();
        for (int i = 0; i < nAgents; i++) {
            RandomPlayer rp = new RandomPlayer(new Random(i));
            rp.setName("R" + i);
            agents.add(rp);
        }
        String[] args = new String[]{"mode=" + mode, "matchups=" + matchups, "seed=1234", "listener=\"\"",
                "distinctRandomSeeds=" + distinctSeeds};
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament rrt = new RoundRobinTournament(agents, gameType, nPlayers, null, config);
        rrt.setResultsFile("");
        rrt.run();
        return rrt;
    }

    @Test
    public void exhaustiveTournamentReportsClusteredSE() {
        RoundRobinTournament rrt = tournament("exhaustive", 2, 60, GameType.TicTacToe);
        TournamentResults tr = rrt.getTournamentResults();
        assertEquals(60, tr.getGameRecords().size());
        assertEquals(10, tr.getGameRecords().stream().mapToLong(r -> r.seed).distinct().count());

        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(tr);
        assertEquals(10, report.seeds);
        assertEquals(1, report.replicates);
        for (int i = 0; i < 3; i++) {
            SeedClusteredAnalysis.ClusterStats s = report.agentWin.get("R" + i);
            assertTrue(s.valid());
            assertEquals(4, s.cells); // 2 (K - 1)
            assertEquals(s.seClu, rrt.getWinStdErr(i), 1e-12);
            assertEquals(report.agentOrdinal.get("R" + i).seClu, rrt.getOrdinalStdErr(i), 1e-12);
        }
        assertEquals(3, report.pairs2P.size());
        assertEquals(2, report.positions.size());
        // confidence intervals for the clustered SE have one observation per seed
        assertEquals(10, new SeedClusteredAnalysis(false).sampleSize(tr, "R0"));
        assertEquals(40, new WinRateAnalysis().sampleSize(tr, "R0"));
    }

    @Test
    public void threePlayerExhaustiveCellCounts() {
        RoundRobinTournament rrt = tournament("exhaustive", 3, 60, GameType.DotsAndBoxes);
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(rrt.getTournamentResults());
        assertEquals(10, report.seeds);
        assertEquals(6, report.agentWin.get("R0").cells);           // 3 (K - 1)(K - 2)
        assertEquals(6, report.pairs.get(new Pair<>("R0", "R1")).cells); // 6 (K - 2)
        assertTrue(report.pairs2P.isEmpty());
        assertEquals(3, report.positions.size());
    }

    @Test
    public void oneVsAllWithTwoAgentsIsBalanced() {
        RoundRobinTournament rrt = tournament("onevsall", 2, 40, GameType.TicTacToe, 2, 0);
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(rrt.getTournamentResults());
        assertEquals(20, report.seeds);
        assertEquals(1, report.replicates);
        assertTrue(report.agentWin.get("R0").balanced);
        assertEquals(report.agentWin.get("R0").seClu, rrt.getWinStdErr(0), 1e-12);
    }

    @Test
    public void oneVsAllWithManyAgentsUsesUnbalancedClusteredSE() {
        RoundRobinTournament rrt = tournament("onevsall", 2, 60, GameType.TicTacToe, 3, 0);
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(rrt.getTournamentResults());
        // seed m is used for game m with the focus agent in each position
        assertEquals(30, report.seeds);
        SeedClusteredAnalysis.ClusterStats focus = report.agentWin.get("R0");
        assertEquals(60, focus.nObs);
        assertTrue(focus.valid());
        assertEquals(focus.seClu, rrt.getWinStdErr(0), 1e-12);
    }

    @Test
    public void fixedModeWithDistinctSeedsClustersReplicates() {
        RoundRobinTournament rrt = tournament("fixed", 2, 20, GameType.TicTacToe, 2, 3);
        SeedClusteredAnalysis.Report report = SeedClusteredAnalysis.analyse(rrt.getTournamentResults());
        assertEquals(3, report.seeds);
        assertEquals(20, report.replicates);
        SeedClusteredAnalysis.ClusterStats s = report.agentWin.get("R0");
        assertEquals(1, s.cells);
        assertEquals(s.seClu, rrt.getWinStdErr(0), 1e-12);
    }

    @Test
    public void randomTournamentKeepsNormalSE() {
        RoundRobinTournament rrt = tournament("random", 2, 60, GameType.TicTacToe);
        LinkedHashMap<String, Pair<Double, Double>> normal = new WinRateAnalysis().getRanking(rrt.getTournamentResults());
        for (int i = 0; i < 3; i++)
            assertEquals(normal.get("R" + i).b, rrt.getWinStdErr(i), 1e-12);
    }
}

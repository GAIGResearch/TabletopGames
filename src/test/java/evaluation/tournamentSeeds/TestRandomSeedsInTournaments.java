package evaluation.tournamentSeeds;

import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.RunGames;
import evaluation.tournaments.RoundRobinTournament;
import evaluation.tournaments.SkillGrid;
import games.GameType;
import org.json.simple.JSONObject;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class TestRandomSeedsInTournaments {

    public static void main(String[] args) {

        // We run each type of tournament (and then manually check the files for seeds)

        RunGames.main(new String[]{"config=Tournament_Random_Base.json"});
        // Should create 10 rows, each with a different seed
        RunGames.main(new String[]{"config=Tournament_Exhaustive_Base.json"});
        // Should create 10 * 6 = 60 rows, with each set of 6 rows having the same seed
        RunGames.main(new String[]{"config=Tournament_Seq_Base.json"});
        // Three directories, one for each pair of agents
        // 10 games in each, with 10 different seeds (seeds the same across each pair of agents)
        RunGames.main(new String[]{"config=Tournament_Focus_Base.json"});
        // 30 games, with 10 different seeds...in each set the Focus player is p1, p1, p2 respectively

        // the ones below should then replicate all of the above, but with 3 seeds
        RunGames.main(new String[]{"config=Tournament_Random_ThreeSeeds.json"});
        // 30 games, each set of 10 with a different seed
        RunGames.main(new String[]{"config=Tournament_Exhaustive_ThreeSeeds.json"});
        // 3  * 60 games, with each set of 60 having the same seed
        RunGames.main(new String[]{"config=Tournament_Seq_ThreeSeeds.json"});
        // 3 directories, one for each pair of agents
        // 3 * 10 games in each, with 3 different seeds (seeds the same across each pair of agents)
        RunGames.main(new String[]{"config=Tournament_Focus_ThreeSeeds.json"});
        // 3 * 30 games, with 1 seed per set of 30...in each set the Focus player is p1, p1, p2 respectively
    }


    SeedListener seedListener = new SeedListener();

    @Test
    public void testRandomSeeds() {
        List<AbstractPlayer> randomPlayer = Collections.singletonList(new RandomPlayer());
        String[] args = new String[] {
          "mode=random", "matchups=10", "distinctRandomSeeds=0", "seed=35830953", "listener=\"\""
        };
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(randomPlayer, GameType.DotsAndBoxes, 4, null, config);
        // we now check that each game used a different random seed
        tournament.addListener(seedListener);
        tournament.run();
        assertEquals(10, seedListener.seeds.size());
        Set<Long> uniqueSeeds = new HashSet<>(seedListener.seeds);
        assertEquals(10, uniqueSeeds.size());
    }

    @Test
    public void testRandomFixedSeeds() {
        List<AbstractPlayer> randomPlayer = Collections.singletonList(new RandomPlayer());
        String[] args = new String[] {
                "mode=random", "matchups=10", "distinctRandomSeeds=3", "seed=35830953", "listener=\"\""
        };
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(randomPlayer, GameType.DotsAndBoxes, 4, null, config);
        // we now check that each game used a different random seed
        tournament.addListener(seedListener);
        tournament.run();
        assertEquals(30, seedListener.seeds.size());
        Set<Long> uniqueSeeds = new HashSet<>(seedListener.seeds);
        assertEquals(3, uniqueSeeds.size());
    }

    @Test
    public void testFixedSingleSeed() {
        // i.e. in Fixed mode
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        players.get(0).setName("p1");
        players.get(1).setName("p2");
        String[] args = new String[] {
                "mode=fixed", "matchups=10", "distinctRandomSeeds=1", "seed=35830953", "listener=\"\""
        };
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(players, GameType.DotsAndBoxes, 2, null, config);
        // we now check that each game used the same random seed
        tournament.addListener(seedListener);
        tournament.run();
        assertEquals(10, seedListener.seeds.size());
        Set<Long> uniqueSeeds = new HashSet<>(seedListener.seeds);
        assertEquals(1, uniqueSeeds.size());

        // then check that all games had p1 as the first player
        int p1Count = seedListener.firstPlayerNames.stream().mapToInt(s -> s.equals("p1") ? 1 : 0).sum();
        assertEquals(10, p1Count);
    }

    @Test
    public void testExhaustiveManySeeds() {
        List<AbstractPlayer> randomPlayer = List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer());
        String[] args = new String[] {
                "mode=exhaustive", "matchups=24", "distinctRandomSeeds=0", "seed=35830953", "listener=\"\""
        };
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(randomPlayer, GameType.DotsAndBoxes, 4, null, config);
        // we now check that each game used the same seed in exhaustive mode
        tournament.addListener(seedListener);
        tournament.run();
        assertEquals(24, seedListener.seeds.size());
        Set<Long> uniqueSeeds = new HashSet<>(seedListener.seeds);
        assertEquals(1, uniqueSeeds.size());
    }

    @Test
    public void testExhaustiveSPManySeeds() {
        List<AbstractPlayer> randomPlayer = List.of(new RandomPlayer(), new RandomPlayer());
        String[] args = new String[] {
                "mode=exhaustiveSP", "matchups=24", "distinctRandomSeeds=0", "seed=35830953", "listener=\"\""
        };
        // 2 x 2 x 2 x 2 = 16 possible matchups
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(randomPlayer, GameType.DotsAndBoxes, 4, null, config);
        // we now check that each game used the same seed in exhaustive mode
        tournament.addListener(seedListener);
        tournament.run();
        assertEquals(16, seedListener.seeds.size());
        Set<Long> uniqueSeeds = new HashSet<>(seedListener.seeds);
        assertEquals(1, uniqueSeeds.size());
    }


    @Test
    public void testExhaustiveThreeSeeds() {
        List<AbstractPlayer> randomPlayer = List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer());
        String[] args = new String[] {
                "mode=exhaustive", "matchups=24", "distinctRandomSeeds=3", "seed=35830953", "listener=\"\""
        };
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(randomPlayer, GameType.DotsAndBoxes, 4, null, config);
        // we now check that each set of games was rerun for each seed
        tournament.addListener(seedListener);
        tournament.run();
        assertEquals(3 * 24, seedListener.seeds.size());
        Set<Long> uniqueSeeds = new HashSet<>(seedListener.seeds);
        assertEquals(3, uniqueSeeds.size());
    }

    @Test
    public void testExhaustiveSPThreeSeeds() {
        List<AbstractPlayer> randomPlayer = List.of(new RandomPlayer(), new RandomPlayer());
        String[] args = new String[] {
                "mode=exhaustiveSP", "matchups=24", "distinctRandomSeeds=3", "seed=35830953", "listener=\"\""
        };
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(randomPlayer, GameType.DotsAndBoxes, 4, null, config);
        // we now check that each set of games was rerun for each seed
        tournament.addListener(seedListener);
        tournament.run();
        assertEquals(3 * 16, seedListener.seeds.size());
        Set<Long> uniqueSeeds = new HashSet<>(seedListener.seeds);
        assertEquals(3, uniqueSeeds.size());
    }

    @Test
    public void testOneVsAllRandomSeeds() {
        List<AbstractPlayer> randomPlayer = List.of(new RandomPlayer(), new RandomPlayer());
        String[] args = new String[] {
                "mode=onevsall", "focusPlayer=random", "matchups=10", "distinctRandomSeeds=0", "seed=35830953", "listener=\"\""
        };
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(randomPlayer, GameType.DotsAndBoxes, 4, null, config);
        // we now check that each set of 4 games uses a different random seed (as 4 players)
        tournament.addListener(seedListener);
        tournament.run();
        assertEquals(8, seedListener.seeds.size());
        Set<Long> uniqueSeeds = new HashSet<>(seedListener.seeds);
        assertEquals(2, uniqueSeeds.size());
    }

    @Test
    public void testOneVsAllFourSeedsSeeds() {
        List<AbstractPlayer> randomPlayer = List.of(new RandomPlayer(), new RandomPlayer());
        String[] args = new String[] {
                "mode=onevsall", "matchups=10", "distinctRandomSeeds=4", "seed=35830953", "listener=\"\""
        };
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(randomPlayer, GameType.DotsAndBoxes, 4, null, config);
        // we now check that we just use 4 seeds
        tournament.addListener(seedListener);
        tournament.run();
        assertEquals(32, seedListener.seeds.size());
        Set<Long> uniqueSeeds = new HashSet<>(seedListener.seeds);
        assertEquals(4, uniqueSeeds.size());
    }

}

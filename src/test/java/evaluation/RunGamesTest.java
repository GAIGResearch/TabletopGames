package evaluation;

import core.AbstractPlayer;
import core.Game;
import evaluation.tournaments.AbstractTournament;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import org.junit.*;
import players.basicMCTS.BasicMCTSPlayer;
import players.mcts.MCTSPlayer;
import players.simple.RandomPlayer;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;
import static players.PlayerConstants.BUDGET_TIME;

public class RunGamesTest {


    RoundRobinTournament tournament;
    Map<RunArg, Object> config;
    List<AbstractPlayer> agents;

    @Before
    public void setup() {
        // Before each test we should clean up the directory

        agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());

        config = RunArg.parseConfig(new String[]{}, Collections.singletonList(RunArg.Usage.RunGames));  // empty config
        config.put(RunArg.matchups, 100);
        config.put(RunArg.verbose, false);  // no verbose output
        config.put(RunArg.destDir, "src/test/java/evaluation");
        config.put(RunArg.listener, new ArrayList<String>());  // no listeners
    }

    @After
    public void teardown() {
        // After each test we should clean up the directory
        File dir = new File("src/test/java/evaluation");
        for (File file : dir.listFiles())
            if (!file.isDirectory() && file.getName().endsWith(".txt"))
                file.delete();

    }

    @Test
    public void exhaustiveMode() {
        config.put(RunArg.mode, "exhaustive");
        tournament = new RoundRobinTournament(agents, GameType.Poker, 3, null, config);
        tournament.run();

        // We expect 96 games to be run; with each agent playing in 72 of them
        for (int i = 0; i < agents.size(); i++)
            assertEquals(72, tournament.getNGamesPlayed()[i]);
    }

    @Test
    public void exhaustiveSPMode() {
        config.put(RunArg.mode, "exhaustiveSP");
        tournament = new RoundRobinTournament(agents, GameType.Poker, 3, null, config);
        tournament.run();

        // We expect 64 games to be run; with each agent playing in 48 of them (well, less than that, but at 48 positions)
        for (int i = 0; i < agents.size(); i++)
            assertEquals(48, tournament.getNGamesPlayed()[i]);
    }


    @Test
    public void exhaustiveModeError() {
        config.put(RunArg.mode, "exhaustive");
        config.put(RunArg.matchups, 20);  // minimum needed is 24
        try {
            tournament = new RoundRobinTournament(agents, GameType.Poker, 3, null, config);
            fail("Should not have created the tournament");
        } catch (IllegalArgumentException e) {
            // we expect exception to be thrown
        }

    }

    @Test
    public void randomMode() {
        config.put(RunArg.mode, "random");
        tournament = new RoundRobinTournament(agents, GameType.Poker, 3, null, config);
        tournament.run();

        // We expect 100 games to be run; with each agent playing in 75 of them
        for (int i = 0; i < agents.size(); i++)
            assertEquals(75, tournament.getNGamesPlayed()[i]);
    }

    @Test
    public void randomModeWithSP() {
        agents = agents.subList(0, 2);  // only two agents
        config.put(RunArg.mode, "random");
        tournament = new RoundRobinTournament(agents, GameType.Poker, 3, null, config);
        tournament.run();

        // We expect 100 games to be run; with each agent playing150 of the 300 positions
        for (int i = 0; i < agents.size(); i++)
            assertEquals(150, tournament.getNGamesPlayed()[i]);
    }

    @Test
    public void oneVsAll2Agents() {
        agents = agents.subList(0, 2);  // only two agents
        config.put(RunArg.mode, "oneVsAll");
        tournament = new RoundRobinTournament(agents, GameType.Poker, 3, null, config);
        tournament.run();

        // We expect 99 games to be run; with the first agent playing in all of them, and the other in all other positions
        assertEquals(99, tournament.getNGamesPlayed()[0]);
        assertEquals(198, tournament.getNGamesPlayed()[1]);
    }


    @Test
    public void oneVsAll() {
        config.put(RunArg.mode, "oneVsAll");
        tournament = new RoundRobinTournament(agents, GameType.Poker, 3, null, config);
        tournament.run();

        // We expect 99 games to be run; with the first agent playing in all of them, and the others in 66 each (on average)
        assertEquals(99, tournament.getNGamesPlayed()[0]);
        assertEquals(198, tournament.getNGamesPlayed()[1] + tournament.getNGamesPlayed()[2] + tournament.getNGamesPlayed()[3]);
        assertEquals(66, tournament.getNGamesPlayed()[1], 15);
        assertEquals(66, tournament.getNGamesPlayed()[2], 15);
        assertEquals(66, tournament.getNGamesPlayed()[3], 15);
    }

    @Test
    public void playersCopiedCorrectly() {
        config.put(RunArg.mode, "random");
        config.put(RunArg.matchups, 1);
        List<AbstractPlayer> singleAgent = Collections.singletonList(new MCTSPlayer());
        singleAgent.get(0).getParameters().setParameterValue("budgetType", BUDGET_TIME);
        singleAgent.get(0).getParameters().setParameterValue("reuseTree", true);
        singleAgent.get(0).getParameters().setParameterValue("budget", 20);
        tournament = new RoundRobinTournament(singleAgent, GameType.Poker, 3, null, config);
        tournament.createAndRunMatchUp(List.of(0, 0, 0));

        Game game = tournament.getGame();
        assertEquals(3, game.getPlayers().size());
        assertEquals(0, game.getPlayers().get(0).getPlayerID());
        assertEquals(1, game.getPlayers().get(1).getPlayerID());
        assertEquals(2, game.getPlayers().get(2).getPlayerID());
        assertNotSame(singleAgent, game.getPlayers().get(0)); // should be a copy);
        assertNotSame(game.getPlayers().get(0), game.getPlayers().get(1)); // should be a copy
        assertNotSame(game.getPlayers().get(0), game.getPlayers().get(2)); // should be a copy
        assertNotSame(game.getPlayers().get(1), game.getPlayers().get(2)); // should be a copy
    }


    @Test
    public void basicMCTSPlayersCopiedCorrectly() {
        config.put(RunArg.mode, "random");
        config.put(RunArg.matchups, 1);
        List<AbstractPlayer> singleAgent = Collections.singletonList(new BasicMCTSPlayer());
        singleAgent.get(0).getParameters().setParameterValue("budgetType", BUDGET_TIME);
        singleAgent.get(0).getParameters().setParameterValue("budget", 20);
        tournament = new RoundRobinTournament(singleAgent, GameType.Poker, 3, null, config);
        tournament.createAndRunMatchUp(List.of(0, 0, 0));

        Game game = tournament.getGame();
        assertEquals(3, game.getPlayers().size());
        assertEquals(0, game.getPlayers().get(0).getPlayerID());
        assertEquals(1, game.getPlayers().get(1).getPlayerID());
        assertEquals(2, game.getPlayers().get(2).getPlayerID());
        assertNotSame(singleAgent, game.getPlayers().get(0)); // should be a copy);
        assertNotSame(game.getPlayers().get(0), game.getPlayers().get(1)); // should be a copy
        assertNotSame(game.getPlayers().get(0), game.getPlayers().get(2)); // should be a copy
        assertNotSame(game.getPlayers().get(1), game.getPlayers().get(2)); // should be a copy
    }


}

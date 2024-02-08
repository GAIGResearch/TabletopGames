package players.mcts;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;

public class RolloutTerminationTests {


    TestMCTSPlayer mctsPlayer;
    MCTSParams params;
    GameType[] gamesToTest = {
            GameType.LoveLetter,
            GameType.Dominion,
            GameType.Virus,
            GameType.Poker,
     //       GameType.DiceMonastery,
            GameType.Catan,
            GameType.ColtExpress,
            GameType.CantStop,
            GameType.Diamant,
            GameType.SushiGo
    };


    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams();
        params.setRandomSeed(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 10;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 200;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.discardStateAfterEachIteration = false;
        params.K = 1.0;
    }

    public Game createGame(MCTSParams params, GameType gameType) {
        mctsPlayer = new TestMCTSPlayer(params, STNRollout::new);
        mctsPlayer.setDebug(false);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(244)));
        Game game = gameType.createGameInstance(3, 3302345);
        game.reset(players);
        return game;
    }

    @Test
    public void test_MaxN_DEFAULT() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.DEFAULT;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


    @Test
    public void test_MaxN_END_TURN() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_TURN;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


    @Test
    public void test_MaxN_START_TURN() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.START_TURN;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }

    @Test
    public void test_MaxN_END_ROUND() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_ROUND;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


    @Test
    public void test_SelfOnly_DEFAULT() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.DEFAULT;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


    @Test
    public void test_SelfOnly_END_TURN() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_TURN;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


    @Test
    public void test_SelfOnly_START_TURN() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.START_TURN;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }

    @Test
    public void test_SelfOnly_END_ROUND() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_ROUND;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }



}

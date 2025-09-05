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
    //        GameType.Catan,
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
        params.rolloutLength = 5;
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

    public Game createMTGame(MCTSParams params, GameType gameType) {
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
        params.rolloutTermination = MCTSEnums.RolloutTermination.EXACT;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }

    @Test
    public void test_rolloutPerPlayer() {
        params.rolloutLengthPerPlayer = true;
        params.rolloutTermination = MCTSEnums.RolloutTermination.EXACT;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }



    @Test
    public void test_MaxN_END_ACTION() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_ACTION;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


    @Test
    public void test_MaxN_START_ACTION() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.START_ACTION;
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
        params.rolloutTermination = MCTSEnums.RolloutTermination.EXACT;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


    @Test
    public void test_SelfOnly_END_ACTION() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_ACTION;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


    @Test
    public void test_SelfOnly_START_ACTION() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.START_ACTION;
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

    @Test
    public void test_MultiTree_DEFAULT() {
        params.rolloutLength = 20;
        params.rolloutTermination = MCTSEnums.RolloutTermination.EXACT;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createMTGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }

    @Test
    public void test_MultiTree_END_ACTION() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_ACTION;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createMTGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }
    @Test
    public void test_MultiTree_START_TURN() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.START_ACTION;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createMTGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }

    @Test
    public void test_MultiTree_END_ROUND() {
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_ROUND;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createMTGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


    @Test
    public void test_SelfOnly_END_TURN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_TURN;
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
    public void test_MultiTree_END_TURN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        params.rolloutTermination = MCTSEnums.RolloutTermination.END_TURN;
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createMTGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }


}

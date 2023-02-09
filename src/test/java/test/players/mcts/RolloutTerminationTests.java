package test.players.mcts;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.simple.RandomPlayer;

import java.util.*;

public class RolloutTerminationTests {


    TestMCTSPlayer mctsPlayer;
    MCTSParams params;
    GameType[] gamesToTest = {GameType.LoveLetter, GameType.Dominion, GameType.Virus, GameType.Poker};


    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 10;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 100;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.discardStateAfterEachIteration = false;
        params.K = 1.0;
    }

    public Game createGame(MCTSParams params, GameType gameType) {
        mctsPlayer = new TestMCTSPlayer(params, STNRollout::new);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(244)));
        Game game = gameType.createGameInstance(3, 3302345);
        game.reset(players);
        return game;
    }

    @Test
    public void testDEFAULT() {
        for (GameType gt : gamesToTest) {
            if (gt == GameType.GameTemplate) continue;
            Game game = createGame(params, gt);
            System.out.println("Running " + gt.name());
            game.run();
        }
    }

}

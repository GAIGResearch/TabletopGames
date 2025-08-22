package games.fmtester;

import evaluation.ForwardModelTester;
import games.catan.CatanParameters;
import games.descent2e.DescentParameters;
import org.junit.Test;

import java.util.List;

public class ForwardModelTestsWithMCTS {

    @Test
    public void testSaboteur() {
        new ForwardModelTester("game=Saboteur", "nGames=1", "nPlayers=3", "agent=json\\players\\gameSpecific\\Saboteur.json");
    }

    @Test
    public void testRoot() {
        ForwardModelTester fmt = new ForwardModelTester("game=Root", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testBackgammon() {
        ForwardModelTester fmt = new ForwardModelTester("game=Backgammon", "nGames=2", "nPlayers=2", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testBattleLore() {
        new ForwardModelTester("game=Battlelore", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Battlelore.json");
    }

    @Test
    public void testDescent2e() {
        DescentParameters params = new DescentParameters();
        params.heroesToBePlayed = List.of("Widow Tarha", "Avric Albright");
        ForwardModelTester fmt = new ForwardModelTester(params, "game=Descent2e", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Descent.json");
        params.heroesToBePlayed = List.of("Leoric of the Book", "Ashrian");
        fmt = new ForwardModelTester(params, "game=Descent2e", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Descent.json");
        params.heroesToBePlayed = List.of("Syndrael", "Jain Fairwood");
        fmt = new ForwardModelTester(params, "game=Descent2e", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Descent.json");
        params.heroesToBePlayed = List.of("Tomble Burrowell", "Grisban the Thirsty");
        fmt = new ForwardModelTester(params, "game=Descent2e", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Descent.json");
    }

    @Test
    public void testCantStop() {
        new ForwardModelTester("game=CantStop", "nGames=2", "nPlayers=3", "agent=json\\players\\gameSpecific\\CantStop.json");
    }

    @Test
    public void testCatan() {
        new ForwardModelTester("game=Catan", "nGames=1", "nPlayers=3", "agent=json\\players\\gameSpecific\\catan\\Catan_LearnedHeuristic.json");
    }

    @Test
    public void testCatanNoTrading() {
        CatanParameters cp = new CatanParameters();
        cp.setParameterValue("tradingAllowed", false);
        new ForwardModelTester(cp, "game=Catan", "nGames=1", "nPlayers=3", "agent=json\\players\\gameSpecific\\catan\\Catan_LearnedHeuristic.json");
    }

    @Test
    public void testColtExpress() {
        new ForwardModelTester("game=ColtExpress", "nGames=2", "nPlayers=3", "agent=json\\players\\gameSpecific\\ColtExpress_3P.json");
    }

    @Test
    public void testConnect4() {
        new ForwardModelTester("game=Connect4", "nGames=2", "nPlayers=2", "agent=json\\players\\gameSpecific\\Connect4.json");
    }

    @Test
    public void testDiamant() {
        new ForwardModelTester("game=Diamant", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Diamant.json");
    }

    @Test
    public void testDominion() {
        new ForwardModelTester("game=Dominion", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Dominion.json");
    }

    @Test
    public void testDotsAndBoxes() {
        new ForwardModelTester("game=DotsAndBoxes", "nGames=2", "nPlayers=2", "agent=json\\players\\gameSpecific\\DotsAndBoxes.json");
    }

    @Test
    public void testExplodingKittens() {
        new ForwardModelTester("game=ExplodingKittens", "nGames=2", "nPlayers=3", "agent=json\\players\\gameSpecific\\ExplodingKittens.json");
    }

    @Test
    public void testLoveLetter() {
        new ForwardModelTester("game=LoveLetter", "nGames=2", "nPlayers=3", "agent=json\\players\\gameSpecific\\LoveLetter_4P.json");
    }

    @Test
    public void testPoker() {
        new ForwardModelTester("game=Poker", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Poker_3+P.json");
    }

    @Test
    public void testStratego() {
        new ForwardModelTester("game=Stratego", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Stratego.json");
    }

    @Test
    public void testSushiGo() {
        new ForwardModelTester("game=SushiGo", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\SushiGo.json");
    }

    @Test
    public void testTicTacToe() {
        new ForwardModelTester("game=TicTacToe", "nGames=2", "nPlayers=2", "agent=json\\players\\gameSpecific\\TicTacToe.json");
    }

    @Test
    public void testUno() {
        new ForwardModelTester("game=Uno", "nGames=1", "nPlayers=5", "agent=json\\players\\gameSpecific\\Virus.json");
    }

    @Test
    public void testResistance() {
        new ForwardModelTester("game=Resistance", "nGames=2", "nPlayers=5", "agent=json\\players\\gameSpecific\\Dominion.json");
    }

    @Test
    public void testVirus() {
        new ForwardModelTester("game=Virus", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Virus.json");
    }

    @Test
    public void testSevenWonders() {
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=3", "agent=json\\players\\gameSpecific\\SushiGo.json");
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=4", "agent=json\\players\\gameSpecific\\SushiGo.json");
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=5", "agent=json\\players\\gameSpecific\\SushiGo.json");
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=6", "agent=json\\players\\gameSpecific\\SushiGo.json");
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=7", "agent=json\\players\\gameSpecific\\SushiGo.json");
    }

    @Test
    public void testHearts() {
        new ForwardModelTester("game=Hearts", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Poker_3+P.json");
    }

    @Test
    public void testMastermind() {
        new ForwardModelTester("game=Mastermind", "nGames=2", "nPlayers=1", "agent=json\\players\\gameSpecific\\TicTacToe.json");
    }

    @Test
    public void testMonopolyDeal() {
        new ForwardModelTester("game=MonopolyDeal", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\TicTacToe.json");
    }


    @Test
    public void testChess() {
        new ForwardModelTester("game=Chess", "nGames=2", "nPlayers=2");
    }

    @Test
    public void testChineseCheckers() {
        ForwardModelTester fmt = new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\ChineseCheckers.json");
        fmt = new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=3", "agent=json\\players\\gameSpecific\\ChineseCheckers.json");
        fmt = new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=4", "agent=json\\players\\gameSpecific\\ChineseCheckers.json");
        fmt = new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=6", "agent=json\\players\\gameSpecific\\ChineseCheckers.json");
    }

}

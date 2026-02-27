package games.fmtester;

import evaluation.ForwardModelTester;
import org.junit.Test;

public class ForwardModelTestsWithRandom {


    @Test
    public void testRoot() {
        new ForwardModelTester("game=Root", "nGames=1", "nPlayers=2");
        new ForwardModelTester("game=Root", "nGames=1", "nPlayers=4");
    }

    @Test
    public void testPickomino() {
        new ForwardModelTester("game=Pickomino", "nGames=1", "nPlayers=2");
        new ForwardModelTester("game=Pickomino", "nGames=1", "nPlayers=3");
        new ForwardModelTester("game=Pickomino", "nGames=1", "nPlayers=4");
        new ForwardModelTester("game=Pickomino", "nGames=1", "nPlayers=5");
    }

    @Test
    public void testMonopolyDeal() {
        new ForwardModelTester("game=MonopolyDeal", "nGames=2", "nPlayers=2");
    }

    @Test
    public void testBattleLore() {
        new ForwardModelTester("game=Battlelore", "nGames=2", "nPlayers=2");
    }
    @Test
    public void testCantStop() {
        new ForwardModelTester("game=CantStop", "nGames=2", "nPlayers=3");
    }

    @Test
    public void testColtExpress() {
        new ForwardModelTester("game=ColtExpress", "nGames=1", "nPlayers=3");
    }
    @Test
    public void testConnect4() {
        new ForwardModelTester("game=Connect4", "nGames=2", "nPlayers=2");
    }
    @Test
    public void testDiamant() {
       new ForwardModelTester("game=Diamant", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testDominion() {
        new ForwardModelTester("game=Dominion", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testDotsAndBoxes() {
        new ForwardModelTester("game=DotsAndBoxes", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testExplodingKittens() {
        new ForwardModelTester("game=ExplodingKittens", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testLoveLetter() {
        new ForwardModelTester("game=LoveLetter", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testPoker() {
        new ForwardModelTester("game=Poker", "nGames=2", "nPlayers=3");
        new ForwardModelTester("game=Poker", "nGames=2", "nPlayers=6");
    }

    @Test
    public void testStratego() {
        new ForwardModelTester("game=Stratego", "nGames=1", "nPlayers=2");
    }
    @Test
    public void testSushiGo() {
        new ForwardModelTester("game=SushiGo", "nGames=2", "nPlayers=3");
    }

    @Test
    public void testTicTacToe() {
        new ForwardModelTester("game=TicTacToe", "nGames=2", "nPlayers=2");
    }
    @Test
    public void testUno() {
        new ForwardModelTester("game=Uno", "nGames=2", "nPlayers=5");
    }

    @Test
    public void testResistance() {
        new ForwardModelTester("game=Resistance", "nGames=2", "nPlayers=5");
    }
    @Test
    public void testVirus() {
        new ForwardModelTester("game=Virus", "nGames=2", "nPlayers=3");
    }

    @Test
    public void testSevenWonders() {
        new ForwardModelTester("game=Wonders7", "nGames=2", "nPlayers=3");
        new ForwardModelTester("game=Wonders7", "nGames=2", "nPlayers=4");
        new ForwardModelTester("game=Wonders7", "nGames=2", "nPlayers=5");
        new ForwardModelTester("game=Wonders7", "nGames=2", "nPlayers=6");
        new ForwardModelTester("game=Wonders7", "nGames=2", "nPlayers=7");
    }

    @Test
    public void testHearts() {
        new ForwardModelTester("game=Hearts", "nGames=2", "nPlayers=4");
    }

    @Test
    public void testSeaSaltPaper() {
        ForwardModelTester fmt = new ForwardModelTester("game=SeaSaltPaper", "nGames=10", "nPlayers=4");
    }

    @Test
    public void testMastermind() {
        new ForwardModelTester("game=Mastermind", "nGames=2", "nPlayers=1");
    }

    @Test
    public void testChess() {
        new ForwardModelTester("game=Chess", "nGames=2", "nPlayers=2");
    }

    @Test
    public void testChineseCheckers() {
        new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=2");
        new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=3");
        new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=4");
        new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=6");
    }
    @Test
    public void testPowerGrid() {
        // Adjust game name, players, seed, etc. to your setup
        new ForwardModelTester("game=PowerGrid","nGames=1", "nPlayers=3");
        new ForwardModelTester("game=PowerGrid","nGames=1", "nPlayers=4");
        new ForwardModelTester("game=PowerGrid","nGames=1", "nPlayers=5");
    }

    @Test
    public void testGoFish() {
        ForwardModelTester fmt = new ForwardModelTester("game=GoFish", "nGames=1", "nPlayers=2");
        fmt = new ForwardModelTester("game=GoFish", "nGames=1", "nPlayers=3");
        fmt = new ForwardModelTester("game=GoFish", "nGames=1", "nPlayers=4");
        fmt = new ForwardModelTester("game=GoFish", "nGames=1", "nPlayers=5");
    }
}

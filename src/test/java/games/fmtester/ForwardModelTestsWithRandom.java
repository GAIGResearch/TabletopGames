package games.fmtester;

import evaluation.ForwardModelTester;
import org.junit.Test;

public class ForwardModelTestsWithRandom {


    @Test
    public void testRoot() {
        ForwardModelTester fmt = new ForwardModelTester("game=Root", "nGames=1", "nPlayers=2");
    }

    @Test
    public void testSaboteur() {
        ForwardModelTester fmt = new ForwardModelTester("game=Saboteur", "nGames=1", "nPlayers=3");
    }

    @Test
    public void testMonopolyDeal() {
        ForwardModelTester fmt = new ForwardModelTester("game=MonopolyDeal", "nGames=5", "nPlayers=2");
    }

    // Descent commented out because it takes a looong time, and the MCTS FM tests give us the required confidence
//    @Test
//    public void testDescent2e() {
//        ForwardModelTester fmt = new ForwardModelTester("game=Descent2e", "nGames=5", "nPlayers=2");
//    }
    @Test
    public void testBattleLore() {
        ForwardModelTester fmt = new ForwardModelTester("game=Battlelore", "nGames=2", "nPlayers=2");
    }
    @Test
    public void testCantStop() {
        ForwardModelTester fmt = new ForwardModelTester("game=CantStop", "nGames=5", "nPlayers=3");
    }
    @Test
    public void testCatan() {
        ForwardModelTester fmt = new ForwardModelTester("game=Catan", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testColtExpress() {
        ForwardModelTester fmt = new ForwardModelTester("game=ColtExpress", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testConnect4() {
        ForwardModelTester fmt = new ForwardModelTester("game=Connect4", "nGames=2", "nPlayers=2");
    }
    @Test
    public void testDiamant() {
        ForwardModelTester fmt = new ForwardModelTester("game=Diamant", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testDominion() {
        ForwardModelTester fmt = new ForwardModelTester("game=Dominion", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testDotsAndBoxes() {
        ForwardModelTester fmt = new ForwardModelTester("game=DotsAndBoxes", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testExplodingKittens() {
        ForwardModelTester fmt = new ForwardModelTester("game=ExplodingKittens", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testLoveLetter() {
        ForwardModelTester fmt = new ForwardModelTester("game=LoveLetter", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testPoker() {
        ForwardModelTester fmt = new ForwardModelTester("game=Poker", "nGames=2", "nPlayers=3");
    }
    @Test
    public void testStratego() {
        ForwardModelTester fmt = new ForwardModelTester("game=Stratego", "nGames=2", "nPlayers=2");
    }
    @Test
    public void testSushiGo() {
        ForwardModelTester fmt = new ForwardModelTester("game=SushiGo", "nGames=2", "nPlayers=3");
    }

    @Test
    public void testTicTacToe() {
        ForwardModelTester fmt = new ForwardModelTester("game=TicTacToe", "nGames=2", "nPlayers=2");
    }
    @Test
    public void testUno() {
        ForwardModelTester fmt = new ForwardModelTester("game=Uno", "nGames=2", "nPlayers=5");
    }
    @Test
    public void testResistance() {
        ForwardModelTester fmt = new ForwardModelTester("game=Resistance", "nGames=2", "nPlayers=5");
    }
    @Test
    public void testVirus() {
        ForwardModelTester fmt = new ForwardModelTester("game=Virus", "nGames=2", "nPlayers=3");
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
        ForwardModelTester fmt = new ForwardModelTester("game=Hearts", "nGames=2", "nPlayers=4");
    }

    @Test
    public void testSeaSaltPaper() {
        ForwardModelTester fmt = new ForwardModelTester("game=SeaSaltPaper", "nGames=10", "nPlayers=4");
    }

    @Test
    public void testMastermind() {
        ForwardModelTester fmt = new ForwardModelTester("game=Mastermind", "nGames=10", "nPlayers=1");
    }
}

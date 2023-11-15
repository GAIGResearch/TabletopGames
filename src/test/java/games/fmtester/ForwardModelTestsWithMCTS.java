package games.fmtester;

import evaluation.ForwardModelTester;
import org.junit.Test;

public class ForwardModelTestsWithMCTS {


    @Test
    public void testBattleLore() {
        new ForwardModelTester("game=Battlelore", "nGames=1", "nPlayers=2", "agentToPlay=json\\players\\gameSpecific\\Battlelore.json");
    }
    @Test
    public void testCantStop() {
         new ForwardModelTester("game=CantStop", "nGames=1", "nPlayers=3", "agentToPlay=json\\players\\gameSpecific\\CantStop.json");
    }
    @Test
    public void testCatan() {
        new ForwardModelTester("game=Catan", "nGames=1", "nPlayers=3", "agentToPlay=json\\players\\gameSpecific\\Catan.json");
    }
    @Test
    public void testColtExpress() {
         new ForwardModelTester("game=ColtExpress", "nGames=1", "nPlayers=3", "agentToPlay=json\\players\\gameSpecific\\ColtExpress_3P.json");
    }
    @Test
    public void testConnect4() {
         new ForwardModelTester("game=Connect4", "nGames=1", "nPlayers=2", "agentToPlay=json\\players\\gameSpecific\\Connect4.json");
    }
    @Test
    public void testDiamant() {
        new ForwardModelTester("game=Diamant", "nGames=1", "nPlayers=4", "agentToPlay=json\\players\\gameSpecific\\Diamant.json");
    }
    @Test
    public void testDominion() {
       new ForwardModelTester("game=Dominion", "nGames=1", "nPlayers=4", "agentToPlay=json\\players\\gameSpecific\\Dominion.json");
    }

    @Test
    public void testDotsAndBoxes() {
         new ForwardModelTester("game=DotsAndBoxes", "nGames=1", "nPlayers=2", "agentToPlay=json\\players\\gameSpecific\\DotsAndBoxes.json");
    }
    @Test
    public void testExplodingKittens() {
         new ForwardModelTester("game=ExplodingKittens", "nGames=1", "nPlayers=3", "agentToPlay=json\\players\\gameSpecific\\ExplodingKittens.json");
    }
    @Test
    public void testLoveLetter() {
        new ForwardModelTester("game=LoveLetter", "nGames=1", "nPlayers=3", "agentToPlay=json\\players\\gameSpecific\\LoveLetter.json");
    }

    @Test
    public void testPoker() {
        new ForwardModelTester("game=Poker", "nGames=1", "nPlayers=4", "agentToPlay=json\\players\\gameSpecific\\Poker.json");
    }
    @Test
    public void testStratego() {
       new ForwardModelTester("game=Stratego", "nGames=1", "nPlayers=2", "agentToPlay=json\\players\\gameSpecific\\Stratego.json");
    }
    @Test
    public void testSushiGo() {
        new ForwardModelTester("game=SushiGo", "nGames=1", "nPlayers=4", "agentToPlay=json\\players\\gameSpecific\\SushiGo.json");
    }

    @Test
    public void testTicTacToe() {
        new ForwardModelTester("game=TicTacToe", "nGames=1", "nPlayers=2", "agentToPlay=json\\players\\gameSpecific\\TicTacToe.json");
    }
    @Test
    public void testUno() {
       new ForwardModelTester("game=Uno", "nGames=1", "nPlayers=5", "agentToPlay=json\\players\\gameSpecific\\Virus.json");
    }
    @Test
    public void testResistance() {
      new ForwardModelTester("game=Resistance", "nGames=1", "nPlayers=5", "agentToPlay=json\\players\\gameSpecific\\Dominion.json");
    }
    @Test
    public void testVirus() {
       new ForwardModelTester("game=Virus", "nGames=1", "nPlayers=4", "agentToPlay=json\\players\\gameSpecific\\Virus.json");
    }

    @Test
    public void testSevenWonders() {
       new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=4", "agentToPlay=json\\players\\gameSpecific\\SushiGo.json");
    }

    @Test
    public void testHearts() {
        new ForwardModelTester("game=Hearts", "nGames=1", "nPlayers=4", "agentToPlay=json\\players\\gameSpecific\\Poker.json");
    }
}

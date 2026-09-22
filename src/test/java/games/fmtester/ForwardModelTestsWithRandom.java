package games.fmtester;

import evaluation.ForwardModelTester;
import games.agram.AgramParameters;
import games.cuckoo.CuckooParameters;
import games.euchre.EuchreParameters;
import games.gofish.GoFishParameters;
import games.whist.WhistParameters;
import games.blackjack.BlackjackParameters;
import games.crazyeights.CZEParameters;
import games.cribbage.CribbageParameters;
import org.junit.Test;

public class ForwardModelTestsWithRandom {


    @Test
    public void testRoot() {
        new ForwardModelTester("game=Root", "nGames=1", "nPlayers=2");
        new ForwardModelTester("game=Root", "nGames=1", "nPlayers=4");
    }

    @Test
    public void testSpades() {
        new ForwardModelTester("game=Spades", "nGames=2", "nPlayers=4");
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
        for (int nPlayers = 2; nPlayers <= 6; nPlayers++)
            new ForwardModelTester("game=GoFish", "nGames=1", "nPlayers=" + nPlayers);
        // the old play-on rules, without the extra turns
        GoFishParameters params = new GoFishParameters();
        params.setParameterValue("playUntilAllBooks", true);
        params.setParameterValue("continueOnSuccess", false);
        params.setParameterValue("continueOnDrawingSameRank", false);
        new ForwardModelTester(params, "game=GoFish", "nGames=2", "nPlayers=3");
        params = new GoFishParameters();
        params.setParameterValue("playUntilAllBooks", true);
        new ForwardModelTester(params, "game=GoFish", "nGames=2", "nPlayers=5");
    }

    @Test
    public void testCrazyEights() {
        new ForwardModelTester("game=CrazyEights", "nGames=2", "nPlayers=2");
        new ForwardModelTester("game=CrazyEights", "nGames=2", "nPlayers=5");
        new ForwardModelTester("game=CrazyEights", "nGames=2", "nPlayers=8");
        CZEParameters params = new CZEParameters();
        params.setParameterValue("dealerNominatesStarterSuit", true);
        new ForwardModelTester(params, "game=CrazyEights", "nGames=5", "nPlayers=3");
    }

    @Test
    public void testWhist() {
        new ForwardModelTester("game=Whist", "nGames=2", "nPlayers=4");
        WhistParameters params = new WhistParameters();
        params.setParameterValue("nDeals", 3);
        params.setParameterValue("trumpMode", WhistParameters.TrumpMode.ROTATION);
        params.setParameterValue("noTrumpsInRotation", true);
        params.setParameterValue("rememberVoids", false);
        new ForwardModelTester(params, "game=Whist", "nGames=2", "nPlayers=4");
    }

    @Test
    public void testAgram() {
        new ForwardModelTester("game=Agram", "nGames=2", "nPlayers=2");
        new ForwardModelTester("game=Agram", "nGames=2", "nPlayers=3");
        new ForwardModelTester("game=Agram", "nGames=2", "nPlayers=5");
        AgramParameters params = new AgramParameters();
        params.setParameterValue("nDeals", 3);
        new ForwardModelTester(params, "game=Agram", "nGames=2", "nPlayers=4");
    }

    @Test
    public void testEuchre() {
        new ForwardModelTester("game=Euchre", "nGames=2", "nPlayers=4");
        EuchreParameters params = new EuchreParameters();
        params.setParameterValue("targetScore", 10);
        params.setParameterValue("sittingOutDealerPicksUp", false);
        params.setParameterValue("rememberVoids", false);
        new ForwardModelTester(params, "game=Euchre", "nGames=2", "nPlayers=4");
    }

    @Test
    public void testCuckoo() {
        new ForwardModelTester("game=Cuckoo", "nGames=2", "nPlayers=6");
        new ForwardModelTester("game=Cuckoo", "nGames=2", "nPlayers=4");
        CuckooParameters params = new CuckooParameters();
        params.setParameterValue("nLives", 1);
        new ForwardModelTester(params, "game=Cuckoo", "nGames=2", "nPlayers=10");
    }

    @Test
    public void testBlackjack() {
        new ForwardModelTester("game=Blackjack", "nGames=2", "nPlayers=1");
        new ForwardModelTester("game=Blackjack", "nGames=2", "nPlayers=3");
        new ForwardModelTester("game=Blackjack", "nGames=2", "nPlayers=7");
        BlackjackParameters params = new BlackjackParameters();
        params.setParameterValue("nHands", 5);
        params.setParameterValue("doubleDown", true);
        params.setParameterValue("splitting", true);
        params.setParameterValue("dealerHitsSoft17", true);
        params.setParameterValue("payout21NaturalOnly", true);
        params.setParameterValue("payout21", 1.5);
        new ForwardModelTester(params, "game=Blackjack", "nGames=2", "nPlayers=4");
    }

    @Test
    public void testCribbage() {
        new ForwardModelTester("game=Cribbage", "nGames=3", "nPlayers=2");
        CribbageParameters params = new CribbageParameters();
        params.setParameterValue("nRounds", 8);
        params.setParameterValue("targetScore", 61);
        params.setParameterValue("playFifteenPoints", 2);
        params.setParameterValue("runsIncludeStarter", true);
        params.setParameterValue("cribFlushNeedsStarter", true);
        new ForwardModelTester(params, "game=Cribbage", "nGames=3", "nPlayers=2");
    }
}

package games.fmtester;

import evaluation.ForwardModelTester;
import games.agram.AgramParameters;
import games.cuckoo.CuckooParameters;
import games.euchre.EuchreParameters;
import games.gofish.GoFishParameters;
import games.golfsix.GolfSixParameters;
import games.klaverjassen.KlaverjassenParameters;
import games.whist.WhistParameters;
import games.blackjack.BlackjackParameters;
import games.goofspiel.GoofspielParameters;
import games.leducpoker.LeducPokerParameters;
import games.catan.CatanParameters;
import games.crazyeights.CZEParameters;
import games.cribbage.CribbageParameters;
import games.descent2e.DescentParameters;
import games.dominion.DominionIParameters;
import games.dominion.DominionParameters;
import games.dominion.DominionSDParameters;
import org.junit.Test;

import java.util.List;

public class ForwardModelTestsWithMCTS {

    @Test
    public void testSaboteur() {
        new ForwardModelTester("game=Saboteur", "nGames=3", "nPlayers=5", "agent=json\\players\\gameSpecific\\Saboteur\\Saboteur.json", "budget=50");
    }

    @Test
    public void testRoot() {
        new ForwardModelTester("game=Root", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testPickomino() {
        new ForwardModelTester("game=Pickomino", "nGames=1", "nPlayers=2", "agent=json\\players\\mcts.json");
        new ForwardModelTester("game=Pickomino", "nGames=1", "nPlayers=3", "agent=json\\players\\mcts.json");
        new ForwardModelTester("game=Pickomino", "nGames=1", "nPlayers=4", "agent=json\\players\\mcts.json");
        new ForwardModelTester("game=Pickomino", "nGames=1", "nPlayers=5", "agent=json\\players\\mcts.json");
        new ForwardModelTester("game=Pickomino", "nGames=1", "nPlayers=6", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testBackgammon() {
        new ForwardModelTester("game=Backgammon", "nGames=2", "nPlayers=2", "agent=json\\players\\gameSpecific\\Backgammon\\Backgammon.json", "budget=50");
        new ForwardModelTester("game=XIIScripta", "nGames=2", "nPlayers=2", "agent=json\\players\\gameSpecific\\Backgammon\\Backgammon.json", "budget=50");
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
        new ForwardModelTester(params, "game=Descent2e", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Descent.json");
        params.heroesToBePlayed = List.of("Syndrael", "Jain Fairwood");
        new ForwardModelTester(params, "game=Descent2e", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Descent.json");
        params.heroesToBePlayed = List.of("Tomble Burrowell", "Grisban the Thirsty");
        new ForwardModelTester(params, "game=Descent2e", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Descent.json");
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
        new ForwardModelTester("game=ColtExpress", "nGames=2", "nPlayers=3", "agent=json\\players\\gameSpecific\\ColtExpress\\ColtExpress_3P.json");
    }

    @Test
    public void testConnect4() {
        new ForwardModelTester("game=Connect4", "nGames=2", "nPlayers=2", "agent=json\\players\\gameSpecific\\Connect4.json", "budget=50");
    }

    @Test
    public void testDiamant() {
        new ForwardModelTester("game=Diamant", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Diamant.json");
    }


    @Test
    public void testDiamantDUCT() {
        new ForwardModelTester("game=Diamant", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Diamant_DUCT.json");
    }

    @Test
    public void testDominion() {
        new ForwardModelTester("game=Dominion", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Dominion\\Dominion.json");
        new ForwardModelTester(new DominionSDParameters(), "game=Dominion", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Dominion\\Dominion.json");
        new ForwardModelTester(new DominionIParameters(), "game=Dominion", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Dominion\\Dominion.json");
    }

    @Test
    public void testDotsAndBoxes() {
        new ForwardModelTester("game=DotsAndBoxes", "nGames=2", "nPlayers=2", "agent=json\\players\\gameSpecific\\DotsAndBoxes.json");
    }

    @Test
    public void testExplodingKittens() {
        new ForwardModelTester("game=ExplodingKittens", "nGames=2", "nPlayers=3",
                "agent=json\\players\\gameSpecific\\ExplodingKittens\\ExplodingKittens.json");
    }

    @Test
    public void testLoveLetter() {
        new ForwardModelTester("game=LoveLetter", "nGames=2", "nPlayers=3", "agent=json\\players\\gameSpecific\\LoveLetter\\LoveLetter_4P.json", "budget=50");
    }

    @Test
    public void testPoker() {
        new ForwardModelTester("game=Poker", "nGames=3", "nPlayers=4", "agent=json\\players\\gameSpecific\\Poker\\Poker_3+P.json");
    }

    @Test
    public void testStratego() {
        new ForwardModelTester("game=Stratego", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\Stratego.json");
    }

    @Test
    public void testSushiGoWithSeqUCT() {
        new ForwardModelTester("game=SushiGo", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\SushiGo\\SushiGo.json");
    }

    @Test
    public void testSushiGoWithDUCT() {
        new ForwardModelTester("game=SushiGo", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\SushiGo\\SushiGoDUCT.json");
    }

    @Test
    public void testTicTacToe() {
        new ForwardModelTester("game=TicTacToe", "nGames=2", "nPlayers=2", "agent=json\\players\\gameSpecific\\TicTacToe.json");
    }

    @Test
    public void testUno() {
        new ForwardModelTester("game=Uno", "nGames=1", "nPlayers=5", "agent=json\\players\\gameSpecific\\Virus\\Virus.json");
    }

    @Test
    public void testResistance() {
        new ForwardModelTester("game=Resistance", "nGames=2", "nPlayers=5", "agent=json\\players\\gameSpecific\\Dominion\\Dominion.json");
    }

    @Test
    public void testVirus() {
        new ForwardModelTester("game=Virus", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Virus\\Virus.json");
    }

    @Test
    public void testSevenWonders() {
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=3", "agent=json\\players\\gameSpecific\\Wonders7\\Wonders7_3P.json", "budget=50");
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=4", "agent=json\\players\\gameSpecific\\Wonders7\\Wonders7_4-5P.json", "budget=50");
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=5", "agent=json\\players\\gameSpecific\\Wonders7\\Wonders7_4-5P.json", "budget=50");
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=6", "agent=json\\players\\gameSpecific\\Wonders7\\Wonders7_4-5P.json", "budget=50");
        new ForwardModelTester("game=Wonders7", "nGames=1", "nPlayers=7", "agent=json\\players\\gameSpecific\\Wonders7\\Wonders7_4-5P.json", "budget=50");
    }

    @Test
    public void testHearts() {
        new ForwardModelTester("game=Hearts", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Hearts\\Hearts.json", "budget=50");
    }

    @Test
    public void testSpades() {
        new ForwardModelTester("game=Spades", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\Hearts\\Hearts.json", "budget=50");
    }


    @Test
    public void testMastermind() {
        new ForwardModelTester("game=Mastermind", "nGames=2", "nPlayers=1", "agent=json\\players\\gameSpecific\\TicTacToe.json");
    }

    @Test
    public void testSeaSaltPaper() {
        new ForwardModelTester("game=SeaSaltPaper", "nGames=3", "nPlayers=4", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testMonopolyDeal() {
        new ForwardModelTester("game=MonopolyDeal", "nGames=2", "nPlayers=4", "agent=json\\players\\gameSpecific\\MonopolyDeal\\MonopolyDeal_64ms.json");
    }


    @Test
    public void testChess() {
        new ForwardModelTester("game=Chess", "nGames=2", "nPlayers=2", "agent=json\\players\\gameSpecific\\TicTacToe.json");
    }

    @Test
    public void testChineseCheckers() {
        new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=2", "agent=json\\players\\gameSpecific\\ChineseCheckers.json");
        new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=3", "agent=json\\players\\gameSpecific\\ChineseCheckers.json");
        new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=4", "agent=json\\players\\gameSpecific\\ChineseCheckers.json");
        new ForwardModelTester("game=ChineseCheckers", "nGames=1", "nPlayers=6", "agent=json\\players\\gameSpecific\\ChineseCheckers.json");
    }

    @Test
    public void testPenteGrammai() {
        new ForwardModelTester("game=PenteGrammai", "nGames=2", "nPlayers=2", "agent=json\\players\\mcts.json");
    }


    @Test
    public void testPowerGrid() {
        new ForwardModelTester("game=PowerGrid", "nGames=1", "nPlayers=4", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testGoFish() {
        new ForwardModelTester("game=GoFish", "nGames=3", "nPlayers=4", "agent=json\\players\\mcts.json");
        GoFishParameters params = new GoFishParameters();
        params.setParameterValue("playUntilAllBooks", true);
        new ForwardModelTester(params, "game=GoFish", "nGames=1", "nPlayers=4", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testGolfSix() {
        new ForwardModelTester("game=GolfSix", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
        GolfSixParameters params = new GolfSixParameters();
        params.setParameterValue("nDeals", 2);
        params.setParameterValue("finalTurns", true);
        new ForwardModelTester(params, "game=GolfSix", "nGames=1", "nPlayers=3", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testCrazyEights() {
        new ForwardModelTester("game=CrazyEights", "nGames=2", "nPlayers=3", "agent=json\\players\\mcts.json");
        CZEParameters params = new CZEParameters();
        params.setParameterValue("dealerNominatesStarterSuit", true);
        new ForwardModelTester(params, "game=CrazyEights", "nGames=3", "nPlayers=2", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testWhist() {
        new ForwardModelTester("game=Whist", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
        WhistParameters params = new WhistParameters();
        params.setParameterValue("nDeals", 3);
        params.setParameterValue("trumpMode", WhistParameters.TrumpMode.ROTATION);
        params.setParameterValue("noTrumpsInRotation", true);
        params.setParameterValue("rememberVoids", false);
        new ForwardModelTester(params, "game=Whist", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testEuchre() {
        new ForwardModelTester("game=Euchre", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
        EuchreParameters params = new EuchreParameters();
        params.setParameterValue("targetScore", 10);
        params.setParameterValue("sittingOutDealerPicksUp", false);
        params.setParameterValue("rememberVoids", false);
        new ForwardModelTester(params, "game=Euchre", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testCuckoo() {
        new ForwardModelTester("game=Cuckoo", "nGames=2", "nPlayers=6", "agent=json\\players\\mcts.json");
        CuckooParameters params = new CuckooParameters();
        params.setParameterValue("nLives", 1);
        new ForwardModelTester(params, "game=Cuckoo", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testAgram() {
        new ForwardModelTester("game=Agram", "nGames=2", "nPlayers=3", "agent=json\\players\\mcts.json");
        AgramParameters params = new AgramParameters();
        params.setParameterValue("nDeals", 3);
        new ForwardModelTester(params, "game=Agram", "nGames=2", "nPlayers=2", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testBlackjack() {
        new ForwardModelTester("game=Blackjack", "nGames=2", "nPlayers=1", "agent=json\\players\\mcts.json");
        BlackjackParameters params = new BlackjackParameters();
        params.setParameterValue("nHands", 3);
        params.setParameterValue("doubleDown", true);
        params.setParameterValue("splitting", true);
        new ForwardModelTester(params, "game=Blackjack", "nGames=2", "nPlayers=3", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testCribbage() {
        new ForwardModelTester("game=Cribbage", "nGames=2", "nPlayers=2", "agent=json\\players\\mcts.json");
        CribbageParameters params = new CribbageParameters();
        params.setParameterValue("nRounds", 4);
        params.setParameterValue("targetScore", 31);
        params.setParameterValue("runsIncludeStarter", true);
        new ForwardModelTester(params, "game=Cribbage", "nGames=2", "nPlayers=2", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testGoofspiel() {
        new ForwardModelTester("game=Goofspiel", "nGames=2", "nPlayers=2", "agent=json\\players\\mcts.json");
        GoofspielParameters params = new GoofspielParameters();
        params.setParameterValue("tieRule", GoofspielParameters.TieRule.DISCARD);
        params.setParameterValue("cardsPerSuit", 5);
        new ForwardModelTester(params, "game=Goofspiel", "nGames=2", "nPlayers=3", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testKlaverjassen() {
        new ForwardModelTester("game=Klaverjassen", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("nHands", 3);
        params.setParameterValue("partnerTrumpRule", KlaverjassenParameters.PartnerTrumpRule.NO_UNDERTRUMP);
        params.setParameterValue("tieIsFailure", true);
        params.setParameterValue("rememberVoids", false);
        new ForwardModelTester(params, "game=Klaverjassen", "nGames=2", "nPlayers=4", "agent=json\\players\\mcts.json");
    }

    @Test
    public void testLeducPoker() {
        new ForwardModelTester("game=LeducPoker", "nGames=5", "nPlayers=2", "agent=json\\players\\mcts.json");
        LeducPokerParameters params = new LeducPokerParameters();
        params.setParameterValue("nHands", 5);
        params.setParameterValue("highCardUsesBoard", true);
        params.setParameterValue("maxRaisesPerRound", 1);
        new ForwardModelTester(params, "game=LeducPoker", "nGames=2", "nPlayers=2", "agent=json\\players\\mcts.json");
    }
}

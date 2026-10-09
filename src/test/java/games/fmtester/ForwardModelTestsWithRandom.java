package games.fmtester;

import games.GameType;
import core.AbstractParameters;
import evaluation.ForwardModelTester;
import games.agram.AgramParameters;
import games.cuckoo.CuckooParameters;
import games.euchre.EuchreParameters;
import games.gofish.GoFishParameters;
import games.golfsix.GolfSixParameters;
import games.klaverjassen.KlaverjassenParameters;
import games.lawnandorder.LawnAndOrderParameters;
import games.schwimmen.SchwimmenParameters;
import games.whist.WhistParameters;
import games.blackjack.BlackjackParameters;
import games.goofspiel.GoofspielParameters;
import games.leducpoker.LeducPokerParameters;
import games.toads.ToadParameters;
import games.crazyeights.CZEParameters;
import games.cribbage.CribbageParameters;
import games.pitch.PitchParameters;
import games.president.PresidentParameters;
import games.rummy.RummyParameters;
import games.scarto.ScartoParameters;
import games.scopa.ScopaParameters;
import games.skitgubbe.SkitgubbeParameters;
import games.sueca.SuecaParameters;
import org.junit.Test;

public class ForwardModelTestsWithRandom {

    @Test
    public void testSkitgubbe() {
        new ForwardModelTester("game=Skitgubbe", "nGames=2", "nPlayers=3");
        SkitgubbeParameters params = new SkitgubbeParameters();
        params.setParameterValue("completerLeads", true);
        params.setParameterValue("exitOrderTiebreak", true);
        params.setParameterValue("handSize", 2);
        new ForwardModelTester(params, "game=Skitgubbe", "nGames=1", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Skitgubbe, "data/skitgubbe/Skitgubbe_Valet.json"),
                "game=Skitgubbe", "nGames=1", "nPlayers=3");
    }

    @Test
    public void testScopa() {
        new ForwardModelTester("game=Scopa", "nGames=2", "nPlayers=2");
        ScopaParameters params = new ScopaParameters();
        params.setParameterValue("targetScore", 11);
        params.setParameterValue("redealOnKings", true);
        new ForwardModelTester(params, "game=Scopa", "nGames=1", "nPlayers=2");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Scopa, "data/scopa/Scopa_Valet.json"),
                "game=Scopa", "nGames=1", "nPlayers=2");
    }


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
    public void testScarto() {
        new ForwardModelTester("game=Scarto", "nGames=2", "nPlayers=3");
        ScartoParameters params = new ScartoParameters();
        params.setParameterValue("nDeals", 3);
        params.setParameterValue("dealerExchange", true);
        params.setParameterValue("rememberVoids", false);
        new ForwardModelTester(params, "game=Scarto", "nGames=1", "nPlayers=3");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Scarto, "data/scarto/Scarto_Valet.json"),
                "game=Scarto", "nGames=1", "nPlayers=3");
    }

    @Test
    public void testLawnAndOrder() {
        new ForwardModelTester("game=LawnAndOrder", "nGames=2", "nPlayers=3");
        new ForwardModelTester("game=LawnAndOrder", "nGames=1", "nPlayers=2");
        LawnAndOrderParameters params = new LawnAndOrderParameters();
        params.setParameterValue("targetScore", 5);
        params.setParameterValue("maxRounds", 6);
        params.setParameterValue("emergencySessionReveals", 1);
        params.setParameterValue("goodwillBonus", 2);
        new ForwardModelTester(params, "game=LawnAndOrder", "nGames=1", "nPlayers=6");
    }

    @Test
    public void testSchwimmen() {
        new ForwardModelTester("game=Schwimmen", "nGames=2", "nPlayers=5");
        new ForwardModelTester("game=Schwimmen", "nGames=1", "nPlayers=2");
        SchwimmenParameters params = new SchwimmenParameters();
        params.setParameterValue("livesGame", true);
        params.setParameterValue("startingChips", 1);
        params.setParameterValue("maxCircuitsPerDeal", 3);
        new ForwardModelTester(params, "game=Schwimmen", "nGames=1", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Schwimmen, "data/schwimmen/Schwimmen_Valet.json"),
                "game=Schwimmen", "nGames=1", "nPlayers=5");
    }

    @Test
    public void testPitch() {
        new ForwardModelTester("game=Pitch", "nGames=2", "nPlayers=4");
        PitchParameters params = new PitchParameters();
        params.setParameterValue("targetScore", 11);
        params.setParameterValue("countHighLowSeparately", true);
        new ForwardModelTester(params, "game=Pitch", "nGames=2", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Pitch, "data/pitch/Pitch_Valet.json"),
                "game=Pitch", "nGames=1", "nPlayers=4");
    }

    @Test
    public void testSueca() {
        new ForwardModelTester("game=Sueca", "nGames=2", "nPlayers=4");
        SuecaParameters params = new SuecaParameters();
        params.setParameterValue("playRubber", true);
        params.setParameterValue("targetGames", 2);
        params.setParameterValue("rememberVoids", false);
        new ForwardModelTester(params, "game=Sueca", "nGames=2", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Sueca, "data/sueca/Sueca_Valet.json"),
                "game=Sueca", "nGames=1", "nPlayers=4");
    }

    @Test
    public void testPresident() {
        new ForwardModelTester("game=President", "nGames=2", "nPlayers=5");
        PresidentParameters params = new PresidentParameters();
        params.setParameterValue("targetScore", 11);
        params.setParameterValue("exchangeCards", 2);
        new ForwardModelTester(params, "game=President", "nGames=1", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.President, "data/president/President_Valet.json"),
                "game=President", "nGames=1", "nPlayers=5");
    }

    @Test
    public void testRummy() {
        new ForwardModelTester("game=Rummy", "nGames=2", "nPlayers=2");
        RummyParameters params = new RummyParameters();
        params.setParameterValue("targetScore", 100);
        new ForwardModelTester(params, "game=Rummy", "nGames=1", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Rummy, "data/rummy/Rummy_Valet.json"),
                "game=Rummy", "nGames=1", "nPlayers=2");
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
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Hearts, "data/hearts/Hearts_Valet.json"),
                "game=Hearts", "nGames=1", "nPlayers=4");
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
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.GoFish, "data/gofish/GoFish_Valet.json"),
                "game=GoFish", "nGames=1", "nPlayers=4");
    }

    @Test
    public void testGolfSix() {
        for (int nPlayers = 2; nPlayers <= 4; nPlayers++)
            new ForwardModelTester("game=GolfSix", "nGames=2", "nPlayers=" + nPlayers);
        GolfSixParameters params = new GolfSixParameters();
        params.setParameterValue("nDeals", 3);
        params.setParameterValue("finalTurns", true);
        new ForwardModelTester(params, "game=GolfSix", "nGames=2", "nPlayers=3");
        // the safeguard ends deals early
        params = new GolfSixParameters();
        params.setParameterValue("nDeals", 3);
        params.setParameterValue("maxTurnsPerPlayer", 4);
        new ForwardModelTester(params, "game=GolfSix", "nGames=2", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.GolfSix, "data/golfsix/GolfSix_Valet.json"),
                "game=GolfSix", "nGames=1", "nPlayers=4");
    }

    @Test
    public void testCrazyEights() {
        new ForwardModelTester("game=CrazyEights", "nGames=2", "nPlayers=2");
        new ForwardModelTester("game=CrazyEights", "nGames=2", "nPlayers=5");
        new ForwardModelTester("game=CrazyEights", "nGames=2", "nPlayers=8");
        CZEParameters params = new CZEParameters();
        params.setParameterValue("dealerNominatesStarterSuit", true);
        new ForwardModelTester(params, "game=CrazyEights", "nGames=5", "nPlayers=3");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.CrazyEights, "data/crazyeights/CrazyEights_Valet.json"),
                "game=CrazyEights", "nGames=1", "nPlayers=2");
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
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Whist, "data/whist/Whist_Valet.json"),
                "game=Whist", "nGames=1", "nPlayers=4");
    }

    @Test
    public void testAgram() {
        new ForwardModelTester("game=Agram", "nGames=2", "nPlayers=2");
        new ForwardModelTester("game=Agram", "nGames=2", "nPlayers=3");
        new ForwardModelTester("game=Agram", "nGames=2", "nPlayers=5");
        AgramParameters params = new AgramParameters();
        params.setParameterValue("nDeals", 3);
        new ForwardModelTester(params, "game=Agram", "nGames=2", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Agram, "data/agram/Agram_Valet.json"),
                "game=Agram", "nGames=1", "nPlayers=2");
    }

    @Test
    public void testEuchre() {
        new ForwardModelTester("game=Euchre", "nGames=2", "nPlayers=4");
        EuchreParameters params = new EuchreParameters();
        params.setParameterValue("targetScore", 10);
        params.setParameterValue("sittingOutDealerPicksUp", false);
        params.setParameterValue("rememberVoids", false);
        new ForwardModelTester(params, "game=Euchre", "nGames=2", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Euchre, "data/euchre/Euchre_Valet.json"),
                "game=Euchre", "nGames=1", "nPlayers=4");
    }

    @Test
    public void testCuckoo() {
        new ForwardModelTester("game=Cuckoo", "nGames=2", "nPlayers=6");
        new ForwardModelTester("game=Cuckoo", "nGames=2", "nPlayers=4");
        CuckooParameters params = new CuckooParameters();
        params.setParameterValue("nLives", 1);
        new ForwardModelTester(params, "game=Cuckoo", "nGames=2", "nPlayers=10");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Cuckoo, "data/cuckoo/Cuckoo_Valet.json"),
                "game=Cuckoo", "nGames=1", "nPlayers=6");
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
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Blackjack, "data/blackjack/Blackjack_Valet.json"),
                "game=Blackjack", "nGames=1", "nPlayers=1");
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
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Cribbage, "data/cribbage/Cribbage_Valet.json"),
                "game=Cribbage", "nGames=1", "nPlayers=2");
    }

    @Test
    public void testGoofspiel() {
        new ForwardModelTester("game=Goofspiel", "nGames=2", "nPlayers=2");
        new ForwardModelTester("game=Goofspiel", "nGames=2", "nPlayers=7");
        GoofspielParameters params = new GoofspielParameters();
        params.setParameterValue("tieRule", GoofspielParameters.TieRule.HIGHEST_UNIQUE);
        params.setParameterValue("aceHigh", true);
        params.setParameterValue("cardsPerSuit", 7);
        new ForwardModelTester(params, "game=Goofspiel", "nGames=2", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Goofspiel, "data/goofspiel/Goofspiel_Valet.json"),
                "game=Goofspiel", "nGames=1", "nPlayers=2");
    }

    @Test
    public void testKlaverjassen() {
        new ForwardModelTester("game=Klaverjassen", "nGames=2", "nPlayers=4");
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("nHands", 3);
        params.setParameterValue("partnerTrumpRule", KlaverjassenParameters.PartnerTrumpRule.NO_UNDERTRUMP);
        params.setParameterValue("tieIsFailure", true);
        params.setParameterValue("rememberVoids", false);
        new ForwardModelTester(params, "game=Klaverjassen", "nGames=2", "nPlayers=4");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.Klaverjassen, "data/klaverjassen/Klaverjassen_Valet.json"),
                "game=Klaverjassen", "nGames=1", "nPlayers=4");
    }

    @Test
    public void testLeducPoker() {
        new ForwardModelTester("game=LeducPoker", "nGames=5", "nPlayers=2");
        LeducPokerParameters params = new LeducPokerParameters();
        params.setParameterValue("nHands", 10);
        params.setParameterValue("highCardUsesBoard", true);
        params.setParameterValue("maxRaisesPerRound", 3);
        new ForwardModelTester(params, "game=LeducPoker", "nGames=2", "nPlayers=2");
        new ForwardModelTester(AbstractParameters.createFromFile(GameType.LeducPoker, "data/leducpoker/LeducPoker_Valet.json"),
                "game=LeducPoker", "nGames=1", "nPlayers=2");
    }

    @Test
    public void testWarOfTheToads() {
        new ForwardModelTester("game=WarOfTheToads", "nGames=10", "nPlayers=2");
        // the legacy deck and flow
        ToadParameters params = new ToadParameters();
        params.setParameterValue("cardFile", "cards_005.json");
        params.setParameterValue("openingReturn", false);
        params.setParameterValue("discardOption", true);
        params.setParameterValue("secondRoundStart", ToadParameters.SecondRoundStart.WINNER);
        new ForwardModelTester(params, "game=WarOfTheToads", "nGames=10", "nPlayers=2");
    }
}

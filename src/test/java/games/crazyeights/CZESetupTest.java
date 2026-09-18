package games.crazyeights;

import core.components.FrenchCard;
import org.junit.Test;

import static core.components.FrenchCard.Suite.Clubs;
import static games.crazyeights.CZETestUtils.assertAllCardsPresent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CZESetupTest {

    CZEForwardModel fm = new CZEForwardModel();

    @Test
    public void dealsSevenCardsEachToTwoPlayersAndFiveEachOtherwise() {
        for (int nPlayers = 2; nPlayers <= 8; nPlayers++) {
            CZEGameState state = new CZEGameState(new CZEParameters(), nPlayers);
            fm.setup(state);
            int handSize = nPlayers == 2 ? 7 : 5;
            String label = nPlayers + " players";
            for (int p = 0; p < nPlayers; p++)
                assertEquals(label + ", player " + p, handSize, state.getPlayerHands().get(p).getSize());
            // one card is turned up to start the discard pile; the rest form the stock
            assertEquals(label, 1, state.getDiscardPile().getSize());
            assertEquals(label, 52 - nPlayers * handSize - 1, state.getDrawDeck().getSize());
            assertEquals(label, 0, state.getCurrentPlayer());
            assertAllCardsPresent(state);
        }
    }

    @Test
    public void suitToMatchIsTheStarterCardsSuitOrTheParameterSuitIfItIsAnEight() {
        CZEParameters params = new CZEParameters();
        params.starterEightSuit = Clubs;   // not the default, so the parameter is seen to be used
        int eights = 0, others = 0;
        for (int seed = 0; seed < 500; seed++) {
            params.setRandomSeed(seed);
            CZEGameState state = new CZEGameState(params, 3);
            fm.setup(state);
            FrenchCard top = state.getTopCard();
            if (top.type == FrenchCard.FrenchCardType.Number && top.number == 8) {
                assertEquals("seed " + seed, Clubs, state.getCurrentSuit());
                eights++;
            } else {
                assertEquals("seed " + seed, top.suite, state.getCurrentSuit());
                others++;
            }
        }
        assertTrue("no starter Eight in 500 deals", eights > 0);
        assertTrue("every starter was an Eight", others > 0);
    }
}

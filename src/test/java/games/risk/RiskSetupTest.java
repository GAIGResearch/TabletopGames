package games.risk;

import games.risk.components.RiskCard;
import org.junit.Test;

import java.util.*;

import static games.risk.RiskTestUtils.newState;
import static org.junit.Assert.*;

public class RiskSetupTest {

    @Test
    public void territoriesAreDealtAtRandomByDefault() {
        RiskGameState state = newState(3, 11, null);
        for (RiskTerritory t : WorldMap.ALL) {
            assertTrue(t.name(), state.getOwner(t) >= 0);
            assertEquals(t.name(), 1, state.getArmies(t));
        }
        for (int p = 0; p < 3; p++)
            assertEquals(14, state.getNTerritories(p));
        assertEquals(RiskGamePhase.PLACE_INITIAL, state.getGamePhase());
    }

    @Test
    public void withoutTheRandomDealAllTerritoriesStartUnclaimedWithEmptyHandsInClaimPhase() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("randomTerritoryDeal", false);
        RiskGameState state = newState(3, 11, params);
        for (RiskTerritory t : WorldMap.ALL) {
            assertEquals(t.name(), -1, state.getOwner(t));
            assertEquals(t.name(), 0, state.getArmies(t));
        }
        for (int p = 0; p < 3; p++)
            assertEquals(0, state.getHand(p).getSize());
        assertEquals(RiskGamePhase.CLAIM, state.getGamePhase());
        assertEquals(state.getFirstPlayer(), state.getCurrentPlayer());
    }

    @Test
    public void startingArmiesDependOnThePlayerCount() {
        // the armies before any are placed: with the territories claimed, not dealt
        int[] expected = {35, 30, 25, 20}; // 3, 4, 5, 6 players
        for (int n = 3; n <= 6; n++) {
            RiskParameters params = new RiskParameters();
            params.setParameterValue("randomTerritoryDeal", false);
            RiskGameState state = newState(n, 1, params);
            for (int p = 0; p < n; p++)
                assertEquals(n + " players", expected[n - 3], state.getArmiesToPlace(p));
        }
        RiskParameters params = new RiskParameters();
        params.setParameterValue("randomTerritoryDeal", false);
        params.setParameterValue("startArmies4", 22);
        RiskGameState state = newState(4, 1, params);
        for (int p = 0; p < 4; p++)
            assertEquals(22, state.getArmiesToPlace(p));
    }

    @Test
    public void drawDeckHasOneCardPerTerritoryFourteenOfEachSymbolAndTwoWilds() {
        RiskGameState state = newState(3, 1, null);
        List<RiskCard> cards = state.getDrawDeck().getComponents();
        assertEquals(44, cards.size()); // 42 territories + 2 wild
        Map<RiskCard.Symbol, Integer> symbols = new EnumMap<>(RiskCard.Symbol.class);
        Set<RiskTerritory> territories = new HashSet<>();
        for (RiskCard c : cards) {
            symbols.merge(c.symbol, 1, Integer::sum);
            if (c.isWild())
                assertNull(c.territory);
            else {
                assertTrue("duplicate " + c, territories.add(c.territory));
                assertEquals(c.territory.symbol(), c.symbol);
            }
        }
        assertEquals(42, territories.size());
        assertEquals(Integer.valueOf(14), symbols.get(RiskCard.Symbol.INFANTRY));
        assertEquals(Integer.valueOf(14), symbols.get(RiskCard.Symbol.CAVALRY));
        assertEquals(Integer.valueOf(14), symbols.get(RiskCard.Symbol.ARTILLERY));
        assertEquals(Integer.valueOf(2), symbols.get(RiskCard.Symbol.WILD));

        RiskParameters params = new RiskParameters();
        params.setParameterValue("nWildCards", 0);
        assertEquals(42, newState(3, 1, params).getDrawDeck().getSize());
    }

    @Test
    public void drawDeckIsShuffled() {
        // same everything but the seed
        List<RiskCard> a = newState(3, 1, null).getDrawDeck().getComponents();
        List<RiskCard> b = newState(3, 2, null).getDrawDeck().getComponents();
        assertEquals(new HashSet<>(a), new HashSet<>(b));
        assertNotEquals(a, b);
    }

    @Test
    public void firstPlayerIsRandom() {
        Set<Integer> firstPlayers = new HashSet<>();
        for (long seed = 0; seed < 30; seed++)
            firstPlayers.add(newState(4, seed, null).getFirstPlayer());
        assertTrue("first players seen: " + firstPlayers, firstPlayers.size() > 1);
    }

    private static RiskParameters randomDeal() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("randomTerritoryDeal", true);
        return params;
    }

    /** Checks the deal: territories dealt round the table from the first player, the first `extra` getting one more. */
    private static void checkDeal(int nPlayers, int startArmies, int base, int extra) {
        RiskGameState state = newState(nPlayers, 5, randomDeal());
        int first = state.getFirstPlayer();
        for (RiskTerritory t : WorldMap.ALL) {
            assertTrue(t.name() + " unowned", state.getOwner(t) >= 0);
            assertEquals(t.name(), 1, state.getArmies(t));
        }
        for (int i = 0; i < nPlayers; i++) {
            int p = (first + i) % nPlayers;
            int held = i < extra ? base + 1 : base;
            assertEquals(nPlayers + " players, " + i + " from first", held, state.getNTerritories(p));
            assertEquals(startArmies - held, state.getArmiesToPlace(p));
        }
        assertEquals(RiskGamePhase.PLACE_INITIAL, state.getGamePhase());
        assertEquals(first, state.getCurrentPlayer());
    }

    @Test
    public void randomDealWithThreePlayersGivesFourteenEach() {
        checkDeal(3, 35, 14, 0); // 42 = 3 x 14; 35 - 14 = 21 left to place
    }

    @Test
    public void randomDealWithFourPlayersGivesTheFirstTwoElevenAndTheOthersTen() {
        checkDeal(4, 30, 10, 2); // 42 = 11 + 11 + 10 + 10
    }

    @Test
    public void randomDealWithFivePlayersGivesTheFirstTwoNineAndTheOthersEight() {
        checkDeal(5, 25, 8, 2); // 42 = 9 + 9 + 8 + 8 + 8
    }

    @Test
    public void randomDealShufflesTheTerritories() {
        // a fixed deal order would give the first player the same territories whoever they are
        Set<List<RiskTerritory>> firstHoldings = new HashSet<>();
        for (long seed = 1; seed <= 3; seed++) {
            RiskGameState state = newState(3, seed, randomDeal());
            firstHoldings.add(state.getTerritories(state.getFirstPlayer()));
        }
        assertEquals(3, firstHoldings.size());
    }
}

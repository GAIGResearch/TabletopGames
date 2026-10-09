package games.agram;

import org.junit.Test;

import static games.agram.AgramTestUtils.*;
import static org.junit.Assert.*;

public class AgramSetupTest {

    private AgramGameState setUpGame(int nPlayers, long seed) {
        AgramParameters params = new AgramParameters();
        params.setRandomSeed(seed);
        AgramGameState state = new AgramGameState(params, nPlayers);
        new AgramForwardModel().setup(state);
        return state;
    }

    @Test
    public void eachPlayerIsDealtSixOfTheThirtyFiveCardsAndPlayerZeroLeads() {
        for (int nPlayers = 2; nPlayers <= 5; nPlayers++) {
            AgramGameState state = setUpGame(nPlayers, 17);
            for (int p = 0; p < nPlayers; p++)
                assertEquals(nPlayers + " players: hand " + p, 6, state.getPlayerHands().get(p).getSize());
            // 35 cards less 6 per player are left undealt
            assertEquals(35 - 6 * nPlayers, state.getDrawDeck().getSize());
            assertEquals(0, state.getCurrentTrick().getSize());
            assertEquals(0, state.getDiscardPile().getSize());
            assertEquals(0, state.getCurrentPlayer());
            assertEquals(0, state.getCurrentTrick().getLeader());
            // exactly the 35 Agram cards: no J/Q/K/2 and no Ace of Spades
            assertAllCardsPresent(state);
        }
    }

    @Test
    public void sixPlayersNeedMoreCardsThanTheDeckHasAndAreRejected() {
        // 6 players x 6 cards = 36 > 35 (GameType allows at most 5, so build the state directly)
        // the message distinguishes the check from Deck.add(null) failing once the deck runs out
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> setUpGame(6, 1));
        assertTrue(e.getMessage(), e.getMessage().startsWith("Not enough cards"));
    }

    @Test
    public void sixPlayersCanBeDealtFiveCardsEach() {
        // 6 x 5 = 30 <= 35: the check is on the cards needed, not on the player count
        AgramParameters params = new AgramParameters();
        params.setParameterValue("nCardsPerPlayer", 5);
        AgramGameState state = new AgramGameState(params, 6);
        new AgramForwardModel().setup(state);
        for (int p = 0; p < 6; p++)
            assertEquals(5, state.getPlayerHands().get(p).getSize());
        assertEquals(5, state.getDrawDeck().getSize());
    }
}

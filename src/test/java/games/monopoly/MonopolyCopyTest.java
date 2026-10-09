package games.monopoly;

import core.components.Deck;
import games.monopoly.actions.BuyDecision;
import games.monopoly.actions.BuyProperty;
import games.monopoly.components.MonopolyCard;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

public class MonopolyCopyTest {

    MonopolyGameState state;
    MonopolyForwardModel fm;

    @Before
    public void setup() {
        state = newState(3, 7, null);
        fm = new MonopolyForwardModel();
        startTurn(state, 0, sq("GO"));
        give(state, 1, sq("Mayfair"));
        state.setMortgaged(sq("Mayfair"), true);
        state.setCash(2, 1234);
        state.setPosition(2, sq("Bond Street"));
        putInJail(state, 1, 2);
        // player 2 holds the Chance Get Out of Jail Free card
        holdJailCard(state, 2, MonopolyCard.Pile.CHANCE);
    }

    @Test
    public void aCopyEqualsTheOriginalAndChangingItLeavesTheOriginalAlone() {
        MonopolyGameState copy = (MonopolyGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        copy.setCash(0, 1);
        copy.setOwner(sq("Old Kent Road"), 0);
        copy.setMortgaged(sq("Mayfair"), false);
        copy.setPosition(2, sq("GO"));
        copy.getJailCards(2).draw();
        assertNotEquals(state, copy);
        assertEquals(1500, state.getCash(0));
        assertEquals(-1, state.getOwner(sq("Old Kent Road")));
        assertTrue(state.isMortgaged(sq("Mayfair")));
        assertEquals(sq("Bond Street"), state.getPosition(2));
        assertEquals(1, state.getJailCards(2).getSize());
    }

    @Test
    public void aCopyWithABuyDecisionPendingIsIndependent() {
        roll(state, fm, 2, 4);
        // GO + 6 = The Angel Islington, unowned
        MonopolyGameState copy = (MonopolyGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        fm.next(copy, new BuyProperty(sq("The Angel Islington")));
        assertEquals(0, copy.getOwner(sq("The Angel Islington")));
        assertEquals(-1, state.getOwner(sq("The Angel Islington")));
        assertEquals(new BuyDecision(0, sq("The Angel Islington")), state.currentActionInProgress());
    }

    private static Map<MonopolyCard, Integer> multiset(Deck<MonopolyCard> deck) {
        Map<MonopolyCard, Integer> m = new HashMap<>();
        for (MonopolyCard c : deck.getComponents())
            m.merge(c, 1, Integer::sum);
        return m;
    }

    @Test
    public void redeterminisationShufflesThePilesKeepingTheirContentsAndTheJailCards() {
        boolean chanceReordered = false, chestReordered = false;
        for (int i = 0; i < 20; i++) {
            MonopolyGameState copy = (MonopolyGameState) state.copy(0);
            assertEquals(16 - 1, copy.getChanceDeck().getSize());
            assertEquals(16, copy.getCommunityChestDeck().getSize());
            assertEquals(multiset(state.getChanceDeck()), multiset(copy.getChanceDeck()));
            assertEquals(multiset(state.getCommunityChestDeck()), multiset(copy.getCommunityChestDeck()));
            assertEquals(state.getJailCards(2).getComponents(), copy.getJailCards(2).getComponents());
            assertEquals(state.getCash(2), copy.getCash(2));
            assertTrue(copy.isInJail(1));
            // the piles have 15! and 16! orderings, so the chance that all 20 copies keep the original order is
            // negligible
            chanceReordered |= !state.getChanceDeck().getComponents().equals(copy.getChanceDeck().getComponents());
            chestReordered |= !state.getCommunityChestDeck().getComponents()
                    .equals(copy.getCommunityChestDeck().getComponents());
        }
        assertTrue(chanceReordered);
        assertTrue(chestReordered);
    }
}

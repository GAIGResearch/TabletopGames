package games.monopoly;

import games.monopoly.actions.*;
import games.monopoly.components.MonopolyCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.monopoly.MonopolyTestUtils.*;
import static games.monopoly.components.MonopolyCard.Pile.CHANCE;
import static games.monopoly.components.MonopolyCard.Pile.COMMUNITY_CHEST;
import static org.junit.Assert.*;

/**
 * Get Out of Jail Free cards: drawing, keeping and using them, and passing them on at bankruptcy.
 */
public class MonopolyJailCardTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;

    @Before
    public void setup() {
        params = new MonopolyParameters();
        state = newState(3, 7, params);
        fm = new MonopolyForwardModel();
    }

    @Test
    public void drawingGetOutOfJailFreeKeepsTheCardOutOfThePile() {
        startTurn(state, 0, sq("GO"));
        onTop(state, chance("Get out of Jail"));
        roll(state, fm, 3, 4); // Chance 1
        assertEquals(List.of(chance("Get out of Jail")), state.getJailCards(0).getComponents());
        assertEquals(15, state.getChanceDeck().getSize());
        assertFalse(state.getChanceDeck().contains(chance("Get out of Jail")));
        assertEquals(1500, state.getCash(0));
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        assertCardsConserved(state);
    }

    @Test
    public void inJailACardHolderMayUseIt() {
        startTurn(state, 0, sq("Jail"));
        putInJail(state, 0, 0);
        MonopolyCard card = holdJailCard(state, 0, CHANCE);
        assertEquals(Set.of(new PayJailFine(), new UseJailCard(card), new RollDice()), actionSet(fm, state));
    }

    @Test
    public void twoCardsFromDifferentPilesGiveTwoUseJailCardActions() {
        startTurn(state, 0, sq("Jail"));
        putInJail(state, 0, 1);
        MonopolyCard chanceCard = holdJailCard(state, 0, CHANCE);
        MonopolyCard chestCard = holdJailCard(state, 0, COMMUNITY_CHEST);
        // without the fine in cash, the cards and the roll remain
        state.setCash(0, 50 - 1);
        assertEquals(Set.of(new UseJailCard(chanceCard), new UseJailCard(chestCard), new RollDice()),
                actionSet(fm, state));
    }

    @Test
    public void outOfJailTheCardIsNotOffered() {
        startTurn(state, 0, sq("GO"));
        holdJailCard(state, 0, CHANCE);
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
    }

    @Test
    public void usingTheCardFreesThePlayerAndReturnsItToTheBottomOfItsOwnPile() {
        startTurn(state, 0, sq("Jail"));
        putInJail(state, 0, 2);
        MonopolyCard chanceCard = holdJailCard(state, 0, CHANCE);
        MonopolyCard chestCard = holdJailCard(state, 0, COMMUNITY_CHEST);
        fm.next(state, new UseJailCard(chestCard));
        assertFalse(state.isInJail(0));
        assertEquals(0, state.getJailRolls(0));
        assertEquals(1500, state.getCash(0));
        assertEquals(16, state.getCommunityChestDeck().getSize());
        assertEquals(chestCard, bottom(state.getCommunityChestDeck()));
        assertEquals(15, state.getChanceDeck().getSize());
        assertEquals(List.of(chanceCard), state.getJailCards(0).getComponents());
        // the player then rolls as normal
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
        roll(state, fm, 2, 3); // 10 + 5 = Marylebone Station
        assertEquals(sq("Marylebone Station"), state.getPosition(0));
        assertEquals(new BuyDecision(0, sq("Marylebone Station")), state.currentActionInProgress());
        assertCardsConserved(state);
    }

    @Test
    public void theChanceCardGoesBackToTheChancePile() {
        startTurn(state, 0, sq("Jail"));
        putInJail(state, 0, 0);
        MonopolyCard chanceCard = holdJailCard(state, 0, CHANCE);
        fm.next(state, new UseJailCard(chanceCard));
        assertEquals(16, state.getChanceDeck().getSize());
        assertEquals(chanceCard, bottom(state.getChanceDeck()));
        assertEquals(16, state.getCommunityChestDeck().getSize());
        assertEquals(0, state.getJailCards(0).getSize());
    }

    @Test
    public void afterUsingTheCardADoubleGivesAnotherRoll() {
        startTurn(state, 0, sq("Jail"));
        putInJail(state, 0, 0);
        fm.next(state, new UseJailCard(holdJailCard(state, 0, CHANCE)));
        roll(state, fm, 2, 2); // 10 + 4 = Northumberland Avenue
        assertEquals(sq("Northumberland Avenue"), state.getPosition(0));
        decline(state, fm, sq("Northumberland Avenue"));
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
    }

    @Test
    public void bankruptToAPlayerTheJailCardsGoToTheCreditor() {
        startTurn(state, 0, sq("Bond Street"));
        MonopolyCard chanceCard = holdJailCard(state, 0, CHANCE);
        MonopolyCard chestCard = holdJailCard(state, 0, COMMUNITY_CHEST);
        state.setCash(0, 10);
        give(state, 1, sq("Mayfair"));
        roll(state, fm, 2, 3); // 34 + 5 = Mayfair, rent 50 > 10
        assertTrue(state.isBankrupt(0));
        assertEquals(0, state.getJailCards(0).getSize());
        assertEquals(Set.of(chanceCard, chestCard), Set.copyOf(state.getJailCards(1).getComponents()));
        assertEquals(15, state.getChanceDeck().getSize());
        assertEquals(15, state.getCommunityChestDeck().getSize());
        assertCardsConserved(state);
    }

    @Test
    public void bankruptToTheBankTheJailCardsGoToTheBottomOfTheirOwnPiles() {
        startTurn(state, 0, sq("Liverpool Street Station"));
        MonopolyCard chanceCard = holdJailCard(state, 0, CHANCE);
        MonopolyCard chestCard = holdJailCard(state, 0, COMMUNITY_CHEST);
        state.setCash(0, 50);
        roll(state, fm, 1, 2); // 35 + 3 = Super Tax, 100 > 50
        assertTrue(state.isBankrupt(0));
        assertEquals(0, state.getJailCards(0).getSize());
        assertEquals(16, state.getChanceDeck().getSize());
        assertEquals(chanceCard, bottom(state.getChanceDeck()));
        assertEquals(16, state.getCommunityChestDeck().getSize());
        assertEquals(chestCard, bottom(state.getCommunityChestDeck()));
        assertCardsConserved(state);
    }
}

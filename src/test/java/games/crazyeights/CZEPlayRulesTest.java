package games.crazyeights;

import core.CoreConstants.GameResult;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.crazyeights.actions.DrawCard;
import games.crazyeights.actions.Pass;
import games.crazyeights.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static core.components.FrenchCard.Suite.*;
import static games.crazyeights.CZETestUtils.*;
import static org.junit.Assert.*;

public class CZEPlayRulesTest {

    CZEGameState state;
    CZEForwardModel fm;

    @Before
    public void setup() {
        CZEParameters params = new CZEParameters();
        params.setRandomSeed(42);
        state = new CZEGameState(params, 3);
        fm = new CZEForwardModel();
        fm.setup(state);
    }

    private Set<AbstractAction> availableActions() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    private static Set<AbstractAction> eightPlays(int player, FrenchCard eight) {
        Set<AbstractAction> plays = new HashSet<>();
        for (FrenchCard.Suite suit : FrenchCard.Suite.values())
            plays.add(new PlayCard(eight, suit));
        return plays;
    }

    private Deck<FrenchCard> hand(int player) {
        return state.getPlayerHands().get(player);
    }

    @Test
    public void cardsMatchingSuitOrRankCanBePlayedAndAnEightCanNominateAnySuit() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9H"), card("5S"), card("KC"), card("8D"));
        // Nine of Hearts matches the suit, Five of Spades the rank; King of Clubs matches neither
        Set<AbstractAction> expected = new HashSet<>(List.of(
                new PlayCard(card("9H"), Hearts),
                new PlayCard(card("5S"), Spades)));
        expected.addAll(eightPlays(0, card("8D")));
        assertEquals(expected, availableActions());
    }

    @Test
    public void afterAnEightOnlyTheNominatedSuitOrAnotherEightCanBePlayed() {
        // Eight of Clubs with Hearts nominated: a Club no longer matches
        setTopDiscard(state, card("8C"), Hearts);
        giveHand(state, 0, card("5C"), card("9H"), card("8S"), card("KD"));
        Set<AbstractAction> expected = new HashSet<>(List.of(new PlayCard(card("9H"), Hearts)));
        expected.addAll(eightPlays(0, card("8S")));
        assertEquals(expected, availableActions());
    }

    @Test
    public void eachEightInHandGivesOnePlayPerSuit() {
        setTopDiscard(state, card("KH"));
        giveHand(state, 0, card("8S"), card("8D"), card("3C"));
        Set<AbstractAction> expected = eightPlays(0, card("8S"));
        expected.addAll(eightPlays(0, card("8D")));
        assertEquals(expected, availableActions());
        assertEquals("no duplicate actions", 8, fm.computeAvailableActions(state).size());
    }

    @Test
    public void drawIsTheOnlyActionWhenNoCardCanBePlayed() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9C"), card("KS"));
        assertEquals(List.of(new DrawCard()), fm.computeAvailableActions(state));
    }

    @Test
    public void passIsTheOnlyActionWhenNothingCanBePlayedOrDrawn() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9C"), card("KS"));
        leaveNothingToDraw(state, 1);
        assertEquals(List.of(new Pass()), fm.computeAvailableActions(state));
    }

    @Test
    public void playingAnEightMovesItToTheDiscardPileAndSetsTheNominatedSuit() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9H"), card("8S"), card("KC"));
        int discards = state.getDiscardPile().getSize();

        fm.next(state, new PlayCard(card("8S"), Diamonds));

        assertEquals(Set.of(card("9H"), card("KC")), new HashSet<>(hand(0).getComponents()));
        assertEquals(card("8S"), state.getTopCard());
        assertEquals(discards + 1, state.getDiscardPile().getSize());
        assertEquals(Diamonds, state.getCurrentSuit());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void playingOnRankChangesTheSuitToMatch() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("5S"), card("2C"));

        fm.next(state, new PlayCard(card("5S"), Spades));
        assertEquals(Spades, state.getCurrentSuit());

        // the next player now needs a Spade or a Five
        giveHand(state, 1, card("5D"), card("9H"), card("JS"));
        assertEquals(Set.of(new PlayCard(card("5D"), Diamonds), new PlayCard(card("JS"), Spades)),
                availableActions());
    }

    @Test
    public void drawingTakesTheTopOfTheStockAndEndsTheTurn() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9C"), card("KS"));
        FrenchCard topOfStock = state.getDrawDeck().peek();
        int stock = state.getDrawDeck().getSize();

        fm.next(state, new DrawCard());

        assertEquals(3, hand(0).getSize());
        assertTrue(hand(0).contains(topOfStock));
        assertEquals(stock - 1, state.getDrawDeck().getSize());
        // the drawn card cannot be played this turn
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void playingTheLastCardWinsAndOthersScoreMinusTheirPenalty() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9H"));
        giveHand(state, 1, card("KS"), card("8D"), card("AC"));   // 10 + 50 + 1
        giveHand(state, 2, card("2C"), card("10D"));              // 2 + 10

        fm.next(state, new PlayCard(card("9H"), Hearts));

        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
        assertEquals(0, state.getGameScore(0), 0.0);
        assertEquals(-61, state.getGameScore(1), 0.0);
        assertEquals(-12, state.getGameScore(2), 0.0);
    }

    @Test
    public void handBuiltActionsEqualGeneratedOnesAndApplyToARedeterminisedCopy() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9H"), card("2C"));
        CZEGameState copy = (CZEGameState) state.copy(0);
        PlayCard play = new PlayCard(card("9H"), Hearts);

        assertTrue(fm.computeAvailableActions(copy).contains(play));
        fm.next(copy, play);

        assertEquals(card("9H"), copy.getTopCard());
        assertEquals(card("5H"), state.getTopCard());
        assertTrue(hand(0).contains(card("9H")));
    }
}

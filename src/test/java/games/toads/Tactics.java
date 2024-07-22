package games.toads;

import core.CoreConstants;
import core.actions.AbstractAction;
import games.toads.abilities.*;
import games.toads.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static games.toads.ToadConstants.ToadCardType.*;
import static games.toads.ToadConstants.ToadGamePhase.POST_BATTLE;
import static org.junit.Assert.*;

public class Tactics {


    ToadParameters params;
    ToadGameState state;
    ToadForwardModel fm;
    Random rnd;

    @Before
    public void setUp() {
        params = new ToadParameters();
        params.setRandomSeed(933);
        state = new ToadGameState(params, 2);
        fm = new ToadForwardModel();
        fm.setup(state);
        rnd = new Random(933);
    }

    @Test
    public void tricksterI() {
        state.fieldCards[0] = new ToadCard("Five", 5, BERSERKER);
        state.fieldCards[1] = new ToadCard("Six", 6, ICON_BEARER);
        state.hiddenFlankCards[0] = new ToadCard("Six", 6, ICON_BEARER);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, TRICKSTER);

        // with no tactics this is a 1 : 1 draw

        // p1  Trickster swaps with IconBearer
        // Field is now 5 : 6  (draw - due to the IconBearer of p0 (not cancelled))
        // Flank is now 6 : 6  (draw)

        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[1][0]);
    }

    @Test
    public void tricksterII() {
        state.fieldCards[0] = new ToadCard("Five", 5, BERSERKER);
        state.fieldCards[1] = new ToadCard("Four", 4, SABOTEUR);
        state.hiddenFlankCards[0] = new ToadCard("Six", 6, ICON_BEARER);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, TRICKSTER);

        // with no tactics this is 2 : 0

        // p1  Trickster swaps with Saboteur, and gains 2 points
        // Field is now 5 : 5  (draw)
        // Flank is now 6 : 4  (p0 wins) - IconBearer is not cancelled; but has no effect

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[1][0]);
    }

    @Test
    public void tricksterIII() {
        state.battlesWon[0][0] = 1;
        state.battlesWon[0][1] = 2;

        state.fieldCards[0] = new ToadCard("Six", 6, ICON_BEARER);
        state.fieldCards[1] = new ToadCard("Four", 4, SABOTEUR);
        state.hiddenFlankCards[0] = new ToadCard("Five", 5, BERSERKER);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, TRICKSTER);

        // with no tactics this is 2 : 0

        // p1  Trickster swaps with Saboteur, and gains 2 points
        // Field is now 6 : 5  (p0 wins)
        // Flank is now 7 : 4  (p0 wins)  [Berserker gains 2 points, as not cancelled by Saboteur...but has no impact]

        // p0 started behind, so gains 2 points

        fm._afterAction(state, null);
        assertEquals(3, state.battlesWon[0][0]);
        assertEquals(2, state.battlesWon[0][1]);
    }

    @Test
    public void iconBearerActivatesTrickster() {
        state.battlesWon[0][0] = 1;
        state.fieldCards[0] = new ToadCard("Five", 5, null);
        state.fieldCards[1] = new ToadCard("Trickster", 3, TRICKSTER);
        state.hiddenFlankCards[0] = new ToadCard("Seven", 7, null);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, ICON_BEARER);

        // with no tactics this is 2 : 0

        // p1 - Iconbearer activates, then activates Trickster, which gains 3 points and Swaps with IconBearer
        // Field is now 5 : 6  (p1 wins)
        // Flank is now 7 : 6  (draw, due to activated IconBearer)

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void tricksterSaboteurIconBearer() {
        state.fieldCards[0] = new ToadCard("Four", 4, SABOTEUR);
        state.fieldCards[1] = new ToadCard("Four", 4, SABOTEUR);
        state.hiddenFlankCards[0] = new ToadCard("Six", 6, ICON_BEARER);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, TRICKSTER);

        // with no tactics this is 1 : 0

        // p1 Trickster swaps with Saboteur, and gains 2 points
        // Field is now 4 : 5  (draw) IconBearer flank ability is not cancelled by the Saboteur opposite
        // Flank is now 6 : 4  (p0 wins)

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void assassinVersusTricksterI() {
        state.fieldCards[0] = new ToadCard("Five", 5, BERSERKER);
        state.fieldCards[1] = new ToadCard("Four", 4, SABOTEUR);
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 1, ASSASSIN);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, TRICKSTER);

        // with no tactics this is 0 : 2

        // Trickster swaps with Four, gaining 2 points
        // And Assassin copies Five, to no effect

        // Field is now 5 : 5 (draw)
        // Flank is now 0 : 4 (p1 wins)

        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void assassinVersusTricksterII() {
        state.battlesWon[0][1] = 4;
        // we adapt the previous test to check that the Assassin copies the tactics of the Berserker
        state.fieldCards[0] = new ToadCard("Five", 5, BERSERKER);
        state.fieldCards[1] = new ToadCard("Four", 4, SABOTEUR);
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 1, ASSASSIN);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, TRICKSTER);

        // with no tactics this is 0 : 2

        // Trickster swaps with Four, gaining 2 points
        // And Assassin copies Five, gaining 4 points

        // Field is now 9 : 5 (p0 wins)
        // Flank is now 4 : 4 (draw)

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(4, state.battlesWon[0][1]);
    }

    @Test
    public void assassinCopiesIconBearerI() {
        state.battlesWon[0][0] = 2;
        state.fieldCards[0] = new ToadCard("IconBearer", 6, ICON_BEARER);
        state.fieldCards[1] = new ToadCard("IconBearer", 6, ICON_BEARER);
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 0, ASSASSIN);
        state.hiddenFlankCards[1] = new ToadCard("Scout", 2, SCOUT);

        // without tactics this would be 0 : 2

        // With tactics Assassin copies IconBearer, which counters the +1 from the Scout
        // Field is now 6 : 7 (draw)
        // Flank is now 0 : 2 (p1)
        fm._afterAction(state, null);
        assertEquals(2, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
        assertEquals(1, state.battlesTied[0]);
    }

    @Test
    public void assassinCopiesIconBearerII() {
        state.battlesWon[0][0] = 2;
        state.fieldCards[0] = new ToadCard("IconBearer", 6, ICON_BEARER);
        state.fieldCards[1] = new ToadCard("IconBearer", 6, ICON_BEARER);
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 1, ASSASSIN);
        state.hiddenFlankCards[1] = new ToadCard("Scout", 2, SCOUT);

        // without tactics this would be 0 : 2

        // With tactics Assassin copies IconBearer, which counters the +1 from the Scout
        // And also makes the Flank a draw from the original Iconbearer
        // Field is now 6 : 7 (draw)
        // Flank is now 1 : 2 (draw)
        fm._afterAction(state, null);
        assertEquals(2, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
        assertEquals(2, state.battlesTied[0]);
    }

    @Test
    public void generalAbilityOneI() {
        state.battlesWon[0][1] = 1;
        state.fieldCards[0] = new ToadCard("Five", 5, BERSERKER);
        state.fieldCards[1] = new ToadCard("Five", 5, BERSERKER);
        state.hiddenFlankCards[0] = new ToadCard("General", 7, GENERAL_ONE);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, ICON_BEARER);

        // This is 1 : 0...so not a Frog

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityOneII() {
        state.fieldCards[0] = new ToadCard("Five", 5, BERSERKER);
        state.fieldCards[1] = new ToadCard("Four", 4, SABOTEUR);
        state.hiddenFlankCards[0] = new ToadCard("General", 7, GENERAL_ONE);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, null);

        // This is 2 : 0...so a Frog...that becomes 2 points

        fm._afterAction(state, null);
        assertEquals(2, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityTwoI() {
        state.fieldCards[0] = new ToadCard("Five", 5, BERSERKER);
        state.fieldCards[1] = new ToadCard("Five", 5, BERSERKER);
        state.hiddenFlankCards[0] = new ToadCard("General", 7, GENERAL_TWO);
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7, null);

        // This is a tie

        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityTwoII() {
        state.battlesTied[0] = 2;
        state.fieldCards[0] = new ToadCard("Five", 5, BERSERKER);
        state.fieldCards[1] = new ToadCard("Five", 5, BERSERKER);
        state.hiddenFlankCards[0] = new ToadCard("General", 7, GENERAL_TWO);
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7, null);

        // This is now not a tie

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityWorksAfterDefeat() {
        // this checks that the General special ability still holds (even though defeated by Assassin)
        state.battlesTied[0] = 2;

        // This is a test-only hack, so that the Assassin does not copy the tactics of the General
        playCards(
                new ToadCard("Five", 5, BERSERKER), // field
                new ToadCard("Assassin", 0, ASSASSIN, new Assassin(), null), // Flank
                new ToadCard("Four", 4, null),  // Field
                new ToadCard("Seven", 7, GENERAL_TWO)// flank
        );
        // with no tactics this is 2 : 0

        // With tactics the 4 becomes a 6, so we are 1 : 1

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityNegatedByAssassinCopyingSaboteur() {
        // this checks that the General special ability still holds (even though defeated by Assassin)
        state.battlesWon[0][0] = 0;
        state.battlesWon[0][1] = 1;
        state.battlesTied[0] = 2;

        state.fieldCards[0] = new ToadCard("Four", 4, SABOTEUR);
        state.fieldCards[1] = new ToadCard("Four", 4, SABOTEUR);
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 1, ASSASSIN);
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7, GENERAL_TWO);

        // with no tactics this is 1 : 0

        // With tactics this is still 1 : 0, because the Assassin copies the Saboteur, and the GeneralTwo is negated

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void berserkerWithOneBonus() {
        state.battlesWon[0][1] = 1;
        state.fieldCards[0] = new ToadCard("One", 1, null);
        state.fieldCards[1] = new ToadCard("Four", 4, SABOTEUR);
        state.hiddenFlankCards[0] = new ToadCard("Berserker", 5, BERSERKER);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, ICON_BEARER);

        // Without tactics this is 0 : 2

        // Berserker adds 2 from battles won
        // Field is 1 : 4 (p1 wins)
        // Flank is 6 : 6 (draw)

        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(2, state.battlesWon[0][1]);
    }

    @Test
    public void berserkerFieldWithTwoBonusToNotMakeADraw() {
        state.battlesWon[0][1] = 2;
        state.fieldCards[0] = new ToadCard("One", 1, null);
        state.fieldCards[1] = new ToadCard("Four", 4, SABOTEUR);
        state.hiddenFlankCards[0] = new ToadCard("Berserker", 5, BERSERKER);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, ICON_BEARER);

        // Without tactics this is 0 : 2

        // Berserker adds 2 from battles won
        // Field is 1 : 4 (p1 wins)
        // Flank is 7 : 6 (p0 wins)

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(3, state.battlesWon[0][1]);
    }

    @Test
    public void berserkerFlankWithTwoBonusToMakeADraw() {
        state.battlesWon[0][1] = 2;
        state.battlesWon[1][1] = 1;
        state.fieldCards[0] = new ToadCard("Two", 2, SCOUT);
        state.fieldCards[1] = new ToadCard("Two", 2, SCOUT);
        state.hiddenFlankCards[0] = new ToadCard("Berserker", 5, BERSERKER);
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7, null);

        // without tactics this is 0 : 1

        // Berserker adds 2 from battles won
        // Field is 2 : 2 (draw)
        // Flank is 7 : 7 (draw)

        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(2, state.battlesWon[0][1]);
    }

    @Test
    public void iconBearerWithSaboteur() {
        state.battlesWon[0][1] = 1;
        state.fieldCards[0] = new ToadCard("Two", 2, SCOUT);
        state.fieldCards[1] = new ToadCard("Two", 2, SCOUT);
        state.hiddenFlankCards[0] = new ToadCard("IconBearer", 6, ICON_BEARER);
        state.hiddenFlankCards[1] = new ToadCard("Saboteur", 4, SABOTEUR);

        // without Tactics this is 1 : 0

        // IconBearer is cancelled by Saboteur, so does not turn the Field battle into a Win
        // This is hence 1 : 0

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }


    @Test
    public void iconBearerWithoutSaboteur() {
        state.fieldCards[0] = new ToadCard("One", 1, null);
        state.fieldCards[1] = new ToadCard("Two", 2, SCOUT);
        state.hiddenFlankCards[0] = new ToadCard("IconBearer", 6, ICON_BEARER);
        state.hiddenFlankCards[1] = new ToadCard("Five", 5, BERSERKER);

        // without Tactics this is 1 : 1

        // IconBearer turns the Field battle into a Draw for 0 : 1

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    /**
     * @param n  Number of cards to skip
     * @param p0 Can p0 see p1's cards
     * @param p1 Can p1 see p0's cards
     */
    private void checkVisibilityOfHands(int n, boolean p0, boolean p1) {
        for (int i = n; i < state.playerHands.get(0).getSize(); i++) {
            assertEquals(p0, state.playerHands.get(0).isComponentVisible(i, 1));
        }
        for (int i = n; i < state.playerHands.get(1).getSize(); i++)
            assertEquals(p1, state.playerHands.get(1).isComponentVisible(i, 0));
    }

    @Test
    public void scoutI() {
        checkVisibilityOfHands(0, false, false);
        state.fieldCards[0] = new ToadCard("Three", 3, TRICKSTER);
        state.fieldCards[1] = new ToadCard("Two", 2, SCOUT);
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, SCOUT);
        state.hiddenFlankCards[1] = new ToadCard("Four", 4, SABOTEUR);

        // without tactics this is 1 : 1
        // which is also true here, as the Saboteur cancels the Scout

        fm._afterAction(state, null);
        checkVisibilityOfHands(0, false, false);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void iconBearerActivatesScout() {
        checkVisibilityOfHands(0, false, false);
        state.fieldCards[0] = new ToadCard("Three", 3, TRICKSTER);
        state.fieldCards[1] = new ToadCard("Scout", 2, SCOUT);
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, SCOUT);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, ICON_BEARER);

        // without tactics this is 1 : 1
        // Field is now 4 : 2 (p0 wins) Iconbearer cannot help; but it does activate the Scout ability
        // Flank is now 2 : 7 (p1 wins)

        fm._afterAction(state, null);
        checkVisibilityOfHands(2, true, true);

        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void scoutIII() {
        checkVisibilityOfHands(0, false, false);
        state.fieldCards[0] = new ToadCard("Three", 3, TRICKSTER);
        state.fieldCards[1] = new ToadCard("One", 1, null);
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, SCOUT);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, TRICKSTER);

        // without tactics this is 1 : 1
        // Field is now 4 : 3 (p0 wins)
        // Flank is now 2 : 1 (p0 wins)

        fm._afterAction(state, null);
        checkVisibilityOfHands(0, false, true);

        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void cardMovedWithTricksterDoesNotTriggerTacticsI() {
        state.fieldCards[0] = new ToadCard("Three", 3, TRICKSTER);
        state.fieldCards[1] = new ToadCard("Scout", 2, SCOUT);
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, SCOUT);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, TRICKSTER);

        fm._afterAction(state, null);
        checkVisibilityOfHands(0, false, true);
    }

    @Test
    public void cardMovedWithTricksterDoesNotTriggerTacticsII() {
        state.fieldCards[0] = new ToadCard("Seven", 7, null);
        state.fieldCards[1] = new ToadCard("IconBearer", 6, ICON_BEARER);
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, SCOUT);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, TRICKSTER);

        // Trickster swaps, giving:
        // Field is now 7 : 6 (p0 wins)
        // Flank is now 2 : 6 (p1 wins)

        // If the IconBearer were active, this would be 0 : 1 instead

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void assaultCannonNamesCard() {
        playCards(
                new ToadCard("Five", 5, null), // field
                new ToadCard("AC", 0, ASSAULT_CANNON), // Flank
                new ToadCard("Five", 5, null),  // Field
                new ToadCard("Six", 6, null) // flank
        );

        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(8, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof ForceOpponentDiscard));
        fm.next(state, actions.get(0));
        assertEquals(1, state.getCurrentPlayer());
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().allMatch(a -> a instanceof PlayFieldCard));
    }

    @Test
    public void assaultCannonActivatedByIconBearer() {
        playCards(
                new ToadCard("Five", 5, null), // field
                new ToadCard("Scout", 2, SCOUT), // Flank
                new ToadCard("AC", 0, ASSAULT_CANNON),  // Field
                new ToadCard("Six", 6, ICON_BEARER) // flank
        );

        assertEquals(1, state.getCurrentPlayer());
        assertEquals(POST_BATTLE, state.getGamePhase());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(8, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof ForceOpponentDiscard));
        fm.next(state, actions.get(0));
        assertEquals(1, state.getCurrentPlayer());
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().allMatch(a -> a instanceof PlayFieldCard));
    }

    @Test
    public void assaultCannonShiftIsToBottomAndVisible() {
        // if we ForceOpponentDiscard for a card they have, then we should be able to see it at the bottom of the pile
        // and it should stay there after a redeterminisation
        playCards(
                new ToadCard("Five", 5, null), // field
                new ToadCard("AC", 0, ASSAULT_CANNON), // Flank
                new ToadCard("Five", 5, null),  // Field
                new ToadCard("Six", 6, null) // flank
        );
        // now find a card in the player's hand
        ToadCard card = state.playerHands.get(1).peek();
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new ForceOpponentDiscard(card.value)));
        fm.next(state, new ForceOpponentDiscard(card.value));
        assertEquals(state.getPlayerDeck(1).peek(2), card);
        assertEquals(3, state.getPlayerDeck(1).getSize());
        assertEquals(3, state.getPlayerDeck(0).getSize());
        assertTrue(state.getPlayerDeck(1).isComponentVisible(2, 0));
        assertTrue(state.getPlayerDeck(1).isComponentVisible(2, 1));
        assertFalse(state.getPlayerDeck(1).isComponentVisible(0, 0));
        assertFalse(state.getPlayerDeck(1).isComponentVisible(0, 1));

        // TODO: And check reshuffle preserves visibility
        ToadGameState copy = (ToadGameState) state.copy(0);
        assertEquals(copy.getPlayerDeck(1).peek(2), card);
        assertTrue(copy.getPlayerDeck(1).isComponentVisible(2, 0));
        assertTrue(copy.getPlayerDeck(1).isComponentVisible(2, 1));
        assertFalse(copy.getPlayerDeck(1).isComponentVisible(0, 0));
        assertFalse(copy.getPlayerDeck(1).isComponentVisible(0, 1));
    }


    @Test
    public void firstPlayerCorrectlySet() {
        // This checks that inclusion of Tactics interrupts does not change the correct resetting of the player for each new battle
        params.secondRoundStart = ToadParameters.SecondRoundStart.ONE;
        for (int gameLoop = 0; gameLoop < 10; gameLoop++) {
            fm.setup(state);
            assertEquals(0, state.getCurrentPlayer());
            int nextPlayer = 0;
            do {
                int startingTurn = state.getTurnCounter() + 8 * state.getRoundCounter();
                nextPlayer = 1 - nextPlayer;
                do {
                    // sub loop
                    AbstractAction action = fm.computeAvailableActions(state).get(rnd.nextInt(fm.computeAvailableActions(state).size()));
                    System.out.println(action + " " + state.getCurrentPlayer() + " Turn " + state.getTurnCounter());
                    fm.next(state, action);
                } while (state.getTurnCounter() + 8 * state.getRoundCounter() <= startingTurn + 1 && state.isNotTerminal());
                // Each player effectively gets four consecutive actions, as after they have defended, they are the attacker in the next battle
                if (state.getGameStatus() != CoreConstants.GameResult.GAME_END) {
                    System.out.println("Round " + state.getRoundCounter() + " Turn " + state.getTurnCounter() + " Player " + state.getCurrentPlayer() + " Expected " + nextPlayer);
                    assertEquals(nextPlayer, state.getCurrentPlayer());
                }
            } while (state.getGameStatus() != CoreConstants.GameResult.GAME_END);
        }
    }

    private void playCards(ToadCard... cardsInOrder) {
        for (int i = 0; i < cardsInOrder.length; i++) {
            state.getPlayerHand(state.getCurrentPlayer()).add(cardsInOrder[i]);
            AbstractAction action = i % 2 == 0 ? new PlayFieldCard(cardsInOrder[i]) : new PlayFlankCard(cardsInOrder[i]);
            fm.next(state, action);
        }
    }
}

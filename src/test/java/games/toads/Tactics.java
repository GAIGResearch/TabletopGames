package games.toads;

import games.toads.abilities.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class Tactics {


    ToadParameters params;
    ToadGameState state;
    ToadForwardModel fm;

    @Before
    public void setUp() {
        params = new ToadParameters();
        params.setRandomSeed(933);
        state = new ToadGameState(params, 2);
        fm = new ToadForwardModel();
        fm.setup(state);
    }

    @Test
    public void tricksterI() {
        state.fieldCards[0] = new ToadCard("Five", 5, new Berserker());
        state.fieldCards[1] = new ToadCard("Six", 6, new IconBearer());
        state.hiddenFlankCards[0] = new ToadCard("Six", 6, new IconBearer());
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, new Trickster());

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
        state.fieldCards[0] = new ToadCard("Five", 5, new Berserker());
        state.fieldCards[1] = new ToadCard("Four", 4, new Saboteur());
        state.hiddenFlankCards[0] = new ToadCard("Six", 6, new IconBearer());
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, new Trickster());

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

        state.fieldCards[0] = new ToadCard("Six", 6, new IconBearer());
        state.fieldCards[1] = new ToadCard("Four", 4, new Saboteur());
        state.hiddenFlankCards[0] = new ToadCard("Five", 5, new Berserker());
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, new Trickster());

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
    public void tricksterSaboteurIconBearer() {
        state.fieldCards[0] = new ToadCard("Four", 4, new Saboteur());
        state.fieldCards[1] = new ToadCard("Four", 4, new Saboteur());
        state.hiddenFlankCards[0] = new ToadCard("Six", 6, new IconBearer());
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, new Trickster());

        // with no tactics this is 1 : 0

        // p1 Trickster swaps with Saboteur, and gains 2 points
        // Field is now 4 : 5  (draw) IconBearer flank ability is not cancelled by the Saboteur opposite
        // Flank is now 6 : 4  (p0 wins)

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void assassinVersusTrickster() {
        state.fieldCards[0] = new ToadCard("Five", 5, new Berserker());
        state.fieldCards[1] = new ToadCard("Four", 4, new Saboteur());
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 1, new Assassin());
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, new Trickster());

        // with no tactics this is 0 : 2

        // Trickster swaps with Five, gaining 2 points
        // And Assassin copies it, swaps with 5, gaining 2 points...

        // Field is now 3 : 5 (p1 wins)
        // Flank is now 5 : 4 (p0 wins)

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void assassinCopiesBerserker() {
        state.battlesWon[0][1] = 2;
        state.fieldCards[0] = new ToadCard("IconBearer", 6, new IconBearer());
        state.fieldCards[1] = new ToadCard("Berserker", 5, new Berserker());
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 0, new Assassin());
        state.hiddenFlankCards[1] = new ToadCard("Scout", 2, new Scout());

        // without tactics this would be 1 : 1

        // With tactics, the Scout adds +1 to p1 IconBearer
        // but Assassin copies Berserker, adding +2 to itself, equalling the Scout
        // Field is now 7 : 5 (p0 wins)
        // Flank is now 2 : 2 (draw)
        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(2, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityOneI() {
        state.battlesWon[0][1] = 1;
        state.fieldCards[0] = new ToadCard("Five", 5, new Berserker());
        state.fieldCards[1] = new ToadCard("Five", 5, new Berserker());
        state.hiddenFlankCards[0] = new ToadCard("General", 7, new GeneralOne());
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, new IconBearer());

        // This is 1 : 0...so not a Frog

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityOneII() {
        state.fieldCards[0] = new ToadCard("Five", 5, new Berserker());
        state.fieldCards[1] = new ToadCard("Four", 4, new Saboteur());
        state.hiddenFlankCards[0] = new ToadCard("General", 7, new GeneralOne());
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, new IconBearer());

        // This is 2 : 0...so a Frog...that becomes 2 points

        fm._afterAction(state, null);
        assertEquals(2, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityTwoI() {
        state.fieldCards[0] = new ToadCard("Five", 5, new Berserker());
        state.fieldCards[1] = new ToadCard("Five", 5, new Berserker());
        state.hiddenFlankCards[0] = new ToadCard("General", 7, new GeneralTwo());
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7);

        // This is a tie

        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityTwoII() {
        state.battlesTied[0] = 2;
        state.fieldCards[0] = new ToadCard("Five", 5, new Berserker());
        state.fieldCards[1] = new ToadCard("Five", 5, new Berserker());
        state.hiddenFlankCards[0] = new ToadCard("General", 7, new GeneralTwo());
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7);

        // This is now not a tie

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void generalAbilityWorksAfterDefeat() {
        // this checks that the General special ability still holds (even though defeated by Assassin)
        state.battlesTied[0] = 2;

        state.fieldCards[0] = new ToadCard("Five", 5, new Berserker());
        state.fieldCards[1] = new ToadCard("Four", 4, new Saboteur());
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 1, new Assassin());
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7, new GeneralTwo());

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

        state.fieldCards[0] = new ToadCard("Four", 4, new Saboteur());
        state.fieldCards[1] = new ToadCard("Four", 4, new Saboteur());
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 1, new Assassin());
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7, new GeneralTwo());

        // with no tactics this is 1 : 0

        // With tactics this is still 1 : 0, because the Assassin copies the Saboteur, and the GeneralTwo is negated

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void berserkerWithOneBonus() {
        state.battlesWon[0][1] = 1;
        state.fieldCards[0] = new ToadCard("One", 1);
        state.fieldCards[1] = new ToadCard("Four", 4, new Saboteur());
        state.hiddenFlankCards[0] = new ToadCard("Berserker", 5, new Berserker());
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, new IconBearer());

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
        state.fieldCards[0] = new ToadCard("One", 1);
        state.fieldCards[1] = new ToadCard("Four", 4, new Saboteur());
        state.hiddenFlankCards[0] = new ToadCard("Berserker", 5, new Berserker());
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, new IconBearer());

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
        state.fieldCards[0] = new ToadCard("Two", 2, new Scout());
        state.fieldCards[1] = new ToadCard("Two", 2, new Scout());
        state.hiddenFlankCards[0] = new ToadCard("Berserker", 5, new Berserker());
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7);

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
        state.fieldCards[0] = new ToadCard("Two", 2, new Scout());
        state.fieldCards[1] = new ToadCard("Two", 2, new Scout());
        state.hiddenFlankCards[0] = new ToadCard("IconBearer", 6, new IconBearer());
        state.hiddenFlankCards[1] = new ToadCard("Saboteur", 4, new Saboteur());

        // without Tactics this is 1 : 0

        // IconBearer is cancelled by Saboteur, so does not turn the Field battle into a Win
        // This is hence 1 : 0

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }


    @Test
    public void iconBearerWithoutSaboteur() {
        state.battlesWon[0][1] = 1;
        state.fieldCards[0] = new ToadCard("Two", 2, new Scout());
        state.fieldCards[1] = new ToadCard("Two", 2, new Scout());
        state.hiddenFlankCards[0] = new ToadCard("IconBearer", 6, new IconBearer());
        state.hiddenFlankCards[1] = new ToadCard("Five", 5, new Berserker());

        // without Tactics this is 1 : 0

        // IconBearer turns the Field battle into a Win, for  2 : 0

        fm._afterAction(state, null);
        assertEquals(2, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }


    @Test
    public void scoutI() {
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertFalse(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertFalse(state.playerHands.get(1).isComponentVisible(i, 0));
        state.fieldCards[0] = new ToadCard("Three", 3, new Trickster());
        state.fieldCards[1] = new ToadCard("Two", 2, new Scout());
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, new Scout());
        state.hiddenFlankCards[1] = new ToadCard("Four", 4, new Saboteur());

        // without tactics this is 1 : 1
        // which is also true here, as the Saboteur cancels the Scout

        fm._afterAction(state, null);
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertFalse(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertFalse(state.playerHands.get(1).isComponentVisible(i, 0));

        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void iconBearerActivatesScout() {
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertFalse(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertFalse(state.playerHands.get(1).isComponentVisible(i, 0));
        state.fieldCards[0] = new ToadCard("Three", 3, new Trickster());
        state.fieldCards[1] = new ToadCard("Scout", 2, new Scout());
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, new Scout());
        state.hiddenFlankCards[1] = new ToadCard("Six", 6, new IconBearer());

        // without tactics this is 1 : 1
        // Field is now 4 : 2 (p0 wins) Iconbearer cannot help; but it does activate the Scout ability
        // Flank is now 2 : 7 (p1 wins)

        fm._afterAction(state, null);
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertTrue(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertTrue(state.playerHands.get(1).isComponentVisible(i, 0));

        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void scoutIII() {
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertFalse(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertFalse(state.playerHands.get(1).isComponentVisible(i, 0));
        state.fieldCards[0] = new ToadCard("Three", 3, new Trickster());
        state.fieldCards[1] = new ToadCard("One", 1);
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, new Scout());
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, new Trickster());

        // without tactics this is 1 : 1
        // Field is now 4 : 3 (p0 wins)
        // Flank is now 2 : 1 (p0 wins)

        fm._afterAction(state, null);
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertFalse(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertTrue(state.playerHands.get(1).isComponentVisible(i, 0));

        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void cardMovedWithTricksterDoesNotTriggerTacticsI() {
        state.fieldCards[0] = new ToadCard("Three", 3, new Trickster());
        state.fieldCards[1] = new ToadCard("Scout", 2, new Scout());
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, new Scout());
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, new Trickster());

        fm._afterAction(state, null);
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertFalse(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertTrue(state.playerHands.get(1).isComponentVisible(i, 0));
    }

    @Test
    public void cardMovedWithTricksterDoesNotTriggerTacticsII() {
        state.battlesWon[0][0] = 1;
        state.fieldCards[0] = new ToadCard("Three", 3, new Trickster());
        state.fieldCards[1] = new ToadCard("IconBearer", 6, new IconBearer());
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2, new Scout());
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3, new Trickster());

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void assaultCannonNamesCard() {
        fail("Not implemented yet");
    }
}

package games.dicemonastery;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.dicemonastery.*;
import games.dicemonastery.actions.*;
import games.dicemonastery.components.IlluminatedText;
import games.dicemonastery.components.Monk;
import games.dicemonastery.components.Treasure;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.BONUS_TOKEN.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase.BID;
import static games.dicemonastery.DiceMonasteryConstants.Phase.SACRIFICE;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.SUMMER;
import static org.junit.Assert.*;


public class CopyTests {

    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    Game game = GameType.DiceMonastery.createGameInstance(4, new DiceMonasteryParams(3));
    RandomPlayer rnd = new RandomPlayer();
    DiceMonasteryGameState s1 = (DiceMonasteryGameState) game.getGameState();
    List<Treasure> allTreasures = s1.availableTreasures();
    Treasure cape = allTreasures.stream().filter(t -> t.getComponentName().equals("Cape"))
            .findFirst().orElseThrow( () -> new AssertionError("Cape not found"));
    Treasure robe = allTreasures.stream().filter(t -> t.getComponentName().equals("Robe"))
            .findFirst().orElseThrow( () -> new AssertionError("Robe not found"));

    @Test
    public void placeMonkActionsGeneratedCorrectly() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        List<AbstractAction> actions;

        fm.next(state, new PlaceMonk(0, ActionArea.MEADOW));
        actions = fm.computeAvailableActions(state);

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, actions.get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void varyingNumbersOfMonksWorksWhenPlacing() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.createMonk(5, 3);

        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.monksIn(ActionArea.DORMITORY, 3).size() > 1);

        // at this point we should have 1 monks still to place for P3, and 0 each for all other players
        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(0)); // PlaceMonk

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0)); // ChooseMonk

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void usingAllMonks() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(0));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void seasonMovesOnAfterPlacingAndUsingAllMonks() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        do { // until we have two players left wit monks in the Chapel
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.monksIn(CHAPEL, -1).stream().map(Monk::getComponentID).distinct().count() > 2);

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(0));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void bakeBread() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        state.useAP(-1);
        // Has 2 Grain in STOREROOM at setup

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        (new BakeBread(1)).execute(state);

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }


    @Test
    public void hireNovice() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.useAP(-1);
        HireNovice action = new HireNovice(3);

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        try {
            fm.next(state, action);
            fail("Should throw exception as not enough AP");
        } catch (IllegalArgumentException e) {
            // expected
        }

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertEquals(midHash, startHash); // special case as the next action fails

        state.useAP(-2);
        fm.next(state, action);

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }


    @Test
    public void foodIsRemovedToFeedMonksAtYearEnd() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.addResource(1, BREAD, 10);
        state.addResource(1, HONEY, 2);
        state.addResource(1, GRAIN, 20);
        state.addResource(2, BREAD, 2);
        state.addResource(2, HONEY, 10);

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void allSurplusPerishablesAreRemovedAtYearEnd() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        state.addResource(0, BREAD, 20);
        state.addResource(0, HONEY, 20);
        state.addResource(0, CALF_SKIN, 20 - state.getResource(0, CALF_SKIN, STOREROOM));
        state.addResource(0, BEER, 20 - state.getResource(0, BEER, STOREROOM));
        state.addResource(0, GRAIN, 20 - state.getResource(0, GRAIN, STOREROOM));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

    }

    private void summerBidSetup() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (!(state.getSeason() == SUMMER));

        for (int p = 0; p < state.getNPlayers(); p++) {
            for (DiceMonasteryConstants.Resource r : DiceMonasteryConstants.Resource.values()) {
                state.addResource(p, r, -state.getResource(p, r, STOREROOM));
            }
        }

        for (int p = 0; p < state.getNPlayers(); p++) {
            state.addResource(p, BEER, 10);
            state.addResource(p, MEAD, 10);
        }
    }

    @Test
    public void copyDuringSummerBidsBlanksThemOutAndResetsTurnOrder() {
        summerBidSetup();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        // Players 0 and 1 then bid
        fm.next(state, new SummerBid(1, 0));
        fm.next(state, new SummerBid(1, 5));

        DiceMonasteryGameState fullCopy = (DiceMonasteryGameState) state.copy();
        // copy state from P2 perspective
        DiceMonasteryGameState perspectiveCopy = (DiceMonasteryGameState) state.copy(2);

        assertEquals(3, fullCopy.getCurrentPlayer());
        assertEquals(3, perspectiveCopy.getCurrentPlayer());
        assertNotSame(perspectiveCopy.hashCode(), fullCopy.hashCode());
        assertFalse(perspectiveCopy.equals(fullCopy));

        fm.next(fullCopy, new SummerBid(1, 0));
        fm.next(fullCopy, new SummerBid(1, 5));

        assertEquals(1, fullCopy.getCurrentPlayer());
        assertEquals(SACRIFICE, fullCopy.getGamePhase());
        assertTrue(fm.computeAvailableActions(fullCopy).stream().noneMatch(a -> a instanceof SummerBid));

        fm.next(perspectiveCopy, new SummerBid(1, 0));
        fm.next(perspectiveCopy, new SummerBid(1, 5));

        assertEquals(1, perspectiveCopy.getCurrentPlayer()); // still P0 to move
        assertEquals(BID, perspectiveCopy.getGamePhase());  // but still in SUMMER
        assertTrue(fm.computeAvailableActions(perspectiveCopy).stream().allMatch(a -> a instanceof SummerBid));
    }

    @Test
    public void summerBidsUniqueForAllPlayers() {
        summerBidSetup();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, new SummerBid(1, 0));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new SummerBid(1, 5));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void summerBidTiesForFirstAndThird() {
        summerBidSetup();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        fm.next(state, new SummerBid(1, 0));
        fm.next(state, new SummerBid(1, 0));

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, new SummerBid(0, 1));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new SummerBid(0, 1));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void takeTokenI() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        state.putToken(DORMITORY, DONATION, 0);

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new TakeToken(DONATION, DORMITORY, 1));
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void takeTokenII() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.putToken(DORMITORY, DONATION, 0);
        fm.next(state, new TakeToken(DONATION, DORMITORY, 1));

        state.putToken(DORMITORY, DEVOTION, 0);
        fm.next(state, new TakeToken(DEVOTION, DORMITORY, 1));

        state.putToken(DORMITORY, PRESTIGE, 0);
        fm.next(state, new TakeToken(PRESTIGE, DORMITORY, 2));

        state.putToken(DORMITORY, PROMOTION, 0);

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, new TakeToken(PROMOTION, DORMITORY, 3));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new PromoteMonk(1, DORMITORY));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    private void advanceToSummer() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (!(state.getSeason() == SUMMER));
    }

    private void emptyAllStores() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        for (int p = 0; p < state.getNPlayers(); p++) {
            for (DiceMonasteryConstants.Resource r : DiceMonasteryConstants.Resource.values()) {
                state.addResource(p, r, -state.getResource(p, r, STOREROOM));
            }
        }
    }

    @Test
    public void summerBidTiesForSecond() {
        advanceToSummer();
        emptyAllStores();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        for (int p = 0; p < state.getNPlayers(); p++) {
            state.addResource(p, BEER, 10);
            state.addResource(p, MEAD, 10);
        }

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, new SummerBid(2, 0));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new SummerBid(0, 0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void pray() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        state.addResource(state.getCurrentPlayer(), PRAYER, 1); // add Prayer token

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new Pray(2));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }


    @Test
    public void writePsalm() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        state.addActionPoints(10);
        IlluminatedText psalm = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Psalm")).findFirst().orElseThrow(
                () -> new AssertionError("Psalm not found")
        );
        WriteText action = new WriteText(psalm, 4);

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        int player = state.getCurrentPlayer();
        state.addResource(player, VELLUM, 2);
        state.addResource(player, CANDLE, 2);
        state.addResource(player, PALE_RED_INK, 2);

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, action);

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }




    @Test
    public void gatehouseActionsCorrect() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        state.acquireTreasure(cape, state.getCurrentPlayer());

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        state.addResource(state.getCurrentPlayer(), SHILLINGS, 8);

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void buyTreasure() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        int player = state.getCurrentPlayer();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        state.addActionPoints(3);
        state.addResource(player, SHILLINGS, 4);

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new BuyTreasure(robe));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

}

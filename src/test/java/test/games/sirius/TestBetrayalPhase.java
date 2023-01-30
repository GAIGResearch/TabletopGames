package test.games.sirius;

import core.*;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.GameType;
import games.sirius.*;
import games.sirius.actions.*;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.Draw;
import static org.junit.Assert.*;


public class TestBetrayalPhase {


    Game game;
    SiriusGameState state;
    SiriusForwardModel fm = new SiriusForwardModel();
    SiriusParameters params;
    List<AbstractPlayer> players = new ArrayList<>();

    @Before
    public void setup() {
        players = Arrays.asList(new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer());
        game = GameType.Sirius.createGameInstance(3, 34, new SiriusParameters());
        game.reset(players);
        state = (SiriusGameState) game.getGameState();
        params = (SiriusParameters) state.getGameParameters();
    }

    @Test
    public void smugglerCardsGiveOptionToSellThemInTwoDirections() {
        state.setGamePhase(Draw);
        assertEquals(TRADING, state.getMoon(0).getMoonType());
        assertEquals(OUTPOST, state.getMoon(3).getMoonType());
        state.movePlayerTo(0, 0);
        SiriusCard card1 = new SiriusCard("Smuggler1", SMUGGLER, 1);
        SiriusCard card2 = new SiriusCard("Smuggler2", SMUGGLER, 1);
        state.addCardToHand(0, card1);
        state.addCardToHand(0, card2);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(5, actions.size());
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card1))));
        assertTrue(actions.contains(new SellCards(Arrays.asList(card1, card2))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card1), true)));
        assertTrue(actions.contains(new SellCards(Arrays.asList(card1, card2), true)));
    }

    @Test
    public void afterSellingOneCanStillBetray() {
        state.setGamePhase(Draw);
        state.movePlayerTo(0, 0);
        SiriusCard card1 = new SiriusCard("Smuggler1", SMUGGLER, 1);
        SiriusCard card2 = new SiriusCard("Ammonia1", AMMONIA, 1);
        state.addCardToHand(0, card1);
        state.addCardToHand(0, card2);
        fm.next(state, new SellCards(Collections.singletonList(card2)));
        while (!(state.getCurrentPlayer() == 0)) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(Draw, state.getGamePhase());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card1))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card1), true)));
    }

    @Test
    public void afterBetrayingOneCanStillSell() {
        state.setGamePhase(Draw);
        state.movePlayerTo(0, 0);
        SiriusCard card1 = new SiriusCard("Smuggler1", SMUGGLER, 1);
        SiriusCard card2 = new SiriusCard("Ammonia1", AMMONIA, 1);
        state.addCardToHand(0, card1);
        state.addCardToHand(0, card2);
        fm.next(state, new SellCards(Collections.singletonList(card1)));
        while (!(state.getCurrentPlayer() == 0)) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(Draw, state.getGamePhase());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card2))));
    }


    @Test
    public void sellingSmugglerCardsMovesCorruptionTrackUp() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        for (int i = 0; i < 2; i++)
            state.addCardToHand(0, state.drawFromDeck(SMUGGLER, false));
        assertEquals(params.startingCorruption, state.getTrackPosition(SMUGGLER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        AbstractAction sellOne = actions.stream().
                filter(a -> a instanceof SellCards &&
                        ((SellCards) a).salesType == SMUGGLER &&
                        ((SellCards) a).getTotalValue() == 1).findFirst().orElseThrow(() -> new AssertionError("No matching action"));
        fm.next(state, sellOne);
        assertEquals(params.startingCorruption + 1, state.getTrackPosition(SMUGGLER));
        assertEquals(Draw, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void sellingSmugglerCardsMovesCorruptionTrackDown() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        for (int i = 0; i < 2; i++)
            state.addCardToHand(0, state.drawFromDeck(SMUGGLER, false));
        assertEquals(params.startingCorruption, state.getTrackPosition(SMUGGLER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        AbstractAction sellOne = actions.stream().
                filter(a -> a instanceof SellCards &&
                        ((SellCards) a).salesType == SMUGGLER &&
                        ((SellCards) a).getTotalValue() == -1).findFirst().orElseThrow(() -> new AssertionError("No matching action"));
        fm.next(state, sellOne);
        assertEquals(params.startingCorruption - 1, state.getTrackPosition(SMUGGLER));
        assertEquals(Draw, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void corruptionTrackHasCeilingAtMaximum() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        for (int i = 0; i < 4; i++)
            state.addCardToHand(0, state.drawFromDeck(SMUGGLER, false));
        assertEquals(params.startingCorruption, state.getTrackPosition(SMUGGLER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        AbstractAction sellFour = actions.stream().
                filter(a -> a instanceof SellCards &&
                        ((SellCards) a).salesType == SMUGGLER &&
                        ((SellCards) a).getTotalValue() == 4).findFirst().orElseThrow(() -> new AssertionError("No matching action"));
        fm.next(state, sellFour);
        assertEquals(params.corruptionTrack.length - 1, state.getTrackPosition(SMUGGLER));
        assertEquals(Draw, state.getGamePhase());
    }


    @Test
    public void movingCorruptionTrackToMarkerTriggersPolicePhase() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        for (int i = 0; i < 4; i++)
            state.addCardToHand(0, state.drawFromDeck(SMUGGLER, false));
        assertEquals(14, state.getTrackPosition(SMUGGLER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        AbstractAction sellTwo = actions.stream().
                filter(a -> a instanceof SellCards &&
                        ((SellCards) a).salesType == SMUGGLER &&
                        ((SellCards) a).getTotalValue() == -2).findFirst().orElseThrow(() -> new AssertionError("No matching action"));
        fm.next(state, sellTwo);
        assertEquals(12, state.getTrackPosition(SMUGGLER));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(sellTwo, state.currentActionInProgress());
    }

    @Test
    public void corruptionTrackEndsGame() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        SiriusCard superSmuggler = new SiriusCard("SuperSmuggler", SMUGGLER, params.startingCorruption);
        state.addCardToHand(0, superSmuggler);
        SellCards endGameAction = new SellCards(Collections.singletonList(superSmuggler), true);
        assertEquals(params.startingCorruption, state.getTrackPosition(SMUGGLER));
        fm.next(state, endGameAction);
        assertEquals(0, state.getTrackPosition(SMUGGLER));
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.WIN, state.getPlayerResults()[0]);
    }

    @Test
    public void movePoliceAction() {
        MovePolice mp = new MovePolice(2);
        state.getMoon(3).setCartelOwner(1);
        for (int i = 0; i <= 4; i++)
            assertFalse(state.getMoon(i).getPolicePresence());
        mp.execute(state);
        assertEquals(1, state.getMoon(3).getCartelOwner());
        for (int i = 0; i <= 4; i++)
            if (i == 2) {
                assertTrue(state.getMoon(i).getPolicePresence());
                assertEquals(-1, state.getMoon(i).getCartelOwner());
            } else
                assertFalse(state.getMoon(i).getPolicePresence());

        MovePolice mp2 = new MovePolice(3);
        mp2.execute(state);
        for (int i = 0; i <= 4; i++)
            if (i == 3) {
                assertTrue(state.getMoon(i).getPolicePresence());
                assertEquals(-1, state.getMoon(i).getCartelOwner());
            } else
                assertFalse(state.getMoon(i).getPolicePresence());
    }


    @Test
    public void stealCardAction() {
        // Add two cards from each moon to p1
        for (Moon m : state.getAllMoons()) {
            if (m.getMoonType() == TRADING)
                continue;
            state.addCardToHand(1, m.drawCard());
            state.addCardToHand(1, m.drawCard());
        }
        SiriusCard card = state.getPlayerHand(1).get(3); // the first two are Favour Cards, which we cannot steal
        StealCard sc = new StealCard(card, 1);
        assertEquals(8, state.getPlayerHand(1).getSize());
        assertEquals(0, state.getPlayerHand(0).getSize());
        sc.execute(state);
        assertEquals(6, state.getPlayerHand(1).getSize());
        assertEquals(1, state.getPlayerHand(0).getSize());
        assertEquals(card, state.getPlayerHand(0).get(0));
        assertTrue(state.getPlayerHand(1).stream().noneMatch(c -> c.cardType == card.cardType));
        for (SiriusConstants.SiriusCardType ct : SiriusConstants.SiriusCardType.values()) {
            if (ct == FAVOUR)
                continue;
            int expected = ct == card.cardType ? 1 : 0;
            assertEquals(expected, state.getDeckSize(ct, true));
        }
    }

    @Test
    public void afterStealingACardWeReturnToMainFM() {
        state.movePlayerTo(1, 2);
        state.addCardToHand(1, state.drawFromDeck(AMMONIA, false));
        state.setGamePhase(Draw);
        MovePolice mp = new MovePolice(2);
        fm.next(state, mp);
        assertTrue(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void movePoliceActionComputation() {
        movingCorruptionTrackToMarkerTriggersPolicePhase();
        // This moves us into a move police phase
        // We can move to any Moon except the one we are at
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(4, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof MovePolice));
        state.getMoon(2).setPolicePresence();
        actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof MovePolice));
    }

    private void stealCardActionCommonSetup(int policeRaid) {
        movingCorruptionTrackToMarkerTriggersPolicePhase();
        // at this point p1 is at moon 1, and p2 is at moon 4
        MovePolice mp = new MovePolice(policeRaid);
        state.addCardToHand(1, new SiriusCard("Ammonia", AMMONIA, 1));
        state.addCardToHand(1, new SiriusCard("Ammonia", AMMONIA, 1));
        state.addCardToHand(1, new SiriusCard("SuperAmmonia", AMMONIA, 2));
        state.addCardToHand(1, new SiriusCard("Contraband", CONTRABAND, 1));
        state.addCardToHand(1, new SiriusCard("Favour", FAVOUR, 1));
        state.addCardToHand(2, new SiriusCard("Favour", FAVOUR, 1));
        fm.next(state, mp);
    }

    @Test
    public void stealCardActionComputationI() {
        stealCardActionCommonSetup(1);
        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size()); // only two valid cards to steal
        assertTrue(actions.contains(new StealCard(new SiriusCard("Ammonia", AMMONIA, 1), 1)));
        assertTrue(actions.contains(new StealCard(new SiriusCard("Ammonia", AMMONIA, 2), 1)));
        assertTrue(actions.contains(new StealCard(new SiriusCard("Contraband", CONTRABAND, 1), 1)));
    }

    @Test
    public void stealCardActionComputationII() {
        stealCardActionCommonSetup(2);
        assertEquals(1, state.getCurrentPlayer()); // nothing to do, so we have moved to next player
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size()); // only two valid cards to steal
        assertEquals(new TakeCard(), actions.get(0));
    }

    @Test
    public void stealCardActionComputationIII() {
        stealCardActionCommonSetup(4);
        assertEquals(0, state.getCurrentPlayer()); // nothing to do, so we have moved to next player
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size()); // only two valid cards to steal
        assertEquals(new DoNothing(), actions.get(0));
    }

    @Test
    public void policePresenceBlocksOneCard() {
        assertEquals(2, state.getMoon(1).getDeckSize());
        state.getMoon(1).setPolicePresence();
        assertEquals(1, state.getMoon(1).getDeckSize());
        state.getMoon(1).removePolicePresence();
        assertEquals(2, state.getMoon(1).getDeckSize());

        state.getMoon(1).setPolicePresence();
        state.getMoon(1).drawCard();
        SiriusCard card = state.getMoon(1).drawCard();
        assertNull(card);

        state.getMoon(1).removePolicePresence();
        card = state.getMoon(1).drawCard();
        assertNotNull(card);
    }

    @Test
    public void policePresenceBlocksMetropolis() {
        Moon metropolis = state.getMoon(4);
        assertEquals(METROPOLIS, metropolis.getMoonType());
        assertEquals(0, metropolis.getDeckSize());
        SiriusCard card = metropolis.drawCard();
        assertEquals(FAVOUR, card.cardType);
        metropolis.setPolicePresence();
        assertEquals(0, metropolis.getDeckSize());
        card = metropolis.drawCard();
        assertNull(card);
    }

    @Test
    public void exhaustingPileReshufflesDiscardDeck() {
        int startingAmmoniaCards = state.getDeckSize(AMMONIA, false);
        assertEquals(0, state.getDeckSize(AMMONIA, true));
        // a reshuffle of the discard deck should be triggered as soon as we empty the deck
        for (int i = 0; i < startingAmmoniaCards; i++) {
            state.addToDeck(AMMONIA, true, Collections.singletonList(state.drawFromDeck(AMMONIA, false)));
            if (i == startingAmmoniaCards - 1) {
                assertEquals(1, state.getDeckSize(AMMONIA, true));
                assertEquals(startingAmmoniaCards - 1, state.getDeckSize(AMMONIA, false));
            } else {
                assertEquals(i + 1, state.getDeckSize(AMMONIA, true));
                assertEquals(startingAmmoniaCards - i - 1, state.getDeckSize(AMMONIA, false));
            }
        }
    }

}

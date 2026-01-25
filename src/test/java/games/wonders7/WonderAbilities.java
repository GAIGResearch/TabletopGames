package games.wonders7;

import core.actions.AbstractAction;
import games.wonders7.actions.BuildFromDiscard;
import games.wonders7.actions.ChooseCard;
import games.wonders7.actions.PlayCard;
import games.wonders7.cards.Wonder7Board;
import games.wonders7.cards.Wonder7Card;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static games.wonders7.cards.Wonder7Card.CardType.*;
import static games.wonders7.cards.Wonder7Card.CardType.Temple;
import static org.junit.Assert.*;

public class WonderAbilities {

    Wonders7ForwardModel fm = new Wonders7ForwardModel();
    Wonders7GameParameters params;
    Wonders7GameState state;
    Random rnd = new Random(309842);

    @Before
    public void setup() {
        params = new Wonders7GameParameters();
        params.setRandomSeed(4902);
        state = new Wonders7GameState(params, 4);
        fm.setup(state);
    }

    @Test
    public void olympiaLevel2BuildsFirstCardOfColourForFree() {
        state.playerWonderBoard[0] = new Wonder7Board(Wonder7Board.Wonder.TheStatueOfZeusInOlympia, 0);
        state.getPlayerHand(0).add(Wonder7Card.factory(Palace, state.getParams()));
        state.getPlayerHand(0).add(Wonder7Card.factory(University, state.getParams()));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, false))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, false))));

        state.getPlayerWonderBoard(0).changeStage();
        actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, true))));

        state.getPlayerWonderBoard(0).changeStage();
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new ChooseCard(new PlayCard(0, Palace, true))));
        assertTrue(actions.contains(new ChooseCard(new PlayCard(0, University, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, false))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, false))));

        state.getPlayedCards(0).add(Wonder7Card.factory(Altar, state.getParams()));
        actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, true))));
        assertTrue(actions.contains(new ChooseCard(new PlayCard(0, University, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, false))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, false))));
    }

    @Test
    public void halicarnassusLevel2BuildsForFreeFromDiscardPileAtEndOfTurn() {
        state.playerWonderBoard[1] = new Wonder7Board(Wonder7Board.Wonder.TheMausoleumOfHalicarnassus, 0);
        // at the end of the Age (and only then), if the second stage is built then the player can build a card from the discard pile for free
        state.getDiscardPile().add(Wonder7Card.factory(Arena, state.getParams()));
        for (int i = 0; i < 7; i++) { // take 7 actions (2 turns minus 1)
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            int index = rnd.nextInt(actions.size());
            fm.next(state, actions.get(index));
        }
        state.playerWonderBoard[1].changeStage();  // stage one; should not do anything
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        fm.next(state, actions.get(0));
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());

        state.playerWonderBoard[1].changeStage();  // stage two; should allow building from discard pile
        for (int i = 0; i < 4; i++) { // four actions
            actions = fm.computeAvailableActions(state);
            int index = rnd.nextInt(actions.size());
            fm.next(state, actions.get(index));
        }
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.currentActionInProgress() instanceof BuildFromDiscard);
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().allMatch(a -> a instanceof PlayCard));
        int discardSize = state.getDiscardPile().getSize();
        fm.next(state, actions.get(0));
        assertEquals(discardSize - 1, state.getDiscardPile().getSize());
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.playerWonderBoard[1].effectUsed);

        // now check we do not build another one
        for (int i = 0; i < 8; i++) { // four actions
            actions = fm.computeAvailableActions(state);
            int index = rnd.nextInt(actions.size());
            fm.next(state, actions.get(index));
            assertFalse(state.isActionInProgress());
            assertTrue(state.playerWonderBoard[1].effectUsed);
        }
        // then build next stage, and check we do not trigger the earlier one
        state.playerWonderBoard[1].changeStage();
        assertFalse(state.playerWonderBoard[1].effectUsed);
        for (int i = 0; i < 8; i++) { // four actions
            actions = fm.computeAvailableActions(state);
            int index = rnd.nextInt(actions.size());
            fm.next(state, actions.get(index));
            assertFalse(state.isActionInProgress());
        }
    }

    @Test
    public void halicarnassusNightSideResetsEffectUsed() {
        state.playerWonderBoard[1] = new Wonder7Board(Wonder7Board.Wonder.TheMausoleumOfHalicarnassus, 1);
        state.getDiscardPile().add(Wonder7Card.factory(Arena, state.getParams()));
        for (int i = 0; i < 7; i++) { // take 7 actions (2 turns minus 1)
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            int index = rnd.nextInt(actions.size());
            fm.next(state, actions.get(index));
        }
        state.playerWonderBoard[1].changeStage();  // stage one;  should allow building from discard pile
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.currentActionInProgress() instanceof BuildFromDiscard);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().allMatch(a -> a instanceof PlayCard));
        int discardSize = state.getDiscardPile().getSize();
        fm.next(state, actions.get(0));
        assertEquals(discardSize - 1, state.getDiscardPile().getSize());
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.playerWonderBoard[1].effectUsed);

        for (int i = 0; i < 8; i++) { // 8 actions
            actions = fm.computeAvailableActions(state);
            int index = rnd.nextInt(actions.size());
            fm.next(state, actions.get(index));
            assertFalse(state.isActionInProgress());
        }
        assertTrue(state.playerWonderBoard[1].effectUsed);
        state.playerWonderBoard[1].changeStage();  // stage two; should allow building from discard pile
        assertFalse(state.playerWonderBoard[1].effectUsed);

        // now check we build another one (after the four actions to play cards)
        for (int i = 0; i < 4; i++) { // four actions
            assertFalse(state.isActionInProgress());
            actions = fm.computeAvailableActions(state);
            int index = rnd.nextInt(actions.size());
            fm.next(state, actions.get(index));
        }
        assertTrue(state.isActionInProgress());
        assertTrue(state.playerWonderBoard[1].effectUsed); // this is set once BuildToDiscard is added to stack
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.currentActionInProgress() instanceof BuildFromDiscard);
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().allMatch(a -> a instanceof PlayCard));
    }

    @Test
    public void olympiaNightSideFirstCardOfAge() {
        state.playerWonderBoard[2] = new Wonder7Board(Wonder7Board.Wonder.TheStatueOfZeusInOlympia, 1);
        // We build the first stage of this wonder, then check that with random play, on the first card built
        // by player 2 for each Age, they can build all cards in their hand for free

        // This does not apply to other players; or on the second action [we add an impossibly expensive card to the hand at the start of each age]
        int checks = 0;
        do {
            if (state.getRoundCounter() % 6 == 0 && state.getCurrentPlayer() == 0 && !state.isActionInProgress()) {
                // before the first action of each Age
                assertEquals(7, state.playerHands.get(2).getSize());
                for (int p = 0; p < 4; p++) {
                    state.getPlayerHand(p).remove(6);  // remove random card, add expensive one
                    state.getPlayerHand(p).add(Wonder7Card.factory(Palace, state.getParams()));
                }
            }
            // we build the wonder during the first Age (so we can check it does not apply for the first card)
            if (state.getRoundCounter() > 2 && state.getPlayerWonderBoard(2).nextStageToBuild() == 1)
                state.getPlayerWonderBoard(2).changeStage();
            List<AbstractAction> availableActions = fm.computeAvailableActions(state);
            if (state.getRoundCounter() % 6 == 0 && !state.isActionInProgress()) {
                // before the first action of each Age
                assertTrue(availableActions.stream().allMatch(a -> a instanceof ChooseCard));
                //          System.out.println("Player " + state.getCurrentPlayer() + " has " + availableActions.size() + " actions");
                assertEquals(state.getCurrentPlayer() == 2 && state.getRoundCounter() > 2,
                        availableActions.contains(new ChooseCard(new PlayCard(state.getCurrentPlayer(), Palace, true))));
                checks++;
            }

            fm.next(state, availableActions.get(state.getRnd().nextInt(availableActions.size())));
        } while (state.isNotTerminal());

        assertEquals(12, checks);
    }

    @Test
    public void olympiaNightSideLastCardOfAge() {
        state.playerWonderBoard[2] = new Wonder7Board(Wonder7Board.Wonder.TheStatueOfZeusInOlympia, 1);
        // We build the first stage of this wonder, then check that with random play, on the first card built
        // by player 2 for each Age, they can build all cards in their hand for free
        state.playerWonderBoard[2].changeStage();

        // This does not apply to other players; or on the second action [we add an impossibly expensive card to the hand at the start of each age]
        int checks = 0;
        do {
            if (state.getRoundCounter() % 6 == 5 && state.getCurrentPlayer() == 0 && !state.isActionInProgress()) {
                // before the last action of each Age
                for (int p = 0; p < 4; p++) {
                    state.getPlayerHand(p).remove(1);  // remove random card, add expensive one
                    state.getPlayerHand(p).add(Wonder7Card.factory(Palace, state.getParams()));
                }
            }
            // we build the wonder during the second Age (so we can check it does not apply for the first card)
            if (state.getRoundCounter() > 9 && state.getPlayerWonderBoard(2).nextStageToBuild() == 2)
                state.getPlayerWonderBoard(2).changeStage();
            List<AbstractAction> availableActions = fm.computeAvailableActions(state);
            if (state.getRoundCounter() % 6 == 5 && !state.isActionInProgress()) {
                // for the last card of the age
                assertTrue(availableActions.stream().allMatch(a -> a instanceof ChooseCard));
                System.out.println("Player " + state.getCurrentPlayer() + " has " + availableActions.size() + " actions");
                assertEquals(state.getCurrentPlayer() == 2 && state.getRoundCounter() > 9,
                        availableActions.contains(new ChooseCard(new PlayCard(state.getCurrentPlayer(), Palace, true))));
                checks++;
            }

            // we play the last card to avoid ever building the palace
            fm.next(state, availableActions.get(availableActions.size() - 1));
        } while (state.isNotTerminal());

        assertEquals(12, checks);
    }

    @Test
    public void babylonNightSideBuildFinalDiscardForFree() {
        state.playerWonderBoard[0] = new Wonder7Board(Wonder7Board.Wonder.TheHangingGardensOfBabylon, 1);
        // We build the first stage of this wonder during Age 2. We check that in Age 1 the player does not build a card for free
        // but does build a free extra card in the other two ages.
        // We also check that all other players do not get this benefit.

        int checks = 0;
        do {
            // we build the wonder during the second Age (so we can check it does not apply for the first age)
            if (state.getRoundCounter() > 9 && state.getPlayerWonderBoard(0).nextStageToBuild() == 1)
                state.getPlayerWonderBoard(0).changeStage();

            for (int i = 0; i < 4; i++) {
                List<AbstractAction> availableActions = fm.computeAvailableActions(state);
                fm.next(state, availableActions.get(availableActions.size() - 1));  // will always be a Discard - so nothing built outside of wonder
            }

            if (state.getPlayerHand(0).getSize() == 0 || state.getPlayerHand(0).getSize() == 7) {
                checks++;
                for (int p = 0; p < 4; p++)
                    assertEquals( p == 0 ? state.getCurrentAge() - 2 : 0, state.getPlayedCards(p).getSize());
            }
        } while (state.isNotTerminal());

        assertEquals(3, checks);
    }

    @Test
    public void halicarnassusOnLastRoundOfAge() {
        // we trigger Halicarnassus on the last round of an age...and check the age still ends correctly
        state.playerWonderBoard[3] = new Wonder7Board(Wonder7Board.Wonder.TheMausoleumOfHalicarnassus, 1);
        for (int i = 0; i < 20; i++) { // take 20 actions (6 rounds)
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            int index = rnd.nextInt(actions.size());
            fm.next(state, actions.get(index));
        }
        assertEquals(1, state.getCurrentAge());
        for (int p = 0; p < 4; p++) {
            assertEquals(2, state.getPlayerHand(p).getSize()); // for the last round
        }
        state.playerWonderBoard[3].changeStage();  // stage one;  should allow building from discard pile

        for (int i = 0; i < 4; i++) { // take 4 actions
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            int index = rnd.nextInt(actions.size());
            fm.next(state, actions.get(index));
        }
        assertTrue(state.isActionInProgress());
        assertEquals(3, state.getCurrentPlayer());
        assertTrue(state.currentActionInProgress() instanceof BuildFromDiscard);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        fm.next(state, actions.get(0));

        assertEquals(2, state.getCurrentAge());
        for (int p = 0; p < 4; p++) {
            assertEquals(7, state.getPlayerHand(p).getSize());
        }

    }

}

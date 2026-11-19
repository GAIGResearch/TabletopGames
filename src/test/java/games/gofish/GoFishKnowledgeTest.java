package games.gofish;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.GameType;
import games.gofish.actions.GoFishAsk;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import utilities.JSONUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static games.gofish.GoFishKnowledge.*;
import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * Why each shown card was shown, and how an agent that uses only some kinds of public knowledge
 * ({@link GoFishKnowledge}) forgets the rest in its redeterminised copies. Three players; P0 to play.
 */
public class GoFishKnowledgeTest {

    GoFishGameState state;
    GoFishForwardModel fm;

    @Before
    public void setup() {
        state = newState(new GoFishParameters(), 3, 17);
        fm = new GoFishForwardModel();
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1, card("5S"), card("5C"), card("9D"));
        giveHand(state, 2, card("7H"), card("2C"));
        stackDrawDeck(state, card("8D"));
    }

    private FrenchCard inHand(GoFishGameState s, int p, String code) {
        return s.playerHands.get(p).get(indexInHand(s, p, card(code)));
    }

    private EnumSet<GoFishKnowledge> why(GoFishGameState s, int p, String code) {
        return s.getShownBecause(inHand(s, p, code));
    }

    /**
     * P0 asks P1 for 5s and is handed the 5S and 5C: P0's 5H is shown as the asker's card, the 5S and 5C as cards
     * fished for. P0 holds 5H 5S 5C KD.
     */
    private void afterASuccessfulAsk() {
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(cards("5H", "5S", "5C", "KD"), cardSet(state.playerHands.get(0)));
    }

    // ---- recording why a card is shown ----

    @Test
    public void aSuccessfulAskRecordsWhyEachCardIsShown() {
        afterASuccessfulAsk();
        assertEquals(EnumSet.of(ASKERS_CARD), why(state, 0, "5H"));
        assertEquals(EnumSet.of(CARDS_FISHED_FOR), why(state, 0, "5S"));
        assertEquals(EnumSet.of(CARDS_FISHED_FOR), why(state, 0, "5C"));
        assertTrue(why(state, 0, "KD").isEmpty());
    }

    @Test
    public void aDrawnCardShownToTakeAnotherTurnIsRecordedAsFishedFor() {
        stackDrawDeck(state, card("5D"));
        fm.next(state, new GoFishAsk(2, 5));     // Go fish, and P0 draws the 5D
        assertVisibleToAll(state, 0, card("5D"));
        assertEquals(EnumSet.of(CARDS_FISHED_FOR), why(state, 0, "5D"));
        assertEquals(EnumSet.of(ASKERS_CARD), why(state, 0, "5H"));
    }

    @Test
    public void aDrawnCardOfAnotherRankIsNotRecorded() {
        fm.next(state, new GoFishAsk(2, 5));     // Go fish, and P0 draws the 8D
        assertTrue(why(state, 0, "8D").isEmpty());
    }

    @Test
    public void anAskTakesACardAlreadyShownAsTheAskersCardAndShowsNoOther() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        showToAll(state, 0, card("5D"));
        state.recordShown(inHand(state, 0, "5D"), CARDS_FISHED_FOR);
        fm.next(state, new GoFishAsk(2, 5));     // Go fish
        assertEquals(EnumSet.of(CARDS_FISHED_FOR, ASKERS_CARD), why(state, 0, "5D"));
        assertVisibleOnlyToOwner(state, 0, card("5H"));
        assertTrue(why(state, 0, "5H").isEmpty());
    }

    @Test
    public void askingForTheSameRankAgainShowsNothingMore() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        new GoFishAsk(2, 5).execute(state);      // Go fish, drawing the 8D; P0 still the current player
        new GoFishAsk(2, 5).execute(state);
        assertEquals(1, visibleToAllOfRank(state, 0, 5));
    }

    @Test
    public void cardsHandedOverForgetWhyTheyWereShownBefore() {
        // P1's 5S was shown as P1's own card when P1 asked for 5s; handed to P0 it is only a card fished for
        showToAll(state, 1, card("5S"));
        state.recordShown(inHand(state, 1, "5S"), ASKERS_CARD);
        afterASuccessfulAsk();
        assertEquals(EnumSet.of(CARDS_FISHED_FOR), why(state, 0, "5S"));
    }

    @Test
    public void layingDownABookForgetsWhyItsCardsWereShown() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        fm.next(state, new GoFishAsk(1, 5));     // the 5S and 5C complete the book
        assertEquals(1, state.getBooks(0));
        for (FrenchCard c : state.playerBooks.get(0).getComponents())
            assertTrue(c + " is in a book", state.getShownBecause(c).isEmpty());
    }

    // ---- full knowledge: nothing changes ----

    @Test
    public void theGameStateUsesAllKnowledge() {
        assertEquals(GoFishKnowledge.all(), state.getKnowledge());
        assertEquals(GoFishKnowledge.all(), ((GoFishGameState) state.copy(1)).getKnowledge());
    }

    @Test
    public void restrictingToAllKnowledgeChangesNothing() {
        afterASuccessfulAsk();
        GoFishGameState before = (GoFishGameState) state.copy();
        state.restrictKnowledge(GoFishKnowledge.all(), 1);
        assertEquals(before, state);
    }

    @Test
    public void knowledgeOnlyNarrows() {
        GoFishGameState obs = (GoFishGameState) state.copy(1);
        obs.restrictKnowledge(of(false, true, true), 1);
        obs.restrictKnowledge(GoFishKnowledge.all(), 1);
        assertEquals(of(false, true, true), obs.getKnowledge());
        obs.restrictKnowledge(of(true, false, true), 1);
        assertEquals(EnumSet.of(ASKERS_CARD), obs.getKnowledge());
    }

    @Test
    public void restrictingAnObservationLeavesTheGameStateAlone() {
        afterASuccessfulAsk();
        GoFishGameState before = (GoFishGameState) state.copy();
        GoFishGameState obs = (GoFishGameState) state.copy(1);
        obs.restrictKnowledge(EnumSet.noneOf(GoFishKnowledge.class), 1);
        assertEquals(before, state);
        assertEquals(3, visibleToAllOfRank(state, 0, 5));
    }

    @Test
    public void copiesKeepTheRestrictedKnowledge() {
        GoFishGameState obs = (GoFishGameState) state.copy(1);
        obs.restrictKnowledge(of(true, false, true), 1);
        assertEquals(of(true, false, true), ((GoFishGameState) obs.copy()).getKnowledge());
        assertEquals(of(true, false, true), ((GoFishGameState) obs.copy(2)).getKnowledge());
    }

    // ---- forgetting each kind of knowledge ----

    @Test
    public void withoutCardsFishedForTheCardsHandedOverAreForgotten() {
        afterASuccessfulAsk();
        GoFishGameState obs = (GoFishGameState) state.copy(1);
        obs.restrictKnowledge(of(false, true, true), 1);
        // only the asker's own 5 is still known to P1
        assertEquals(1, visibleToAllOfRank(obs, 0, 5));
        assertVisibleToAll(obs, 0, card("5H"));
        assertEquals(EnumSet.of(ASKERS_CARD), why(obs, 0, "5H"));
        assertEquals(4, obs.playerHands.get(0).getSize());
        // and P0's three other cards are drawn afresh from those P1 cannot see
        boolean moved = false;
        for (int i = 0; i < 50 && !moved; i++) {
            GoFishGameState copy = (GoFishGameState) obs.copy(1);
            assertEquals(1, visibleToAllOfRank(copy, 0, 5));
            moved = !cardSet(copy.playerHands.get(0)).containsAll(cards("5S", "5C"));
        }
        assertTrue("the 5S and 5C leave P0's hand in some copy", moved);
    }

    @Test
    public void withoutTheAskersCardTheAskersOwnCardIsForgotten() {
        afterASuccessfulAsk();
        GoFishGameState obs = (GoFishGameState) state.copy(1);
        obs.restrictKnowledge(of(true, true, false), 1);
        assertEquals(2, visibleToAllOfRank(obs, 0, 5));
        assertVisibleToAll(obs, 0, card("5S"));
        assertVisibleToAll(obs, 0, card("5C"));
        boolean moved = false;
        for (int i = 0; i < 50 && !moved; i++) {
            GoFishGameState copy = (GoFishGameState) obs.copy(1);
            moved = !cardSet(copy.playerHands.get(0)).contains(card("5H"));
        }
        assertTrue("the 5H leaves P0's hand in some copy", moved);
    }

    @Test
    public void aCardShownForAReasonStillKnownStaysShown() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        showToAll(state, 0, card("5D"));
        state.recordShown(inHand(state, 0, "5D"), CARDS_FISHED_FOR);
        fm.next(state, new GoFishAsk(2, 5));     // the 5D is now shown for both reasons
        GoFishGameState obs = (GoFishGameState) state.copy(1);
        obs.restrictKnowledge(of(true, true, false), 1);
        assertVisibleToAll(obs, 0, card("5D"));
        assertEquals(EnumSet.of(CARDS_FISHED_FOR), why(obs, 0, "5D"));
    }

    @Test
    public void aPlayerAlwaysKnowsTheirOwnHand() {
        afterASuccessfulAsk();
        GoFishGameState obs = (GoFishGameState) state.copy(0);
        obs.restrictKnowledge(EnumSet.noneOf(GoFishKnowledge.class), 0);
        assertEquals(state.playerHands.get(0).getComponents(), obs.playerHands.get(0).getComponents());
        for (int i = 0; i < obs.playerHands.get(0).getSize(); i++)
            assertTrue(obs.playerHands.get(0).isVisibleTo(i, 0));
        assertEquals(0, visibleToAllOfRank(obs, 0, 5));
    }

    @Test
    public void withoutVoidsACopyMayGiveAPlayerARankTheyAreKnownVoidIn() {
        // as GoFishCopyTest: P1 is known void in every rank 2-10, and with voids known is never dealt one
        giveHand(state, 1, card("JH"), card("QH"), card("KH"), card("AH"), card("JD"));
        for (int r = 2; r <= 10; r++) state.getKnownVoids().record(1, r);
        GoFishGameState obs = (GoFishGameState) state.copy(0);
        obs.restrictKnowledge(of(true, false, true), 0);
        boolean broken = false;
        for (int i = 0; i < 50 && !broken; i++) {
            GoFishGameState copy = (GoFishGameState) obs.copy(0);
            for (FrenchCard c : copy.playerHands.get(1).getComponents())
                broken |= c.number <= 10;
        }
        assertTrue("P1 is dealt a rank 2-10 in some copy", broken);
        // the observation forgets the voids, which the game state keeps
        assertTrue(obs.getKnownVoids().get(1).isEmpty());
        assertTrue(state.getKnownVoids().isVoid(1, 5));
    }

    @Test
    public void withoutVoidsAnAskInTheSearchRecordsNone() {
        GoFishGameState obs = (GoFishGameState) state.copy(0);
        obs.restrictKnowledge(of(true, false, true), 0);
        fm.next(obs, new GoFishAsk(2, 5));     // Go fish: P2 is seen to hold no 5
        assertTrue(obs.getKnownVoids().get(2).isEmpty());
        fm.next(state, new GoFishAsk(2, 5));
        assertTrue(state.getKnownVoids().isVoid(2, 5));
    }

    @Test
    public void withoutCardsFishedForTheCardsHandedOverInTheSearchAreForgottenAtOnce() {
        GoFishGameState obs = (GoFishGameState) state.copy(1);
        obs.restrictKnowledge(of(false, true, true), 1);
        // in P1's search, P0 asks P1 for 5s
        GoFishGameState search = (GoFishGameState) obs.copy();
        giveHand(search, 0, card("5H"), card("KD"));
        giveHand(search, 1, card("5S"), card("5C"), card("9D"));
        fm.next(search, new GoFishAsk(1, 5));
        assertEquals(1, visibleToAllOfRank(search, 0, 5));
        assertVisibleToAll(search, 0, card("5H"));
        assertVisibleOnlyToOwner(search, 0, card("5S"));
        assertVisibleOnlyToOwner(search, 0, card("5C"));
    }

    // ---- the decorator ----

    @Test
    public void theDecoratorRestrictsTheObservationAndChangesNoActions() {
        afterASuccessfulAsk();
        GoFishGameState obs = (GoFishGameState) state.copy(0);
        List<AbstractAction> actions = fm.computeAvailableActions(obs);
        GoFishKnowledgeDecorator decorator = new GoFishKnowledgeDecorator(false, true, false);
        assertSame(actions, decorator.actionFilter(obs, actions));
        assertEquals(EnumSet.of(VOIDS), obs.getKnowledge());
        assertEquals(state.playerHands.get(0).getComponents(), obs.playerHands.get(0).getComponents());
    }

    @Test
    public void theDecoratorIsBuiltFromJSON() throws Exception {
        JSONObject json = (JSONObject) new JSONParser().parse(
                "{\"class\" : \"games.gofish.GoFishKnowledgeDecorator\", \"args\" : [true, false, true]}");
        GoFishKnowledgeDecorator decorator = JSONUtils.loadClassFromJSON(json);
        assertEquals(of(true, false, true), decorator.knowledge);
    }

    @Test
    public void gamesBetweenSearchAgentsWithDifferentKnowledgeRunToTheEnd() {
        boolean[][] flags = {{true, true, true}, {false, true, true}, {true, false, false}};
        for (long seed = 1; seed <= 3; seed++) {
            Game game = GameType.GoFish.createGameInstance(3, seed, new GoFishParameters());
            List<AbstractPlayer> players = new ArrayList<>();
            for (boolean[] f : flags) {
                MCTSParams params = new MCTSParams();
                params.setParameterValue("budget", 100);
                params.setRandomSeed((int) seed);
                MCTSPlayer player = new MCTSPlayer(params);
                player.addDecorator(new GoFishKnowledgeDecorator(f[0], f[1], f[2]));
                players.add(player);
            }
            game.reset(players);
            game.run();
            GoFishGameState end = (GoFishGameState) game.getGameState();
            assertFalse("game " + seed + " ends", end.isNotTerminal());
            assertEquals("the game state keeps all knowledge", GoFishKnowledge.all(), end.getKnowledge());
            assertAllCardsPresent(end);
        }
    }
}

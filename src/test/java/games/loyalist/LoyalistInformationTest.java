package games.loyalist;

import static games.loyalist.LoyalistGameState.Phase;
import static games.loyalist.LoyalistGameState.Role;

import static org.junit.Assert.*;

import core.Game;
import core.actions.AbstractAction;

import games.GameType;
import games.loyalist.actions.LoyalistAction;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Checks the state/action boundary used by core.Game, not just rendering visibility. */
public class LoyalistInformationTest {
    private final LoyalistForwardModel model = new LoyalistForwardModel();

    private LoyalistGameState fixture(Phase phase, int actor) {
        Game game = GameType.Loyalist.createGameInstance(5, 78123L);
        LoyalistGameState state = (LoyalistGameState) game.getGameState();
        state.roles = new Role[] {Role.KING, Role.LOYALIST, Role.MAGNATE, Role.MAGNATE, Role.SPY};
        state.offices = new int[] {1, 2, 3};
        state.treasury = new int[] {12, 6, 3};
        state.wealth = new int[] {0, 1, 1, 1, 1};
        state.reserve = 0;
        state.round = 2;
        state.publicTokenUpperBound = 25;
        state.eventId = 1;
        state.hands = LoyalistGameState.fiveLists();
        state.deck = LoyalistData.powerCards();
        state.dead.clear();
        state.bagCards.clear();
        state.bagTokens = 0;
        state.keepSelected.clear();
        state.privateLog = LoyalistGameState.fiveStringLists();
        state.publicLog.clear();
        state.knownBagCards = LoyalistGameState.fiveLists();
        state.knownDead = LoyalistGameState.fiveLists();
        Arrays.fill(state.liveBagTokens, false);
        Arrays.fill(state.liveBagCards, false);
        Arrays.fill(state.rememberedBagTokens, -1);
        Arrays.fill(state.rememberedBagCount, -1);
        Arrays.fill(state.exileThroughRound, 0);
        for (int player = 0; player < 5; player++) {
            Arrays.fill(state.knownTreasury[player], -1);
            Arrays.fill(state.knownWealth[player], -1);
            Arrays.fill(state.knownHandSum[player], Integer.MIN_VALUE);
        }
        setHand(state, 1, 3, 1);
        setHand(state, 2, -3, 2);
        setHand(state, 3, -2, 1);
        setHand(state, 4, -1, 2);
        state.refreshOwners();
        state.transition(phase, actor);
        return state;
    }

    private void setHand(LoyalistGameState state, int player, int... cards) {
        for (int card : cards) {
            assertTrue(state.deck.remove(Integer.valueOf(card)));
            state.hands.get(player).add(card);
        }
    }

    private void setBag(LoyalistGameState state, int tokens, int... cards) {
        state.bagTokens = tokens;
        state.treasury[0] -= tokens;
        state.treasuryChanged(0);
        for (int card : cards) {
            assertTrue(state.deck.remove(Integer.valueOf(card)));
            state.bagCards.add(card);
        }
    }

    private LoyalistGameState fullCopy(LoyalistGameState state) {
        return (LoyalistGameState) state.copy();
    }

    private LoyalistGameState observedCopy(LoyalistGameState state, int player, long seed) {
        state.setRedeterminisationSeed(seed);
        return (LoyalistGameState) state.copy(player);
    }

    private List<AbstractAction> actions(LoyalistGameState state) {
        return model.computeAvailableActions(state);
    }

    private void assertIndistinguishable(
            LoyalistGameState left, LoyalistGameState right, int player) {
        for (long seed = 0; seed < 12; seed++) {
            LoyalistGameState observedLeft = observedCopy(left, player, seed);
            LoyalistGameState observedRight = observedCopy(right, player, seed);
            assertEquals(
                    "Same information must produce the same sampled world at seed " + seed,
                    observedLeft,
                    observedRight);
            assertEquals(
                    "Agents receive actions computed on their sampled observation",
                    new HashSet<>(actions(observedLeft)),
                    new HashSet<>(actions(observedRight)));
            assertEquals(observedLeft.getHistory(), observedRight.getHistory());
            assertEquals(observedLeft.getHistoryAsText(), observedRight.getHistoryAsText());
            assertCardInventory(observedLeft);
            assertCardInventory(observedRight);
        }
    }

    private void assertCardInventory(LoyalistGameState state) {
        List<Integer> cards = new ArrayList<>(state.deck);
        cards.addAll(state.dead);
        cards.addAll(state.bagCards);
        cards.addAll(state.keepSelected);
        for (List<Integer> hand : state.hands) cards.addAll(hand);
        List<Integer> expected = LoyalistData.powerCards();
        Collections.sort(cards);
        Collections.sort(expected);
        assertEquals(
                "A determinization must retain the physical forty-card inventory", expected, cards);
    }

    @Test
    public void kingCannotDistinguishHiddenRoleAssignments() {
        LoyalistGameState first = fixture(Phase.EXILE, 0);
        LoyalistGameState second = fullCopy(first);
        Collections.swap(Arrays.asList(second.roles), 1, 4);
        assertIndistinguishable(first, second, 0);
    }

    @Test
    public void ministerCannotDistinguishOtherHiddenHands() {
        LoyalistGameState first = fixture(Phase.SUPPLY, 1);
        first.supplyResource = 0;
        setBag(first, 2, 0);
        first.peekBag(1, false);
        LoyalistGameState second = fullCopy(first);
        int card = second.hands.get(2).get(0);
        second.hands.get(2).set(0, second.hands.get(3).get(0));
        second.hands.get(3).set(0, card);
        assertIndistinguishable(first, second, 1);
    }

    @Test
    public void kingCannotDistinguishHiddenWealthOrTreasury() {
        LoyalistGameState first = fixture(Phase.CONVERT_W_M, 0);
        LoyalistGameState second = fullCopy(first);
        second.wealth[4] += 4;
        second.treasury[1] -= 4;
        second.wealthChanged(4);
        second.treasuryChanged(1);
        assertIndistinguishable(first, second, 0);
        assertEquals(
                "The king's request menu must not disclose actual treasury contents",
                new HashSet<>(actions(first)),
                new HashSet<>(actions(second)));
    }

    @Test
    public void ownFactsAndAuditedFactsSurviveRedeterminization() {
        LoyalistGameState state = fixture(Phase.SUPPLY, 2);
        state.supplyResource = 1;
        state.knownTreasury[2][0] = state.treasury[0];
        state.knownWealth[2][4] = state.wealth[4];
        state.knownHandSum[2][3] = state.hands.get(3).stream().mapToInt(Integer::intValue).sum();
        state.remember(2, "Inspector privately observed seat 4 wealth 1");
        for (int seed = 0; seed < 20; seed++) {
            LoyalistGameState copy = observedCopy(state, 2, seed);
            assertEquals(state.roles[2], copy.roles[2]);
            assertEquals(state.hands.get(2), copy.hands.get(2));
            assertEquals(state.wealth[2], copy.wealth[2]);
            assertEquals(state.treasury[1], copy.treasury[1]);
            assertEquals(state.treasury[0], copy.treasury[0]);
            assertEquals(state.wealth[4], copy.wealth[4]);
            assertEquals(
                    state.knownHandSum[2][3],
                    copy.hands.get(3).stream().mapToInt(Integer::intValue).sum());
            assertEquals(state.privateLog.get(2), copy.privateLog.get(2));
            assertCardInventory(copy);
        }
    }

    @Test
    public void magnatesDoNotReceiveTheOtherMagnatesIdentity() {
        LoyalistGameState state = fixture(Phase.PURCHASE, 2);
        Set<Integer> otherMagnateSeats = new HashSet<>();
        for (int seed = 0; seed < 48; seed++) {
            LoyalistGameState copy = observedCopy(state, 2, seed);
            assertEquals(Role.MAGNATE, copy.roles[2]);
            assertEquals(Role.KING, copy.roles[0]);
            assertEquals(1, Collections.frequency(Arrays.asList(copy.roles), Role.SPY));
            assertEquals(1, Collections.frequency(Arrays.asList(copy.roles), Role.LOYALIST));
            assertEquals(2, Collections.frequency(Arrays.asList(copy.roles), Role.MAGNATE));
            for (int player = 1; player < 5; player++) {
                if (player != 2 && copy.roles[player] == Role.MAGNATE)
                    otherMagnateSeats.add(player);
            }
        }
        assertEquals(Set.of(1, 3, 4), otherMagnateSeats);
    }

    @Test
    public void supplierKnowsBagCountsButNotCardValues() {
        LoyalistGameState first = fixture(Phase.SUPPLY, 1);
        first.supplyResource = 0;
        setBag(first, 3, -3);
        first.peekBag(1, false);
        LoyalistGameState second = fullCopy(first);
        assertTrue(second.deck.remove(Integer.valueOf(3)));
        second.deck.add(-3);
        second.bagCards.set(0, 3);
        assertIndistinguishable(first, second, 1);
        for (int seed = 0; seed < 12; seed++) {
            LoyalistGameState copy = observedCopy(first, 1, seed);
            assertEquals(3, copy.bagTokens);
            assertEquals(1, copy.bagCards.size());
            assertEquals(3, copy.rememberedBagTokens[1]);
            assertEquals(1, copy.rememberedBagCount[1]);
        }
    }

    @Test
    public void actingIntriguerKnowsCurrentBagAndHasTheSameLegalActions() {
        LoyalistGameState state = fixture(Phase.INTRIGUE_STEAL, 2);
        setBag(state, 5, -1, 2);
        state.embezzled = 2;
        state.peekBag(2, true);
        Set<AbstractAction> actualActions = new HashSet<>(actions(state));
        for (int seed = 0; seed < 12; seed++) {
            LoyalistGameState copy = observedCopy(state, 2, seed);
            assertEquals(state.bagTokens, copy.bagTokens);
            assertEquals(state.bagCards, copy.bagCards);
            assertEquals(
                    "Current legal theft cap is part of the action interface",
                    state.embezzled,
                    copy.embezzled);
            assertEquals(actualActions, new HashSet<>(actions(copy)));
        }
    }

    @Test
    public void legalTheftLimitDoesNotRevealMorePreciseEarlierEmbezzlement() {
        LoyalistGameState first = fixture(Phase.INTRIGUE_STEAL, 2);
        setBag(first, 1, 0);
        first.embezzled = 0;
        first.peekBag(2, true);
        LoyalistGameState second = fullCopy(first);
        second.embezzled = 2;
        second.wealth[4] += 2;
        second.treasury[0] -= 2;
        second.wealthChanged(4);
        second.treasuryChanged(0);
        // Both offer exactly STEAL 0 and STEAL 1. The action menu does not disclose
        // whether a previous minister stole zero or two tokens before this turn.
        assertEquals(new HashSet<>(actions(first)), new HashSet<>(actions(second)));
        assertIndistinguishable(first, second, 2);
    }

    @Test
    public void earlierBagObservationDoesNotTrackLaterPlayersChanges() {
        LoyalistGameState first = fixture(Phase.INTRIGUE_STEAL, 2);
        setBag(first, 3, -1, 2);
        first.peekBag(2, true);
        first.closeBag();
        first.transition(Phase.INTRIGUE_STEAL, 3);
        first.peekBag(3, true);
        LoyalistGameState second = fullCopy(first);
        second.bagTokens += 2;
        second.wealth[4] += 1;
        second.treasury[0] -= 3;
        second.wealthChanged(4);
        second.treasuryChanged(0);
        second.rememberedBagTokens[3] = second.bagTokens;
        for (int seed = 0; seed < 12; seed++) {
            LoyalistGameState left = observedCopy(first, 2, seed);
            LoyalistGameState right = observedCopy(second, 2, seed);
            assertEquals(left, right);
            assertEquals(3, left.rememberedBagTokens[2]);
            assertEquals(3, right.rememberedBagTokens[2]);
            assertFalse(left.liveBagTokens[2]);
            assertFalse(left.liveBagCards[2]);
        }
    }

    @Test
    public void settlementDetailsAreKnownToKingAndOnlyTotalIsPublic() {
        LoyalistGameState first = fixture(Phase.REWARD, 0);
        setBag(first, 4, -1, 2);
        first.lastTotal = 5;
        first.peekBag(0, true);
        LoyalistGameState king = observedCopy(first, 0, 18);
        assertEquals(first.bagTokens, king.bagTokens);
        assertEquals(first.bagCards, king.bagCards);
        LoyalistGameState second = fullCopy(first);
        assertTrue(second.deck.remove(Integer.valueOf(-2)));
        second.deck.add(-1);
        second.bagCards.set(0, -2);
        second.bagTokens++;
        second.treasury[0]--;
        second.treasuryChanged(0);
        second.knownBagCards.set(0, new ArrayList<>(second.bagCards));
        second.rememberedBagTokens[0] = second.bagTokens;
        assertIndistinguishable(first, second, 4);
        assertEquals(5, observedCopy(second, 4, 18).lastTotal);
    }

    @Test
    public void publicSettlementTotalDoesNotRevealItsHiddenMilitaryBonus() {
        LoyalistGameState first = fixture(Phase.REWARD, 0);
        first.reforms[2] = true;
        setBag(first, 4, -1, 2);
        first.mSupply = 2;
        first.lastMilitaryBonus = 1;
        first.lastTotal = 6;
        first.peekBag(0, true);
        LoyalistGameState second = fullCopy(first);
        second.mSupply = 4;
        second.lastMilitaryBonus = 2;
        second.bagTokens = 3;
        second.treasury[0]++;
        second.treasuryChanged(0);
        second.rememberedBagTokens[0] = 3;
        assertIndistinguishable(first, second, 4);
        assertEquals(1, observedCopy(first, 0, 3).lastMilitaryBonus);
        assertEquals(2, observedCopy(second, 0, 3).lastMilitaryBonus);
    }

    @Test
    public void hiddenImpartialityRemovalDoesNotTellAnEarlierObserverWhichCardWasRemoved() {
        LoyalistGameState first = fixture(Phase.INTRIGUE_CARDS, 4);
        first.reforms[5] = true;
        setBag(first, 8, -2, -3);
        first.knownBagCards.get(2).add(-2);
        first.rememberedBagCount[2] = 1;
        first.rememberedBagTokens[2] = 1;
        first.remember(2, "Previously observed one -2 card and one token, before later intrigue");
        first.closeBag();
        LoyalistGameState second = fullCopy(first);
        assertTrue(second.deck.remove(Integer.valueOf(3)));
        second.deck.add(-3);
        second.bagCards.set(1, 3);
        second.bagTokens = 3;
        second.treasury[0] += 5;
        second.treasuryChanged(0);
        // The king sees either [-2] + eight tokens or [+3] + three tokens.
        // Both publicly announce six, while minister 2 cannot know whether their -2 was removed.
        LoyalistForwardModel.settle(first);
        LoyalistForwardModel.settle(second);
        assertEquals(6, first.lastTotal);
        assertEquals(6, second.lastTotal);
        assertEquals(first.publicLog, second.publicLog);
        assertEquals(first.privateLog.get(2), second.privateLog.get(2));
        assertIndistinguishable(first, second, 2);
    }

    @Test
    public void copyingNeverExposesOtherSeatsPrivateTranscript() {
        LoyalistGameState state = fixture(Phase.PURCHASE, 2);
        state.remember(0, "Secret treasury audit: P=12");
        state.remember(1, "Secret role: loyalist");
        state.remember(2, "Own fact which must survive");
        state.remember(4, "Secret role: spy");
        LoyalistGameState copy = observedCopy(state, 2, 7);
        assertEquals(state.privateLog.get(2), copy.privateLog.get(2));
        for (int player : new int[] {0, 1, 3, 4}) assertTrue(copy.privateLog.get(player).isEmpty());
    }

    @Test
    public void historicalBagMemoryDoesNotRequireCardsToRemainInAnEmptiedBag() {
        LoyalistGameState state = fixture(Phase.PURCHASE, 2);
        assertTrue(state.deck.remove(Integer.valueOf(-2)));
        state.dead.add(-2);
        state.knownBagCards.get(2).add(-2);
        state.rememberedBagTokens[2] = 3;
        state.rememberedBagCount[2] = 1;
        state.liveBagTokens[2] = false;
        state.liveBagCards[2] = false;
        for (int seed = 0; seed < 12; seed++) {
            LoyalistGameState copy = observedCopy(state, 2, seed);
            assertTrue(copy.bagCards.isEmpty());
            assertEquals(0, copy.bagTokens);
            assertEquals(3, copy.rememberedBagTokens[2]);
            assertEquals(List.of(-2), copy.knownBagCards.get(2));
            assertCardInventory(copy);
        }
    }

    @Test
    public void keepCardsPreservesTheActingMinistersPrivateSelectedCards() {
        LoyalistGameState state = fixture(Phase.KEEP_CARDS, 2);
        state.keepSelected.add(state.hands.get(2).remove(0));
        Set<AbstractAction> actualActions = new HashSet<>(actions(state));
        for (int seed = 0; seed < 12; seed++) {
            LoyalistGameState copy = observedCopy(state, 2, seed);
            assertEquals(state.keepSelected, copy.keepSelected);
            assertEquals(state.hands.get(2), copy.hands.get(2));
            assertEquals(actualActions, new HashSet<>(actions(copy)));
            assertCardInventory(copy);
        }
    }

    @Test
    public void competitionModeStripsFrameworkHistoryEvenWhenItContainsSecrets() {
        LoyalistGameState state = fixture(Phase.PURCHASE, 2);
        assertTrue(
                "Loyalist must enable competition mode during setup",
                state.getCoreGameParameters().competitionMode);
        state.recordHistory("Unredacted debug trace: seat 4 is the spy");
        assertFalse(state.getHistoryAsText().isEmpty());
        LoyalistGameState copy = observedCopy(state, 2, 3);
        assertTrue(copy.getHistory().isEmpty());
        assertTrue(copy.getHistoryAsText().isEmpty());
    }

    @Test
    public void copiedObservationsCanBeRedeterminizedWithoutChangingKnownFactsOrActions() {
        LoyalistGameState state = fixture(Phase.INTRIGUE_CARDS, 2);
        setBag(state, 3, -1, 2);
        state.peekBag(2, true);
        LoyalistGameState observation = observedCopy(state, 2, 9);
        LoyalistGameState exactCopy = fullCopy(observation);
        assertEquals(observation, exactCopy);
        assertEquals(new HashSet<>(actions(observation)), new HashSet<>(actions(exactCopy)));
        for (int seed = 0; seed < 12; seed++) {
            LoyalistGameState resampled = observedCopy(observation, 2, seed);
            assertEquals(state.roles[2], resampled.roles[2]);
            assertEquals(state.hands.get(2), resampled.hands.get(2));
            assertEquals(state.bagCards, resampled.bagCards);
            assertEquals(state.bagTokens, resampled.bagTokens);
            assertEquals(new HashSet<>(actions(state)), new HashSet<>(actions(resampled)));
            assertCardInventory(resampled);
        }
    }

    @Test
    public void copiedMutableDataDoesNotAliasTheAuthoritativeGame() {
        LoyalistGameState state = fixture(Phase.PURCHASE, 2);
        state.remember(2, "Private memory");
        LoyalistGameState before = fullCopy(state);
        LoyalistGameState copy = observedCopy(state, 2, 77);
        copy.wealth[2]++;
        copy.hands.get(2).clear();
        copy.privateLog.get(2).clear();
        copy.knownTreasury[2][1] = 1000;
        copy.deck.clear();
        assertEquals(before, state);
    }

    @Test
    public void speechChoiceDoesNotForceTruthOrExposeTheExecutedQuantity() {
        LoyalistGameState first = fixture(Phase.SUPPLY_REPORT, 1);
        first.pendingTokenChange = 1;
        LoyalistGameState second = fullCopy(first);
        second.pendingTokenChange = 3;
        assertEquals(new HashSet<>(actions(first)), new HashSet<>(actions(second)));
        List<AbstractAction> reports = actions(observedCopy(first, 1, 31));
        assertTrue(reports.contains(new LoyalistAction(LoyalistAction.Kind.REPORT, 0)));
        assertTrue(reports.contains(new LoyalistAction(LoyalistAction.Kind.REPORT, 4)));
    }
}

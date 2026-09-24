package games.loyalist;

import static core.CoreConstants.GameResult.*;

import static games.loyalist.LoyalistGameState.Phase.*;
import static games.loyalist.LoyalistGameState.Role.*;

import static org.junit.Assert.*;

import core.actions.AbstractAction;

import games.loyalist.actions.LoyalistAction;
import games.loyalist.actions.LoyalistAction.Kind;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class LoyalistRulesTest {
    LoyalistGameState s;
    LoyalistForwardModel fm;

    @Before
    public void setup() {
        s = new LoyalistGameState(new LoyalistParameters(991L), 5);
        fm = new LoyalistForwardModel();
        fm.setup(s);
        s.roles = new LoyalistGameState.Role[] {KING, LOYALIST, MAGNATE, MAGNATE, SPY};
        s.offices = new int[] {1, 2, 3};
        s.round = 1;
        s.eventId = 1;
        for (List<Integer> hand : s.hands) {
            s.deck.addAll(hand);
            hand.clear();
        }
        for (int p = 1; p < 5; p++) {
            s.deck.remove(Integer.valueOf(1));
            s.deck.remove(Integer.valueOf(-1));
            s.hands.get(p).addAll(List.of(1, -1));
        }
        s.refreshOwners();
    }

    private List<AbstractAction> actions() {
        return fm.computeAvailableActions(s);
    }

    private boolean offers(Kind kind, int n) {
        return actions().contains(new LoyalistAction(kind, n));
    }

    private void act(Kind kind, int n) {
        LoyalistAction a = new LoyalistAction(kind, n);
        assertTrue("Legal action " + a + " at " + s, actions().contains(a));
        fm.next(s, a);
    }

    private void cards(int player, int... values) {
        s.deck.addAll(s.hands.get(player));
        s.hands.get(player).clear();
        for (int v : values) {
            assertTrue(s.deck.remove(Integer.valueOf(v)));
            s.hands.get(player).add(v);
        }
    }

    private void bag(int tokens, int... values) {
        s.deck.addAll(s.bagCards);
        s.bagCards.clear();
        s.bagTokens = tokens;
        for (int v : values) {
            assertTrue(s.deck.remove(Integer.valueOf(v)));
            s.bagCards.add(v);
        }
    }

    @Test
    public void initialSetupHasCorrectRolesResourcesAndFortyCards() {
        assertArrayEquals(new int[] {12, 6, 3}, s.treasury);
        assertArrayEquals(new int[] {0, 1, 1, 1, 1}, s.wealth);
        assertEquals(32, s.deck.size());
        for (int p = 1; p < 5; p++) assertEquals(2, s.hands.get(p).size());
        assertEquals(40, LoyalistData.powerCards().size());
        assertEquals(18, LoyalistData.EVENTS.length);
        assertEquals(12, LoyalistData.REFORMS.length);
        assertEquals(2, Collections.frequency(Arrays.asList(s.roles), MAGNATE));
    }

    @Test
    public void officeAssignmentUsesThreeDistinctMinisters() {
        s.offices = new int[] {-1, -1, -1};
        s.officeIndex = 0;
        s.round = 0;
        s.transition(ASSIGN_OFFICES, 0);
        act(Kind.ASSIGN, 1);
        assertFalse(offers(Kind.ASSIGN, 1));
        act(Kind.ASSIGN, 2);
        act(Kind.ASSIGN, 3);
        assertArrayEquals(new int[] {1, 2, 3}, s.offices);
        assertEquals(1, s.round);
        assertEquals(PURCHASE, s.phase);
        assertEquals(3, s.getCurrentPlayer());
    }

    @Test
    public void ordinarySupplyAllowsZeroThroughAllAndConcealsActualInReport() {
        s.transition(SUPPLY, 1);
        s.supplyResource = 0;
        bag(2, 0);
        s.peekBag(1, false);
        assertTrue(offers(Kind.SUPPLY, 0));
        assertTrue(offers(Kind.SUPPLY, 12));
        act(Kind.SUPPLY, 4);
        assertEquals(8, s.treasury[0]);
        assertEquals(6, s.bagTokens);
        assertEquals(SUPPLY_REPORT, s.phase);
        assertTrue(offers(Kind.REPORT, 0));
        assertTrue(offers(Kind.REPORT, 12));
        assertFalse(s.publicLog.stream().anyMatch(x -> x.contains("Supplied:")));
    }

    @Test
    public void daedongMakesWealthAnAllowedSupplyForPopulationEvent() {
        s.eventId = 1;
        s.reforms[3] = true;
        s.supplyResource = 1;
        s.transition(SUPPLY, 2);
        assertTrue(offers(Kind.SUPPLY, 6));
        act(Kind.SUPPLY, 3);
        assertEquals(3, s.treasury[1]);
        assertEquals(3, s.bagTokens);
    }

    @Test
    public void sogoUsesPairsAndDoesNotAllowUnpairedPopulationOnMilitaryOnlyEvent() {
        s.eventId = 3;
        s.reforms[11] = true;
        s.supplyResource = 0;
        s.transition(SUPPLY, 1);
        assertTrue(offers(Kind.CONVERT_SUPPLY, 6));
        assertFalse(offers(Kind.SUPPLY, 1));
        act(Kind.CONVERT_SUPPLY, 2);
        assertEquals(8, s.treasury[0]);
        assertEquals(2, s.bagTokens);
        assertEquals(2, s.reserve);
    }

    @Test
    public void firstSupplierNominatesAnotherMinister() {
        s.firstSupplier = 1;
        s.supplyResource = 2;
        s.eventId = 3;
        s.transition(SUPPLY_REPORT, 3);
        act(Kind.REPORT, 0);
        assertEquals(FIRST_NOMINATION, s.phase);
        assertEquals(1, s.getCurrentPlayer());
        assertFalse(offers(Kind.NOMINATE, 1));
        act(Kind.NOMINATE, 4);
        assertEquals(INTRIGUE_STEAL, s.phase);
        assertEquals(4, s.getCurrentPlayer());
    }

    @Test
    public void individualAndGlobalEmbezzlementCapsDoNotRefundAfterContribution() {
        bag(8);
        s.embezzled = 2;
        s.transition(INTRIGUE_STEAL, 2);
        s.peekBag(2, true);
        assertTrue(offers(Kind.STEAL, 2));
        assertFalse(offers(Kind.STEAL, 3));
        act(Kind.STEAL, 2);
        act(Kind.CONTRIBUTE, 2);
        assertEquals(4, s.embezzled);
        assertEquals(8, s.bagTokens);
    }

    @Test
    public void centralisationReducesBothEmbezzlementCaps() {
        s.reforms[6] = true;
        bag(10);
        s.embezzled = 0;
        s.transition(INTRIGUE_STEAL, 2);
        assertTrue(offers(Kind.STEAL, 2));
        assertFalse(offers(Kind.STEAL, 3));
        s.embezzled = 2;
        assertTrue(offers(Kind.STEAL, 1));
        assertFalse(offers(Kind.STEAL, 2));
    }

    @Test
    public void inspectorCanReplaceIntrigueWithPrivateWealthInspection() {
        s.inspector = 2;
        s.wealth[4] = 7;
        bag(3);
        s.transition(INTRIGUE_STEAL, 2);
        s.peekBag(2, true);
        act(Kind.INSPECT, 4);
        assertEquals(TESTIMONY, s.phase);
        assertEquals(3, s.bagTokens);
        assertEquals(7, s.knownWealth[0][4]);
        assertEquals(7, s.knownWealth[2][4]);
        assertTrue(s.publicLog.stream().noneMatch(x -> x.contains("wealth=7")));
    }

    @Test
    public void cardPlayCanSelectArbitraryMultisetWithoutRemovingEarlierCards() {
        cards(2, -2, 1, 1);
        bag(3, 3);
        s.transition(INTRIGUE_CARDS, 2);
        act(Kind.PLAY_CARD, 1);
        act(Kind.PLAY_CARD, -2);
        act(Kind.FINISH_CARDS, 0);
        assertEquals(List.of(3, 1, -2), s.bagCards);
        assertEquals(List.of(1), s.hands.get(2));
        assertEquals(TESTIMONY, s.phase);
    }

    @Test
    public void militaryBonusUsesOriginalSupplyAfterEmbezzlement() {
        s.reforms[2] = true;
        s.mSupply = 5;
        s.eventId = 1;
        bag(0, 0);
        LoyalistForwardModel.settle(s);
        assertEquals(3, s.lastMilitaryBonus);
        assertEquals(3, s.lastTotal);
        assertEquals(REWARD, s.phase);
    }

    @Test
    public void proposedReformDoesNotAffectCurrentSettlement() {
        s.proposedReform = 2;
        s.mSupply = 6;
        bag(1, 1);
        s.eventId = 1;
        LoyalistForwardModel.settle(s);
        assertEquals(2, s.lastTotal);
        assertEquals(1, s.crisis);
        assertFalse(s.reforms[2]);
        assertTrue(s.removedReforms[2]);
    }

    @Test
    public void impartialityRemovesOnlyOneLowestNegativeCard() {
        s.reforms[5] = true;
        cards(1);
        cards(2);
        cards(3);
        cards(4);
        bag(10, -3, -2, -1, 1);
        LoyalistForwardModel.settle(s);
        assertEquals(List.of(-2, -1, 1), s.bagCards);
        assertTrue(s.dead.contains(-3));
        assertEquals(8, s.lastTotal);
    }

    @Test
    public void successBonusIsNotMultiplied() {
        s.eventId = 5;
        bag(4);
        int before = s.treasury[0];
        LoyalistForwardModel.returnResources(s, 0);
        assertEquals(before + 9, s.treasury[0]);
        assertEquals(0, s.bagTokens);
    }

    @Test
    public void additionalReturnIsAppliedOnceToBonus() {
        s.eventId = 8;
        bag(4);
        int before = s.treasury[1];
        LoyalistForwardModel.returnResources(s, 1);
        assertEquals(before + 7, s.treasury[1]);
    }

    @Test
    public void halfDestructionRoundsUpForBagAndSeparatelyForBonus() {
        s.eventId = 17;
        bag(5);
        int before = s.treasury[2];
        LoyalistForwardModel.returnResources(s, 2);
        assertEquals(before + 2, s.treasury[2]);
        assertEquals(4, s.reserve);
    }

    @Test
    public void destroyedReturnAndBonusBothGoToReserve() {
        s.eventId = 7;
        bag(5);
        int[] before = s.treasury.clone();
        LoyalistForwardModel.returnResources(s, 2);
        assertArrayEquals(before, s.treasury);
        assertEquals(6, s.reserve);
    }

    @Test
    public void purchaseRatesAndOncePerRoundRestriction() {
        s.crisis = 2;
        s.wealth[2] = 4;
        s.queue = new ArrayList<>(List.of(2, 1));
        s.queueIndex = 0;
        s.transition(PURCHASE, 2);
        int before = s.hands.get(2).size();
        act(Kind.BUY, 0);
        assertEquals(before + 4, s.hands.get(2).size());
        assertEquals(3, s.wealth[2]);
        s.transition(EXCHANGE, 2);
        assertFalse(offers(Kind.BUY, 0));
        assertFalse(offers(Kind.SELL, 0));
    }

    @Test
    public void cardRewardPreventsSaleButDoesNotCountAsPurchase() {
        s.acquired[2] = true;
        s.bought[2] = false;
        s.reserve = 5;
        s.wealth[2] = 1;
        cards(2, -1, 1, 2);
        s.transition(EXCHANGE, 2);
        assertFalse(offers(Kind.SELL, 0));
        assertTrue(offers(Kind.BUY, 0));
    }

    @Test
    public void sellingUsesLowestValuesAndRequiresReserveToken() {
        cards(2, -3, -1, 1, 3);
        s.crisis = 1;
        s.reserve = 0;
        s.transition(EXCHANGE, 2);
        assertFalse(offers(Kind.SELL, 0));
        s.reserve = 1;
        s.queue = new ArrayList<>(List.of(2, 3));
        s.queueIndex = 0;
        act(Kind.SELL, 0);
        assertEquals(List.of(1, 3), s.hands.get(2));
        assertEquals(2, s.wealth[2]);
        assertEquals(0, s.reserve);
        assertTrue(s.dead.containsAll(List.of(-3, -1)));
    }

    @Test
    public void deckRefillsFromDeadCardsAndPartialDrawIsAllowed() {
        for (int p = 1; p < 5; p++) {
            s.hands.get(p).addAll(s.deck);
            s.deck.clear();
            break;
        }
        s.dead.add(s.hands.get(1).remove(0));
        s.dead.add(s.hands.get(1).remove(0));
        s.knownDead.get(1).addAll(s.dead);
        int n = LoyalistForwardModel.draw(s, 2, 4);
        assertEquals(2, n);
        assertEquals(0, s.deck.size());
        assertEquals(0, s.dead.size());
        assertTrue(s.knownDead.get(1).isEmpty());
    }

    @Test
    public void centralisationAllowsOwnerToChooseWhichTwoCardsToKeep() {
        cards(2, -2, 1, 3);
        s.reforms[6] = true;
        s.resumePhase = CONVERT_W_M;
        s.resumePlayer = 0;
        s.transition(KEEP_CARDS, 2);
        act(Kind.KEEP, -2);
        act(Kind.KEEP, 3);
        assertEquals(List.of(-2, 3), s.hands.get(2));
        assertTrue(s.dead.contains(1));
        assertEquals(CONVERT_W_M, s.phase);
    }

    @Test
    public void ordinaryExileConfiscatesHalfAndDoesNotPenalizeLoyalRole() {
        cards(1, -2, -1, 1, 2, 3);
        s.wealth[1] = 5;
        s.transition(EXILE, 0);
        act(Kind.EXILE, 1);
        assertEquals(3, s.wealth[1]);
        assertEquals(8, s.treasury[1]);
        assertEquals(0, s.crisis);
        assertEquals(2, s.cardsToConfiscate);
        act(Kind.DISCARD_BLIND, 0);
        act(Kind.DISCARD_BLIND, 0);
        assertEquals(3, s.hands.get(1).size());
        assertEquals(REASSIGN_OFFICE, s.phase);
        act(Kind.ASSIGN, 2);
        assertEquals(2, s.offices[0]);
        assertEquals(2, s.offices[1]);
        assertTrue(s.isExiled(1));
        assertEquals(2, s.exileThroughRound[1]);
    }

    @Test
    public void exileLastsWholeFollowingRoundAndReturnsWithoutOffice() {
        s.exileThroughRound[1] = 2;
        s.offices[0] = 2;
        s.reforms[7] = true;
        s.transition(APPOINT_INSPECTOR, 0);
        act(Kind.APPOINT, 2);
        assertEquals(2, s.round);
        assertTrue(s.isExiled(1));
        s.transition(APPOINT_INSPECTOR, 0);
        act(Kind.APPOINT, 2);
        assertEquals(3, s.round);
        assertFalse(s.isExiled(1));
        assertFalse(Arrays.stream(s.offices).anyMatch(x -> x == 1));
    }

    @Test
    public void royalConversionsApplyWealthBeforePopulation() {
        s.treasury = new int[] {4, 0, 0};
        s.transition(CONVERT_W_M, 0);
        assertTrue(offers(Kind.CONVERT, 2));
        act(Kind.CONVERT, 2);
        act(Kind.CONVERT, 2);
        assertArrayEquals(new int[] {0, 2, 0}, s.treasury);
        assertEquals(2, s.reserve);
    }

    @Test
    public void borderCouncilReturnsUnchosenCardToBottom() {
        s.eventOptions = new ArrayList<>(List.of(1, 2));
        s.eventDecks.get(0).removeAll(s.eventOptions);
        s.transition(EVENT_CHOICE, 0);
        act(Kind.CHOOSE, 1);
        assertEquals(1, s.eventId);
        assertEquals(Integer.valueOf(2), s.eventDecks.get(0).get(s.eventDecks.get(0).size() - 1));
    }

    @Test
    public void reformOpportunityIsConsumedEvenWhenKingPasses() {
        s.prosperity = 2;
        s.transition(REFORM_PROPOSAL, 0);
        act(Kind.PASS, 0);
        assertTrue(s.triggered[2]);
        assertEquals(0, s.proposedReform);
        s.transition(APPOINT_INSPECTOR, 0);
        act(Kind.APPOINT, 1);
        assertEquals(PURCHASE, s.phase);
    }

    @Test
    public void finalPurgeOfLoyalistCanGiveSpyFourthCrisis() {
        s.prosperity = 10;
        s.crisis = 3;
        s.transition(FINAL_PURGE, 0);
        act(Kind.EXILE, 1);
        assertEquals(GAME_END, s.getGameStatus());
        assertEquals(WIN_GAME, s.getPlayerResults()[4]);
        assertEquals(LOSE_GAME, s.getPlayerResults()[0]);
    }

    @Test
    public void magnateNeedsThreeWealthAndStrictlyMoreThanMilitaryPlusLoyalist() {
        s.wealth = new int[] {0, 0, 2, 1, 9};
        s.treasury[2] = 0;
        LoyalistForwardModel.finish(s, false);
        assertEquals(WIN_GAME, s.getPlayerResults()[0]);
        setup();
        s.wealth = new int[] {0, 1, 3, 2, 9};
        s.treasury[2] = 2;
        LoyalistForwardModel.finish(s, false);
        assertEquals(WIN_GAME, s.getPlayerResults()[0]);
        setup();
        s.wealth = new int[] {0, 1, 3, 2, 9};
        s.treasury[2] = 1;
        LoyalistForwardModel.finish(s, false);
        assertEquals(WIN_GAME, s.getPlayerResults()[2]);
    }

    @Test
    public void equalQualifyingMagnatesShareVictory() {
        s.wealth = new int[] {0, 1, 5, 5, 20};
        s.treasury[2] = 1;
        LoyalistForwardModel.finish(s, false);
        assertEquals(WIN_GAME, s.getPlayerResults()[2]);
        assertEquals(WIN_GAME, s.getPlayerResults()[3]);
        assertEquals(LOSE_GAME, s.getPlayerResults()[4]);
    }

    @Test
    public void exiledMagnateCannotWinAndExiledLoyalistStillSharesKingWin() {
        s.wealth = new int[] {0, 1, 8, 2, 30};
        s.treasury[2] = 1;
        s.exileThroughRound[2] = 2;
        s.exileThroughRound[1] = 2;
        LoyalistForwardModel.finish(s, false);
        assertEquals(WIN_GAME, s.getPlayerResults()[0]);
        assertEquals(WIN_GAME, s.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, s.getPlayerResults()[2]);
    }

    @Test
    public void publicMarketAndTokenRewardUpdatePreviouslyKnownWealth() {
        s.queue = new ArrayList<>(List.of(2, 3));
        s.queueIndex = 0;
        s.transition(PURCHASE, 2);
        act(Kind.BUY, 0);
        assertEquals(0, s.knownWealth[0][2]);
        assertEquals(0, ((LoyalistGameState) s.copy(0)).wealth[2]);
        s.bought[2] = false;
        s.acquired[2] = false;
        s.crisis = 2;
        s.reserve = 3;
        s.queueIndex = 0;
        s.transition(EXCHANGE, 2);
        act(Kind.SELL, 0);
        assertEquals(1, s.knownWealth[0][2]);
        assertEquals(1, ((LoyalistGameState) s.copy(0)).wealth[2]);
        bag(1);
        s.peekBag(0, true);
        s.transition(REWARD, 0);
        act(Kind.REWARD_TOKEN, 2);
        assertEquals(2, s.knownWealth[0][2]);
        assertEquals(2, ((LoyalistGameState) s.copy(0)).wealth[2]);
    }

    @Test
    public void knownConfiscationAndLevyPreserveDerivableBalancesWithoutRevealingUnknowns() {
        s.wealth[2] = 5;
        s.knownWealth[0][2] = 5;
        s.knownWealth[4][2] = -1;
        s.refreshOwners();
        LoyalistForwardModel.exile(s, 2);
        assertEquals(3, s.knownWealth[0][2]);
        assertEquals(8, s.knownTreasury[0][1]);
        assertEquals(-1, s.knownWealth[4][2]);
        assertEquals(-1, s.knownTreasury[4][1]);
        setup();
        s.reforms[10] = true;
        s.transition(ROUND_LEVY, 0);
        act(Kind.LEVY, 1);
        assertEquals(0, s.knownWealth[0][1]);
        assertEquals(7, s.knownTreasury[0][1]);
    }

    @Test
    public void publicLandReformIncomePreservesKnownTreasuryBalance() {
        s.reforms[1] = true;
        s.transition(APPOINT_INSPECTOR, 0);
        act(Kind.APPOINT, 1);
        assertEquals(7, s.treasury[1]);
        assertEquals(7, s.knownTreasury[0][1]);
        assertEquals(7, ((LoyalistGameState) s.copy(0)).treasury[1]);
    }

    @Test
    public void royalConversionPreservesBalancesInferableFromPriorKnowledge() {
        s.transition(CONVERT_W_M, 0);
        act(Kind.CONVERT, 2);
        assertEquals(2, s.knownTreasury[0][1]);
        assertEquals(5, s.knownTreasury[0][2]);
        act(Kind.CONVERT, 1);
        assertArrayEquals(new int[] {10, 3, 5}, s.knownTreasury[0]);
        assertArrayEquals(s.treasury, ((LoyalistGameState) s.copy(0)).treasury);
    }

    private int tokenInventory(LoyalistGameState state) {
        int total = state.reserve + state.bagTokens;
        assertTrue(state.reserve >= 0);
        assertTrue(state.bagTokens >= 0);
        for (int n : state.treasury) {
            assertTrue(n >= 0);
            total += n;
        }
        for (int n : state.wealth) {
            assertTrue(n >= 0);
            total += n;
        }
        return total;
    }

    @Test
    public void sampledHiddenTokensConserveExactInitialInventory() {
        s.treasury[0] -= 2;
        s.treasuryChanged(0);
        s.bagTokens = 2;
        s.transition(INTRIGUE_STEAL, 2);
        s.peekBag(2, true);
        assertEquals(25, tokenInventory(s));
        for (int player = 0; player < 5; player++)
            for (int seed = 0; seed < 20; seed++) {
                s.setRedeterminisationSeed(seed);
                LoyalistGameState copy = (LoyalistGameState) s.copy(player);
                assertEquals(25, tokenInventory(copy));
                assertEquals(25, tokenInventory((LoyalistGameState) copy.copy(player)));
            }
    }

    @Test
    public void hiddenMultiplierReturnsRespectPublicTotalInterval() {
        s.treasury[0] -= 4;
        s.treasuryChanged(0);
        s.bagTokens = 4;
        s.eventId = 5;
        LoyalistForwardModel.returnResources(s, 0);
        s.transition(TREASURY_PLAN, 0);
        assertEquals(30, tokenInventory(s));
        assertEquals(26, s.publicTokenLowerBound);
        assertEquals(51, s.publicTokenUpperBound);
        for (int player = 0; player < 5; player++)
            for (int seed = 0; seed < 20; seed++) {
                s.setRedeterminisationSeed(seed);
                LoyalistGameState copy = (LoyalistGameState) s.copy(player);
                int total = tokenInventory(copy);
                assertTrue(total >= 26);
                assertTrue(total <= 51);
            }
    }

    @Test
    public void successorMilitaryOfficerDoesNotInheritPredecessorsPrivateSupplyKnowledge() {
        s.militarySupplier = 3;
        s.mSupply = 2;
        s.offices[2] = 2;
        s.exileThroughRound[3] = 2;
        s.refreshOwners();
        s.transition(CONVERT_W_M, 0);
        LoyalistGameState other = (LoyalistGameState) s.copy();
        other.mSupply = 4;
        for (int seed = 0; seed < 16; seed++) {
            s.setRedeterminisationSeed(seed);
            other.setRedeterminisationSeed(seed);
            assertEquals(s.copy(2), other.copy(2));
            assertEquals(2, ((LoyalistGameState) s.copy(3)).mSupply);
            assertEquals(4, ((LoyalistGameState) other.copy(3)).mSupply);
        }
    }

    @Test
    public void kingsRoundedMilitaryBonusDoesNotRevealExactSupply() {
        s.militarySupplier = 3;
        s.mSupply = 3;
        s.reforms[2] = true;
        s.lastMilitaryBonus = 2;
        s.lastTotal = 6;
        s.transition(REWARD, 0);
        s.peekBag(0, true);
        LoyalistGameState other = (LoyalistGameState) s.copy();
        other.mSupply = 4;
        for (int seed = 0; seed < 16; seed++) {
            s.setRedeterminisationSeed(seed);
            other.setRedeterminisationSeed(seed);
            LoyalistGameState a = (LoyalistGameState) s.copy(0);
            LoyalistGameState b = (LoyalistGameState) other.copy(0);
            assertEquals(a, b);
            assertEquals(2, a.lastMilitaryBonus);
            assertTrue(a.mSupply == 3 || a.mSupply == 4);
        }
    }

    @Test
    public void borderCouncilRejectedBottomCardRemainsKnownToKingUntilDrawn() {
        s.eventOptions = new ArrayList<>(List.of(1, 2));
        s.eventDecks.get(0).removeAll(s.eventOptions);
        s.transition(EVENT_CHOICE, 0);
        act(Kind.CHOOSE, 1);
        assertEquals(List.of(2), s.kingKnownEventBottom.get(0));
        for (int seed = 0; seed < 20; seed++) {
            s.setRedeterminisationSeed(seed);
            LoyalistGameState king = (LoyalistGameState) s.copy(0);
            List<Integer> deck = king.eventDecks.get(0);
            assertEquals(Integer.valueOf(2), deck.get(deck.size() - 1));
            assertEquals(List.of(2), king.kingKnownEventBottom.get(0));
            LoyalistGameState other = (LoyalistGameState) s.copy(3);
            assertTrue(other.kingKnownEventBottom.get(0).isEmpty());
        }
        while (s.eventDecks.get(0).size() > 1) {
            assertNotEquals(2, LoyalistForwardModel.drawEvent(s));
            assertEquals(List.of(2), s.kingKnownEventBottom.get(0));
        }
        assertEquals(2, LoyalistForwardModel.drawEvent(s));
        assertTrue(s.kingKnownEventBottom.get(0).isEmpty());
    }

    @Test
    public void bagCardPermutationsDoNotRevealContributorsOrDisasterPosition() {
        bag(3, 2, -3, 0);
        s.transition(INTRIGUE_STEAL, 2);
        LoyalistGameState other = (LoyalistGameState) s.copy();
        Collections.reverse(other.bagCards);
        s.peekBag(2, true);
        other.peekBag(2, true);
        assertEquals(List.of(-3, 0, 2), s.getBagCards());
        assertEquals(s.getBagCards(), other.getBagCards());
        assertEquals(s.privateLog.get(2), other.privateLog.get(2));
        for (int seed = 0; seed < 20; seed++) {
            s.setRedeterminisationSeed(seed);
            other.setRedeterminisationSeed(seed);
            LoyalistGameState a = (LoyalistGameState) s.copy(2);
            LoyalistGameState b = (LoyalistGameState) other.copy(2);
            assertEquals(a, b);
            assertEquals(List.of(-3, 0, 2), a.bagCards);
        }
    }
}

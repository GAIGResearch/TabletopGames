package games.toads;

import core.actions.AbstractAction;
import games.toads.ToadConstants.ToadCardType;
import games.toads.abilities.BattleResult;
import games.toads.actions.*;
import games.toads.components.ToadCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.toads.ToadConstants.ToadCardType.*;
import static games.toads.ToadConstants.ToadGamePhase.PLAY;
import static games.toads.ToadConstants.ToadGamePhase.POST_BATTLE;
import static games.toads.ToadTestUtils.*;
import static org.junit.Assert.*;

/**
 * The Siege Cannon's guess, the order of the post-battle decisions and which player owns a Scout's or a Siege
 * Cannon's decision when player 1 attacks. The Rulebook 3 deck (cards.json) has 9 card types, one of each per player; the two Generals share one
 * guess ("General", key GENERAL_ONE), so there are 8 guesses when nothing is ruled out.
 * Unit tests build a BattleResult directly; integration tests play through fm.next with the players' own cards
 * rearranged by stack (player 0 attacks the first Battle, and the Defender attacks the next).
 */
public class SiegeCannonGuessTest {

    ToadParameters params;
    ToadForwardModel fm;
    ToadGameState state;

    @Before
    public void setUp() {
        params = tacticsParams(933);
        fm = new ToadForwardModel();
        state = newState(params, fm);
    }

    private List<AbstractAction> guessOptions(int guesser) {
        return new SiegeCannonGuess(guesser)._computeAvailableActions(state);
    }

    private static Set<AbstractAction> guesses(ToadCardType... types) {
        Set<AbstractAction> retValue = new HashSet<>();
        for (ToadCardType type : types)
            retValue.add(new GuessCard(type));
        return retValue;
    }

    /** The guess as the given player: the guesser is the current player, as when the SiegeCannonGuess is on the stack. */
    private void guessAs(int guesser, ToadCardType type) {
        state.setActionInProgress(new SiegeCannonGuess(guesser));
        assertEquals(guesser, state.getCurrentPlayer());
        new GuessCard(type).execute(state);
    }

    /** The current player plays the cards of these types from hand, to the Field and then the Flank. */
    private void play(ToadCardType field, ToadCardType flank) {
        int player = state.getCurrentPlayer();
        fm.next(state, new PlayFieldCard(inHand(state, player, field)));
        fm.next(state, new PlayFlankCard(inHand(state, player, flank)));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Guess groups and names
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void bothGeneralsShareOneGuessGroupAndEveryOtherTypeIsItsOwn() {
        assertEquals(GENERAL_ONE, GENERAL_ONE.guessGroup());
        assertEquals(GENERAL_ONE, GENERAL_TWO.guessGroup());
        for (ToadCardType type : ToadCardType.values())
            if (type != GENERAL_TWO)
                assertEquals(type, type.guessGroup());
    }

    @Test
    public void bothGeneralsAreGuessedAsGeneral() {
        assertEquals("General", GENERAL_ONE.guessName());
        assertEquals("General", GENERAL_TWO.guessName());
        assertEquals("Siege Cannon", SIEGE_CANNON.guessName());
        assertEquals("Scout", SCOUT.guessName());
    }

    // ---------------------------------------------------------------------------------------------------------
    // The Siege Cannon Tactic (AFTER) queues the guess for its owner
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void hiddenSiegeCannonQueuesAGuessForTheAttacker() {
        // Player 0 attacks with the Cannon in the Flank (the activated hidden card): its owner, player 0, guesses.
        BattleResult br = battle(state, plain(5), plain(4), siegeCannon(), plain(3));
        br.calculate();
        assertEquals(List.of(new SiegeCannonGuess(0)), br.getPostBattleActions());
        assertEquals(0, br.getPostBattleActions().get(0).getCurrentPlayer(state));
    }

    @Test
    public void attackingSiegeCannonGuessBelongsToPlayerOneWhenPlayerOneAttacks() {
        // Player 1 attacks with the Cannon in the Flank, so player 1 guesses.
        // (Queuing by side, isAttacker ? 0 : 1, would give it to player 0.)
        BattleResult br = new BattleResult(state, 1, plain(5), plain(4), siegeCannon(), plain(3));
        br.calculate();
        assertEquals(List.of(new SiegeCannonGuess(1)), br.getPostBattleActions());
        assertEquals(1, br.getPostBattleActions().get(0).getCurrentPlayer(state));
    }

    @Test
    public void defendingSiegeCannonGuessBelongsToPlayerZeroWhenPlayerOneAttacks() {
        // Player 1 attacks, player 0 defends with the Cannon in the Flank, so player 0 guesses.
        // (Queuing by side would give the Defender's guess to player 1.)
        BattleResult br = new BattleResult(state, 1, plain(5), plain(4), plain(3), siegeCannon());
        br.calculate();
        assertEquals(List.of(new SiegeCannonGuess(0)), br.getPostBattleActions());
        assertEquals(0, br.getPostBattleActions().get(0).getCurrentPlayer(state));
    }

    @Test
    public void siegeCannonInTheFieldMakesNoGuess() {
        // Only the hidden cards' Tactics activate: a face-up Field Cannon has no guess.
        BattleResult br = battle(state, siegeCannon(), plain(4), plain(5), plain(3));
        br.calculate();
        assertTrue(br.getPostBattleActions().isEmpty());
    }

    @Test
    public void bodyguardBlocksTheSiegeCannonGuess() {
        // Attacker Flank Siege Cannon, defender Flank Bodyguard: the Bodyguard (BLOCK) blocks the opposing hidden
        // card, so the Cannon's AFTER Tactic never runs and no guess is queued.
        BattleResult br = battle(state, plain(5), plain(4), siegeCannon(), bodyguard());
        br.calculate();
        assertTrue(br.getPostBattleActions().isEmpty());
    }

    @Test
    public void scoutDecisionGoesToTheOpponentOfItsOwnerWhenPlayerOneAttacks() {
        // Player 1 attacks with the Scout in the Flank: ScoutCards(1) - player 1 scouts, so player 0 chooses the
        // cards to show. (Queuing by side gives ScoutCards(0): player 1 would choose and player 0 would see.)
        BattleResult br = new BattleResult(state, 1, plain(5), plain(4), scout(), plain(3));
        br.calculate();
        assertEquals(List.of(new ScoutCards(1)), br.getPostBattleActions());
        assertEquals(0, br.getPostBattleActions().get(0).getCurrentPlayer(state));
    }

    @Test
    public void postBattleActionsAreListedInResolutionOrder() {
        // Attacker (player 0) Flank Siege Cannon (AFTER), defender (player 1) Flank Scout (START): the Scout's
        // Tactic runs first although the attacker's run first within a stage, so the list is Scout then Cannon.
        BattleResult br = battle(state, plain(5), plain(4), siegeCannon(), scout());
        br.calculate();
        assertEquals(List.of(new ScoutCards(1), new SiegeCannonGuess(0)), br.getPostBattleActions());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Candidate guesses
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void candidatesAreOnePerPrintedNameWithASingleGeneral() {
        // 9 types in play, nothing ruled out (no Casualties, empty discards): 9 - 1 (the Generals merged) = 8,
        // with no NONE_OF_THESE.
        List<AbstractAction> options = guessOptions(0);
        assertEquals(8, options.size());
        assertEquals(guesses(ASSASSIN, SCOUT, SABOTEUR, TRICKSTER, BERSERKER, BODYGUARD, GENERAL_ONE, SIEGE_CANNON),
                new HashSet<>(options));
    }

    @Test
    public void candidatesExcludeTheGuessersOwnCasualtyOnly() {
        // Guesser 0's Casualty is the Scout: ruled out. Player 1's Casualty (Saboteur) is not the guesser's, so it
        // stays. 8 - 1 = 7.
        state.tieBreakers[0] = scout();
        state.tieBreakers[1] = saboteurIII();
        List<AbstractAction> options = guessOptions(0);
        assertEquals(7, options.size());
        assertEquals(guesses(ASSASSIN, SABOTEUR, TRICKSTER, BERSERKER, BODYGUARD, GENERAL_ONE, SIEGE_CANNON),
                new HashSet<>(options));
    }

    @Test
    public void candidatesExcludeTheOpponentsDiscardsOnly() {
        // Guesser 1: player 0 (the opponent) has discarded the Berserker and the Bodyguard - ruled out. Player 1's
        // own discard (Trickster) is not. 8 - 2 = 6.
        state.playerDiscards.get(0).add(berserkerII());
        state.playerDiscards.get(0).add(bodyguard());
        state.playerDiscards.get(1).add(tricksterII());
        List<AbstractAction> options = guessOptions(1);
        assertEquals(6, options.size());
        assertEquals(guesses(ASSASSIN, SCOUT, SABOTEUR, TRICKSTER, GENERAL_ONE, SIEGE_CANNON), new HashSet<>(options));
    }

    @Test
    public void generalStaysACandidateWhileOneGeneralIsNotRuledOut() {
        // Player 1 has discarded General (Hostages), but General (Flags) may still be in hand: General is offered.
        // All 8 guesses remain.
        state.playerDiscards.get(1).add(generalHostages());
        List<AbstractAction> options = guessOptions(0);
        assertEquals(8, options.size());
        assertTrue(options.contains(new GuessCard(GENERAL_ONE)));
    }

    @Test
    public void generalIsExcludedWhenBothGeneralsAreRuledOut() {
        // General (Flags) in the opponent's discards and General (Hostages) as the guesser's Casualty: neither can
        // be in the opponent's hand, so General goes. 8 - 1 = 7.
        state.playerDiscards.get(1).add(generalFlags());
        state.tieBreakers[0] = generalHostages();
        List<AbstractAction> options = guessOptions(0);
        assertEquals(7, options.size());
        assertEquals(guesses(ASSASSIN, SCOUT, SABOTEUR, TRICKSTER, BERSERKER, BODYGUARD, SIEGE_CANNON),
                new HashSet<>(options));
    }

    @Test
    public void noneOfTheseIsOfferedOnlyWhenEveryGuessIsRuledOut() {
        // Only Scout and the two Generals in play; the Scout and General (Hostages) are in the opponent's discards
        // and General (Flags) is the guesser's Casualty: no guess is left, so the single option is NONE_OF_THESE.
        state.cardTypesInPlay = Set.of(SCOUT, GENERAL_ONE, GENERAL_TWO);
        state.playerDiscards.get(1).add(scout());
        state.playerDiscards.get(1).add(generalHostages());
        state.tieBreakers[0] = generalFlags();
        assertEquals(List.of(new GuessCard(NONE_OF_THESE)), guessOptions(0));
    }

    // ---------------------------------------------------------------------------------------------------------
    // The guess
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void correctGuessShowsTheFirstMatchingCardToTheGuesser() {
        // Player 1 holds Assassin, Trickster, Berserker, Trickster. Guess Trickster: only index 1 (the first
        // Trickster) becomes visible to player 0; the second Trickster (index 3) stays hidden.
        setHand(state, 1, assassinII(), tricksterII(), berserkerII(), tricksterII());
        guessAs(0, TRICKSTER);
        assertTrue(state.getPlayerHand(1).isComponentVisible(1, 0));
        assertFalse(state.getPlayerHand(1).isComponentVisible(3, 0));
        assertEquals(1, visibleTo(state, 1, 0));
    }

    @Test
    public void correctGuessLeavesTheOpponentsCardsWhereTheyAre() {
        // Information only: the guessed Berserker is not discarded or replaced (unlike the legacy
        // ForceOpponentDiscard); player 1's hand, deck and discards are unchanged, card for card.
        setHand(state, 1, assassinII(), berserkerII(), tricksterII());
        List<ToadCard> hand = new ArrayList<>(state.getPlayerHand(1).getComponents());
        List<ToadCard> deck = new ArrayList<>(state.getPlayerDeck(1).getComponents());
        guessAs(0, BERSERKER);
        assertEquals(hand, state.getPlayerHand(1).getComponents());
        assertEquals(deck, state.getPlayerDeck(1).getComponents());
        assertEquals(0, state.getDiscards(1).getSize());
    }

    @Test
    public void wrongGuessShowsNothing() {
        // Player 1 holds no Scout: nothing becomes visible to player 0.
        setHand(state, 1, assassinII(), tricksterII(), berserkerII());
        guessAs(0, SCOUT);
        assertEquals(0, visibleTo(state, 1, 0));
    }

    @Test
    public void generalGuessFindsGeneralHostages() {
        // Guess General (key GENERAL_ONE) against a hand whose only General is General (Hostages), at index 1.
        setHand(state, 1, assassinII(), generalHostages(), berserkerII());
        guessAs(0, GENERAL_ONE);
        assertTrue(state.getPlayerHand(1).isComponentVisible(1, 0));
        assertEquals(1, visibleTo(state, 1, 0));
    }

    @Test
    public void generalGuessFindsGeneralFlags() {
        // Guess General (key GENERAL_ONE) against a hand whose only General is General (Flags) - type GENERAL_TWO,
        // matched through its guess group - at index 1.
        setHand(state, 1, assassinII(), generalFlags(), berserkerII());
        guessAs(0, GENERAL_ONE);
        assertTrue(state.getPlayerHand(1).isComponentVisible(1, 0));
        assertEquals(1, visibleTo(state, 1, 0));
    }

    @Test
    public void playerOnesGuessShowsPlayerZerosCardToPlayerOne() {
        // Player 1 guesses Saboteur; player 0 holds Scout, Saboteur: index 1 of player 0's hand becomes visible to
        // player 1 (and nothing of player 1's hand to player 0).
        setHand(state, 0, scout(), saboteurIII());
        guessAs(1, SABOTEUR);
        assertTrue(state.getPlayerHand(0).isComponentVisible(1, 1));
        assertEquals(1, visibleTo(state, 0, 1));
        assertEquals(0, visibleTo(state, 1, 0));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Integration through fm.next
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void siegeCannonBattleGoesThroughTheGuessAndBackToPlay() {
        // Battle 1, player 0 attacks: Field Berserker, Flank Siege Cannon; player 1 Field Trickster, Flank Saboteur.
        // Player 1's hand keeps General (Hostages) and the Bodyguard, and draws 2 of Assassin, Scout, Berserker
        // (the top of the deck).
        stack(state, 0, BERSERKER, SIEGE_CANNON, ASSASSIN, SCOUT);
        stack(state, 1, TRICKSTER, SABOTEUR, GENERAL_ONE, BODYGUARD, ASSASSIN, SCOUT, BERSERKER);
        play(BERSERKER, SIEGE_CANNON);
        play(TRICKSTER, SABOTEUR);

        assertEquals(POST_BATTLE, state.getGamePhase());
        assertTrue(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        // Player 1 has discarded the Trickster and the Saboteur: 8 - 2 = 6 guesses.
        assertEquals(guesses(ASSASSIN, SCOUT, BERSERKER, BODYGUARD, GENERAL_ONE, SIEGE_CANNON),
                new HashSet<>(fm.computeAvailableActions(state)));
        assertEquals(6, fm.computeAvailableActions(state).size());

        ToadCard bodyguard = inHand(state, 1, BODYGUARD);
        fm.next(state, new GuessCard(BODYGUARD));

        // only player 1's Bodyguard is shown to player 0; the hand is still 2 kept + 2 drawn = 4 cards
        int index = state.getPlayerHand(1).getComponents().indexOf(bodyguard);
        assertTrue(state.getPlayerHand(1).isComponentVisible(index, 0));
        assertEquals(1, visibleTo(state, 1, 0));
        assertEquals(4, state.getPlayerHand(1).getSize());
        // back to PLAY, the Defender (player 1) attacks Battle 2
        assertFalse(state.isActionInProgress());
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    /**
     * Battle 1 with no post-battle decision, so player 1 attacks Battle 2. Player 0 plays Berserker / Assassin
     * then Trickster / Saboteur, and draws Bodyguard, General (Hostages), General (Flags), Siege Cannon (Scout stays
     * at the bottom of the deck). Player 1 plays Trickster / Saboteur in Battle 1 and keeps the other two cards
     * of its opening hand for Battle 2.
     */
    private void battleOneThenPlayerOneAttacks(ToadCardType p1Field, ToadCardType p1Flank) {
        stack(state, 0, BERSERKER, ASSASSIN, TRICKSTER, SABOTEUR, BODYGUARD, GENERAL_ONE, GENERAL_TWO, SIEGE_CANNON, SCOUT);
        stack(state, 1, TRICKSTER, SABOTEUR, p1Field, p1Flank);
        play(BERSERKER, ASSASSIN);
        play(TRICKSTER, SABOTEUR);
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        play(p1Field, p1Flank);
        play(TRICKSTER, SABOTEUR);
    }

    @Test
    public void siegeCannonOwnerGuessesWhenPlayerOneAttacks() {
        // Battle 2: player 1 attacks with Field General (Hostages), Flank Siege Cannon; player 0 defends with
        // Trickster / Saboteur. Player 1 owns the Cannon, so player 1 guesses.
        battleOneThenPlayerOneAttacks(GENERAL_ONE, SIEGE_CANNON);
        assertEquals(POST_BATTLE, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        // Player 0 has discarded Berserker, Assassin, Trickster, Saboteur: 8 - 4 = 4 guesses.
        assertEquals(guesses(SCOUT, BODYGUARD, GENERAL_ONE, SIEGE_CANNON), new HashSet<>(fm.computeAvailableActions(state)));

        // Player 0's hand is Bodyguard and both Generals and the Siege Cannon (the Scout was not drawn).
        // A General guess shows exactly one card, a General, to player 1.
        fm.next(state, new GuessCard(GENERAL_ONE));
        assertEquals(1, visibleTo(state, 0, 1));
        List<ToadCard> hand0 = state.getPlayerHand(0).getComponents();
        for (int i = 0; i < hand0.size(); i++)
            if (state.getPlayerHand(0).isComponentVisible(i, 1))
                assertTrue(hand0.get(i).type == GENERAL_ONE || hand0.get(i).type == GENERAL_TWO);
        // the Defender of Battle 2 (player 0) attacks Battle 3
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void scoutOwnersOpponentShowsCardsWhenPlayerOneAttacks() {
        // Battle 2: player 1 attacks with Field General (Hostages), Flank Scout. Player 1 scouts, so player 0
        // chooses which 3 of its 4 cards (Bodyguard, both Generals, Siege Cannon - all different) to show to player 1.
        battleOneThenPlayerOneAttacks(GENERAL_ONE, SCOUT);
        assertEquals(POST_BATTLE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, visibleTo(state, 0, 1));
        List<AbstractAction> options = fm.computeAvailableActions(state);
        assertEquals(4, options.size());
        assertTrue(options.stream().allMatch(a -> a instanceof ShowCards));

        fm.next(state, options.get(0));
        // player 1 sees 4 - 1 = 3 of player 0's cards; player 0 sees nothing new of player 1's hand
        assertEquals(3, visibleTo(state, 0, 1));
        assertEquals(0, visibleTo(state, 1, 0));
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void scoutRevealComesBeforeTheSiegeCannonGuess() {
        // Battle 1: player 0 attacks with Field Berserker, Flank Siege Cannon; player 1 defends with Field Trickster,
        // Flank Scout. The Scout (START) resolves before the Cannon (AFTER), so player 0 first chooses the cards to
        // show player 1 (ShowCards), and only then guesses (GuessCard). (Pushed onto the stack in list order, the guess would come first.)
        stack(state, 0, BERSERKER, SIEGE_CANNON, ASSASSIN, BODYGUARD);
        stack(state, 1, TRICKSTER, SCOUT, ASSASSIN, BODYGUARD);
        play(BERSERKER, SIEGE_CANNON);
        play(TRICKSTER, SCOUT);

        assertEquals(POST_BATTLE, state.getGamePhase());
        assertEquals(2, state.getActionsInProgress().size());
        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> first = fm.computeAvailableActions(state);
        assertTrue(first.stream().allMatch(a -> a instanceof ShowCards));
        fm.next(state, first.get(0));

        assertEquals(POST_BATTLE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> second = fm.computeAvailableActions(state);
        assertTrue(second.stream().allMatch(a -> a instanceof GuessCard));
        fm.next(state, new GuessCard(ASSASSIN));

        assertFalse(state.isActionInProgress());
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }
}

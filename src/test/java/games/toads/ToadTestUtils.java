package games.toads;

import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.toads.ToadConstants.ToadCardType;
import games.toads.abilities.*;
import games.toads.actions.PlayFieldCard;
import games.toads.actions.PlayFlankCard;
import games.toads.components.ToadCard;
import utilities.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared factories and arrange helpers for the War of the Toads tests.
 */
class ToadTestUtils {

    /**
     * Parameters with Tactics on, the recycle (discard) option and the opening return off (4-card deals, PLAY at
     * once), and the War 1 winner attacking first in War 2; seeded.
     */
    static ToadParameters tacticsParams(long seed) {
        ToadParameters params = new ToadParameters();
        params.setRandomSeed(seed);
        params.setParameterValue("useTactics", true);
        params.setParameterValue("discardOption", false);
        params.setParameterValue("openingReturn", false);
        params.setParameterValue("secondRoundStart", ToadParameters.SecondRoundStart.WINNER);
        return params;
    }

    /** A 2-player state set up by the forward model (fm.setup) with the given parameters. */
    static ToadGameState newState(ToadParameters params, ToadForwardModel fm) {
        ToadGameState state = new ToadGameState(params, 2);
        fm.setup(state);
        return state;
    }

    /** A card with no ability of any kind (no Tactics and no CardModifiers). */
    static ToadCard plain(String name, int value, ToadCardType type) {
        return new ToadCard(name, value, type, new NoAbility());
    }

    // Rulebook 3 cards, with the ability given explicitly (so independent of ToadCardType.defaultAbility)
    static ToadCard assassinII() { return new ToadCard("Assassin", 1, ToadCardType.ASSASSIN, new AssassinII()); }
    static ToadCard scout() { return new ToadCard("Scout", 2, ToadCardType.SCOUT, new Scout()); }
    static ToadCard saboteurIII() { return new ToadCard("Saboteur", 3, ToadCardType.SABOTEUR, new SaboteurIII()); }
    static ToadCard tricksterII() { return new ToadCard("Trickster", 4, ToadCardType.TRICKSTER, new TricksterII()); }
    static ToadCard berserkerII() { return new ToadCard("Berserker", 5, ToadCardType.BERSERKER, new BerserkerII()); }
    static ToadCard bodyguard() { return new ToadCard("Bodyguard", 6, ToadCardType.BODYGUARD, new Bodyguard()); }
    static ToadCard generalHostages() { return new ToadCard("General One", 7, ToadCardType.GENERAL_ONE, new GeneralHostages()); }
    static ToadCard generalFlags() { return new ToadCard("General Two", 7, ToadCardType.GENERAL_TWO, new GeneralFlags()); }
    static ToadCard siegeCannon() { return new ToadCard("Siege Cannon", 0, ToadCardType.SIEGE_CANNON, new SiegeCannon()); }

    /** An untyped card with no ability. */
    static ToadCard plain(int value) {
        return plain("Plain" + value, value, null);
    }

    /** A card whose (blockable) ability has the given Tactics, each a (priority, effect) pair. */
    @SafeVarargs
    static ToadCard withTactics(String name, int value, ToadCardType type, Pair<Integer, ToadAbility.BattleEffect>... tactics) {
        return new ToadCard(name, value, type, ability(true, tactics));
    }

    /** As withTactics, but the ability returns canBeBlocked() == false. */
    @SafeVarargs
    static ToadCard unblockable(String name, int value, ToadCardType type, Pair<Integer, ToadAbility.BattleEffect>... tactics) {
        return new ToadCard(name, value, type, ability(false, tactics));
    }

    /** An ability with the given Tactics and no CardModifiers. */
    @SafeVarargs
    static ToadAbility ability(boolean blockable, Pair<Integer, ToadAbility.BattleEffect>... tactics) {
        List<Pair<Integer, ToadAbility.BattleEffect>> list = List.of(tactics);
        return new ToadAbility() {
            @Override
            public List<Pair<Integer, BattleEffect>> tactics() {
                return list;
            }

            @Override
            public boolean canBeBlocked() {
                return blockable;
            }
        };
    }

    /** Shorthand for a (priority, effect) Tactic pair. */
    static Pair<Integer, ToadAbility.BattleEffect> tactic(int priority, ToadAbility.BattleEffect effect) {
        return new Pair<>(priority, effect);
    }

    /** A Tactic effect that adds the amount to this card's Ally (own card in the other lane). */
    static ToadAbility.BattleEffect addToAlly(double amount) {
        return (isAttacker, isFlank, br) -> br.addValue(isAttacker, !isFlank, amount);
    }

    /** A Tactic effect that adds the amount to this card itself. */
    static ToadAbility.BattleEffect addToSelf(double amount) {
        return (isAttacker, isFlank, br) -> br.addValue(isAttacker, isFlank, amount);
    }

    /** A Tactic effect that blocks the opposing hidden (flank) card. */
    static ToadAbility.BattleEffect blockOpposingFlank() {
        return (isAttacker, isFlank, br) -> br.block(!isAttacker, true);
    }

    /** A Tactic effect that gives this card's Ally the tie-break flag. */
    static ToadAbility.BattleEffect tieBreakAlly() {
        return (isAttacker, isFlank, br) -> br.setTieBreak(isAttacker, !isFlank);
    }

    /**
     * Builds a battle with player 0 as the Attacker and returns it, not yet calculated.
     * Argument order is lane by lane: attacker Field, defender Field, attacker Flank, defender Flank.
     */
    static BattleResult battle(ToadGameState state, ToadCard aField, ToadCard dField, ToadCard aFlank, ToadCard dFlank) {
        return new BattleResult(state, 0, aField, dField, aFlank, dFlank);
    }

    /**
     * Rearranges a player's own cards (hand + deck, moved, so conserving): the hand gets the cards of the first
     * types listed (as many as it held), in that order, and the deck gets the rest, top first; cards of types not
     * listed go to the bottom of the deck in their current order. Each type must be one of the player's cards.
     */
    static void stack(ToadGameState state, int player, ToadCardType... order) {
        PartialObservableDeck<ToadCard> hand = state.getPlayerHand(player);
        PartialObservableDeck<ToadCard> deck = state.getPlayerDeck(player);
        int handSize = hand.getSize();
        List<ToadCard> all = new ArrayList<>(hand.getComponents());
        all.addAll(deck.getComponents());
        hand.clear();
        deck.clear();
        List<ToadCard> ordered = new ArrayList<>();
        for (ToadCardType type : order) {
            ToadCard card = all.stream().filter(c -> c.type == type).findFirst()
                    .orElseThrow(() -> new AssertionError("Player " + player + " has no " + type));
            all.remove(card);
            ordered.add(card);
        }
        ordered.addAll(all);
        for (int i = 0; i < ordered.size(); i++) {
            if (i < handSize) hand.addToBottom(ordered.get(i));
            else deck.addToBottom(ordered.get(i));
        }
    }

    /** Replaces a player's hand with the given cards, in order (not conserving: the old hand is dropped). */
    static void setHand(ToadGameState state, int player, ToadCard... cards) {
        PartialObservableDeck<ToadCard> hand = state.getPlayerHand(player);
        hand.clear();
        for (ToadCard card : cards)
            hand.addToBottom(card);
    }

    /** The first card of the given type in the player's hand. */
    static ToadCard inHand(ToadGameState state, int player, ToadCardType type) {
        return state.getPlayerHand(player).stream().filter(c -> c.type == type).findFirst()
                .orElseThrow(() -> new AssertionError("Player " + player + " holds no " + type));
    }

    /** How many cards of the owner's hand the viewer can see. */
    static int visibleTo(ToadGameState state, int owner, int viewer) {
        PartialObservableDeck<ToadCard> hand = state.getPlayerHand(owner);
        int count = 0;
        for (int i = 0; i < hand.getSize(); i++)
            if (hand.isComponentVisible(i, viewer)) count++;
        return count;
    }

    /**
     * Plays the cards through fm.next: the current player plays cards[0] to the Field and cards[1] to the Flank,
     * then the next player cards[2] and cards[3]. Each card is added to the player's hand just before it is played
     * (so this adds components rather than moving them - as the legacy Tactics.playCards does).
     * Returns the actions taken.
     */
    static List<AbstractAction> playCards(ToadGameState state, ToadForwardModel fm, ToadCard... cardsInOrder) {
        List<AbstractAction> taken = new ArrayList<>();
        for (int i = 0; i < cardsInOrder.length; i++) {
            state.getPlayerHand(state.getCurrentPlayer()).add(cardsInOrder[i]);
            AbstractAction action = i % 2 == 0 ? new PlayFieldCard(cardsInOrder[i]) : new PlayFlankCard(cardsInOrder[i]);
            fm.next(state, action);
            taken.add(action);
        }
        return taken;
    }
}

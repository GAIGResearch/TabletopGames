package games.blackjack;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IGamePhase;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;

/**
 * <p>Game state for Blackjack. All initialisation and rule logic lives in {@link BlackjackForwardModel}; the public
 * methods that change the state only move chips and cards.
 * The dealer is part of the game (the bank), not a player: every player plays against the dealer, whose play is
 * fixed by the rules and carried out by the forward model.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>drawDeck - the face-down draw deck, HIDDEN_TO_ALL</li>
 *     <li>dealerHand - the dealer's face-up cards, VISIBLE_TO_ALL</li>
 *     <li>holeCard - the dealer's face-down card (0 or 1 cards), HIDDEN_TO_ALL until it is turned up and moved to
 *     dealerHand</li>
 *     <li>playerHands - for each player, their hands in the order they are played. There is one hand unless the player
 *     splits. Player cards are dealt face up, so VISIBLE_TO_ALL</li>
 *     <li>bets - for each player, the chips bet on each of their hands (parallel to playerHands). 0 on a player's only
 *     hand means they are not in the current hand (they have too few chips to bet)</li>
 *     <li>chips - the chips each player holds, not counting those bet on the hand in progress</li>
 *     <li>insurance - the chips each player has paid for insurance on the hand in progress (0 if none)</li>
 *     <li>activeHand - the index of the hand the current player is playing (only valid in the Play phase)</li>
 * </ul>
 */
public class BlackjackGameState extends AbstractGameState {

    /**
     * Betting: each player in turn places a bet. Insurance: each player in turn may insure against a dealer
     * Blackjack. Play: each player in turn plays out their hand(s). The dealer's own play and the settlement of
     * bets follow automatically when the last player finishes.
     */
    public enum BlackjackGamePhase implements IGamePhase {
        Betting, Insurance, Play
    }

    public static final int BLACKJACK = 21;

    Deck<FrenchCard> drawDeck;
    Deck<FrenchCard> dealerHand;
    Deck<FrenchCard> holeCard;
    List<List<Deck<FrenchCard>>> playerHands;
    List<List<Integer>> bets;
    int[] chips;
    int[] insurance;
    int activeHand;

    public BlackjackGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Blackjack;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.add(drawDeck);
        components.add(dealerHand);
        components.add(holeCard);
        for (List<Deck<FrenchCard>> hands : playerHands)
            components.addAll(hands);
        return components;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getDealerHand() {
        return dealerHand;
    }

    public Deck<FrenchCard> getHoleCard() {
        return holeCard;
    }

    public List<Deck<FrenchCard>> getPlayerHands(int playerId) {
        return playerHands.get(playerId);
    }

    public Deck<FrenchCard> getPlayerHand(int playerId, int hand) {
        return playerHands.get(playerId).get(hand);
    }

    public int getBet(int playerId, int hand) {
        return bets.get(playerId).get(hand);
    }

    public int getChips(int playerId) {
        return chips[playerId];
    }

    public int getInsurance(int playerId) {
        return insurance[playerId];
    }

    public int getActiveHand() {
        return activeHand;
    }

    /**
     * Deal cards from the draw deck.
     * If the draw deck is empty, a fresh 52-card pack shuffled with getRnd() is first added to it.
     */
    public FrenchCard drawCard() {
        if (drawDeck.getSize() == 0) {
            Deck<FrenchCard> pack = FrenchCard.generateDeck("Pack", HIDDEN_TO_ALL);
            pack.shuffle(getRnd());
            drawDeck.add(pack);
        }
        return drawDeck.draw();
    }

    /**
     * Split the player's hand into two. The second card of the pair moves to a new hand, which is placed immediately
     * after the hand being split. The new hand has the same bet as the original, taken from the player's chips.
     */
    public void splitHand(int playerId, int hand) {
        Deck<FrenchCard> newHand = new Deck<>("Hand " + playerId, playerId, VISIBLE_TO_ALL);
        newHand.add(getPlayerHand(playerId, hand).draw());
        playerHands.get(playerId).add(hand + 1, newHand);
        int bet = getBet(playerId, hand);
        bets.get(playerId).add(hand + 1, bet);
        chips[playerId] -= bet;
    }

    /**
     * Move chips from the player's chips to the bet on their first hand.
     */
    public void placeBet(int playerId, int amount) {
        chips[playerId] -= amount;
        bets.get(playerId).set(0, amount);
    }

    /**
     * Move half the player's bet from their chips to their insurance.
     */
    public void buyInsurance(int playerId) {
        int cost = getBet(playerId, 0) / 2;
        chips[playerId] -= cost;
        insurance[playerId] = cost;
    }

    /**
     * Double the bet on one of the player's hands. Creates a second bet equal to the first.
     */
    public void doubleBet(int playerId, int hand) {
        int bet = getBet(playerId, hand);
        chips[playerId] -= bet;
        bets.get(playerId).set(hand, 2 * bet);
    }

    /**
     * The dealer's up card: the first card dealt to the dealer. Deck.add puts each later card on top, at index 0, so
     * the up card is the last card in dealerHand.
     */
    public FrenchCard getUpCard() {
        return dealerHand.get(dealerHand.getSize() - 1);
    }

    /**
     * The Blackjack value of one card: 2-10 at face value, court cards 10, and an Ace 1 (see {@link #handValue}
     * for when an Ace counts 11).
     */
    public static int cardValue(FrenchCard card) {
        // FrenchCard numbers Aces as 14 (and court cards 11-13), so only Number cards can use card.number
        return switch (card.type) {
            case Number -> card.number;
            case Jack, Queen, King -> 10;
            case Ace -> 1;
        };
    }

    /**
     * The total value of a hand: one Ace counts 11 if that does not take the total over 21, every other Ace counts 1.
     */
    public static int handValue(List<FrenchCard> cards) {
        int total = 0;
        boolean hasAce = false;
        for (FrenchCard card : cards) {
            total += cardValue(card);
            hasAce |= card.type == FrenchCard.FrenchCardType.Ace;
        }
        return hasAce && total + 10 <= BLACKJACK ? total + 10 : total;
    }

    /**
     * The total value of the cards in a hand, as {@link #handValue(List)}.
     */
    public static int handValue(Deck<FrenchCard> hand) {
        return handValue(hand.getComponents());
    }

    /**
     * A hand is soft if it holds an Ace counted as 11.
     */
    public static boolean isSoft(List<FrenchCard> cards) {
        int hard = 0;
        for (FrenchCard card : cards)
            hard += cardValue(card);
        return handValue(cards) != hard;
    }

    /**
     * A ten-value card: a 10 or a court card.
     */
    public static boolean isTenValue(FrenchCard card) {
        return cardValue(card) == 10;
    }

    /**
     * A natural (Blackjack): the player's hand consists of two cards with a total value of 21 (an Ace and a ten-value
     * card).
     * A 21 on a hand made by splitting is not a natural.
     */
    public boolean isNatural(int playerId, int hand) {
        List<FrenchCard> cards = getPlayerHand(playerId, hand).getComponents();
        return playerHands.get(playerId).size() == 1 && cards.size() == 2 && handValue(cards) == BLACKJACK;
    }

    /**
     * The dealer has Blackjack: the up card and the hole card (face down or turned up) are an Ace and a ten-value
     * card.
     */
    public boolean dealerHasBlackjack() {
        List<FrenchCard> cards = new ArrayList<>(dealerHand.getComponents());
        cards.addAll(holeCard.getComponents());
        return cards.size() == 2 && handValue(cards) == BLACKJACK;
    }

    @Override
    protected BlackjackGameState _copy(int playerId) {
        BlackjackGameState copy = new BlackjackGameState(gameParameters, getNPlayers());
        copy.drawDeck = drawDeck.copy();
        copy.dealerHand = dealerHand.copy();
        copy.holeCard = holeCard.copy();
        copy.playerHands = new ArrayList<>();
        for (List<Deck<FrenchCard>> hands : playerHands) {
            List<Deck<FrenchCard>> handsCopy = new ArrayList<>();
            for (Deck<FrenchCard> hand : hands)
                handsCopy.add(hand.copy());
            copy.playerHands.add(handsCopy);
        }
        copy.bets = new ArrayList<>();
        for (List<Integer> playerBets : bets)
            copy.bets.add(new ArrayList<>(playerBets));
        copy.chips = chips.clone();
        copy.insurance = insurance.clone();
        copy.activeHand = activeHand;

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // The only hidden cards are the hole card and the draw deck. Once play has started with the hole card still
            // face down, the dealer does not have Blackjack (a dealer Blackjack ends the hand before play), so the hole
            // card cannot be one that makes 21 with the up card.
            BiPredicate<Deck<FrenchCard>, FrenchCard> permitted = (deck, card) -> true;
            if (getGamePhase() == BlackjackGamePhase.Play && holeCard.getSize() == 1) {
                FrenchCard up = getUpCard();
                permitted = (deck, card) -> deck != copy.holeCard || handValue(List.of(up, card)) != BLACKJACK;
            }
            DeterminisationUtilities.reshuffle(playerId, List.of(copy.drawDeck, copy.holeCard), c -> true,
                    redeterminisationRnd, permitted);
        }
        return copy;
    }

    /**
     * The player's chips as a fraction of the most they could hold at the end of the game
     * (BlackjackParameters.maxChips), so in [0, 1].
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        return chips[playerId] / (double) ((BlackjackParameters) gameParameters).maxChips();
    }

    /**
     * The chips the player holds.
     */
    @Override
    public double getGameScore(int playerId) {
        return chips[playerId];
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlackjackGameState that)) return false;
        return Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(dealerHand, that.dealerHand) &&
                Objects.equals(holeCard, that.holeCard) &&
                Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(bets, that.bets) &&
                Arrays.equals(chips, that.chips) &&
                Arrays.equals(insurance, that.insurance) &&
                activeHand == that.activeHand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), drawDeck, dealerHand, holeCard, playerHands, bets, activeHand)
                + 31 * Arrays.hashCode(chips) + 997 * Arrays.hashCode(insurance);
    }
}

package games.blackjack;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.blackjack.BlackjackGameState.BlackjackGamePhase;
import games.blackjack.actions.Bet;
import games.blackjack.actions.DoubleDown;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Insurance;
import games.blackjack.actions.Split;
import games.blackjack.actions.Stand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

import static core.CoreConstants.GameResult.*;
import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static games.blackjack.BlackjackGameState.BLACKJACK;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Betting;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackGameState.handValue;

/**
 * <p>The forward model contains all the game rules and logic for Blackjack, including the dealer's play.</p>
 */
public class BlackjackForwardModel extends StandardForwardModel {

    // the dealer stands on a total of 17 or more (but see BlackjackParameters.dealerHitsSoft17)
    static final int DEALER_STANDS = 17;

    /**
     * Each player starts with their chips and an empty hand. The deck is shuffled face down.
     * No cards are dealt until everyone has bet, so the game starts in the Betting phase with player 0.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        BlackjackGameState state = (BlackjackGameState) firstState;
        BlackjackParameters params = (BlackjackParameters) state.getGameParameters();

        state.drawDeck = FrenchCard.generateDeck("DrawDeck", HIDDEN_TO_ALL);
        state.drawDeck.shuffle(state.getRnd());
        state.dealerHand = new Deck<>("DealerHand", VISIBLE_TO_ALL);
        state.holeCard = new Deck<>("HoleCard", HIDDEN_TO_ALL);
        state.playerHands = new ArrayList<>();
        state.bets = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++) {
            List<Deck<FrenchCard>> hands = new ArrayList<>();
            hands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_ALL));
            state.playerHands.add(hands);
            List<Integer> playerBets = new ArrayList<>();
            playerBets.add(0);
            state.bets.add(playerBets);
        }
        state.chips = new int[state.getNPlayers()];
        Arrays.fill(state.chips, params.startingChips);
        state.insurance = new int[state.getNPlayers()];
        state.activeHand = 0;

        state.setGamePhase(Betting);
        state.setFirstPlayer(0);
    }

    /**
     * Betting: every even amount from minBet to the smaller of maxBet and the player's chips.
     * Insurance: buy or decline insurance.
     * Play: Hit or Stand on the active hand. If the player has the chips to match the hand's bet, also DoubleDown
     * (with BlackjackParameters.doubleDown) on a first two cards that are not a natural and not from a split, and
     * Split (with BlackjackParameters.splitting) on a pair of the same rank while the player has fewer than
     * maxHandsAfterSplit hands.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        BlackjackGameState state = (BlackjackGameState) gameState;
        BlackjackParameters params = (BlackjackParameters) state.getGameParameters();
        List<AbstractAction> actions = new ArrayList<>();
        switch ((BlackjackGamePhase) state.getGamePhase()) {
            case Betting -> {
                int most = Math.min(params.maxBet, state.chips[state.getCurrentPlayer()]);
                for (int amount = params.minBet; amount <= most; amount += 2)
                    actions.add(new Bet(amount));
            }
            case Insurance -> {
                actions.add(new Insurance(true));
                actions.add(new Insurance(false));
            }
            case Play -> {
                actions.add(new Hit());
                actions.add(new Stand());
                int player = state.getCurrentPlayer();
                int hand = state.activeHand;
                List<FrenchCard> cards = state.getPlayerHand(player, hand).getComponents();
                boolean canMatchBet = state.getChips(player) >= state.getBet(player, hand);
                int nHands = state.getPlayerHands(player).size();
                if (params.doubleDown && cards.size() == 2 && nHands == 1 && !state.isNatural(player, hand) && canMatchBet)
                    actions.add(new DoubleDown());
                if (params.splitting && cards.size() == 2 && cards.get(0).type == cards.get(1).type
                        && cards.get(0).number == cards.get(1).number && nHands < params.maxHandsAfterSplit && canMatchBet)
                    actions.add(new Split());
            }
            default -> throw new AssertionError("Unexpected phase " + state.getGamePhase());
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        BlackjackGameState state = (BlackjackGameState) currentState;
        int player = state.getCurrentPlayer();
        if (actionTaken instanceof Bet) {
            passTurnOrElse(state, nextToBet(state, player + 1), () -> {
                deal(state);
                afterDeal(state);
            });
        } else if (actionTaken instanceof Insurance) {
            passTurnOrElse(state, nextToInsure(state, player + 1), () -> startPlay(state));
        } else if (actionTaken instanceof Split) {
            // split Aces take one card each, and both hands are finished
            Deck<FrenchCard> hand = state.getPlayerHand(player, state.activeHand);
            if (hand.get(hand.getSize() - 1).type == FrenchCard.FrenchCardType.Ace)
                handFinished(state, player, 2);
        } else if (actionTaken instanceof Stand || actionTaken instanceof DoubleDown ||
                handValue(state.getPlayerHand(player, state.activeHand)) > BLACKJACK) {
            handFinished(state, player, 1);
        }
        // otherwise (a Hit that is not bust, or a split of anything but Aces) the same hand continues
    }

    /**
     * Called when the current player finishes the hand they are playing. {@code finished} is the number of hands
     * finished: 1, or 2 after splitting Aces (both hands are then complete). The player goes on to their next hand if
     * they have one; otherwise the next player plays, or after the last player the dealer plays and the hand ends.
     */
    private void handFinished(BlackjackGameState state, int player, int finished) {
        state.activeHand += finished;
        if (state.activeHand < state.getPlayerHands(player).size())
            return;
        state.activeHand = 0;
        passTurnOrElse(state, nextToPlay(state, player + 1), () -> dealerPlaysAndHandEnds(state));
    }

    /**
     * Deal to every player with a bet: one card face up to each, then the dealer's up card, then a second
     * card to each, then the dealer's hole card face down.
     */
    private void deal(BlackjackGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++)
            if (state.getBet(p, 0) > 0)
                state.getPlayerHand(p, 0).add(state.drawCard());
        state.dealerHand.add(state.drawCard());
        for (int p = 0; p < state.getNPlayers(); p++)
            if (state.getBet(p, 0) > 0)
                state.getPlayerHand(p, 0).add(state.drawCard());
        state.holeCard.add(state.drawCard());
    }

    /**
     * Under an Ace or ten-value up card, each player who can afford it is offered insurance in turn.
     * Otherwise play starts at once.
     */
    private void afterDeal(BlackjackGameState state) {
        FrenchCard up = state.getUpCard();
        boolean insuranceOffered = up.type == FrenchCard.FrenchCardType.Ace || BlackjackGameState.isTenValue(up);
        int first = nextToInsure(state, 0);
        if (insuranceOffered && first < state.getNPlayers()) {
            state.setGamePhase(BlackjackGamePhase.Insurance);
            endPlayerTurn(state, first);
        } else {
            startPlay(state);
        }
    }

    /**
     * Once any insurance has been bought: a dealer Blackjack ends the hand before anyone plays, paying each insurance
     * 2:1 (the stake back and twice as much again). Otherwise all insurance is lost; with
     * BlackjackParameters.payout21NaturalOnly every natural is paid at once and takes no further part; and play
     * starts.
     */
    private void startPlay(BlackjackGameState state) {
        BlackjackParameters params = (BlackjackParameters) state.getGameParameters();
        state.setGamePhase(Play);
        boolean dealerBlackjack = state.dealerHasBlackjack();
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (dealerBlackjack)
                state.chips[p] += 3 * state.insurance[p];
            state.insurance[p] = 0;
        }
        if (dealerBlackjack) {
            state.dealerHand.add(state.holeCard.draw());
            settle(state);
            endHand(state);
            return;
        }
        if (params.payout21NaturalOnly) {
            for (int p = 0; p < state.getNPlayers(); p++) {
                if (state.isNatural(p, 0)) {
                    int bet = state.getBet(p, 0);
                    state.chips[p] += bet + params.winningsOn21(bet);
                    state.bets.get(p).set(0, 0);
                }
            }
        }
        passTurnOrElse(state, nextToPlay(state, 0), () -> dealerPlaysAndHandEnds(state));
    }

    /**
     * Give the turn to {@code next}, or if that is nPlayers (no player is left to act at this stage) run
     * {@code otherwise}.
     */
    private void passTurnOrElse(BlackjackGameState state, int next, Runnable otherwise) {
        if (next < state.getNPlayers())
            endPlayerTurn(state, next);
        else
            otherwise.run();
    }

    /**
     * The first player from {@code from} onwards who satisfies {@code eligible}, or nPlayers if there is none.
     */
    private static int firstPlayerFrom(BlackjackGameState state, int from, IntPredicate eligible) {
        for (int p = from; p < state.getNPlayers(); p++)
            if (eligible.test(p))
                return p;
        return state.getNPlayers();
    }

    /**
     * Returns the player id who bets next: the first from {@code from} with the chips for the minimum bet (a player
     * with fewer sits the hand out), or nPlayers if there is none.
     */
    private static int nextToBet(BlackjackGameState state, int from) {
        int minBet = ((BlackjackParameters) state.getGameParameters()).minBet;
        return firstPlayerFrom(state, from, p -> state.getChips(p) >= minBet);
    }

    /**
     * Returns the player id who considers insurance next.
     */
    private static int nextToInsure(BlackjackGameState state, int from) {
        return firstPlayerFrom(state, from, p -> state.getBet(p, 0) > 0 && state.getChips(p) >= state.getBet(p, 0) / 2);
    }

    /**
     * Returns the player id who plays next: the first from {@code from} with a hand still to play (a bet on it).
     */
    private static int nextToPlay(BlackjackGameState state, int from) {
        return firstPlayerFrom(state, from, p -> state.getBet(p, 0) > 0);
    }

    private void dealerPlaysAndHandEnds(BlackjackGameState state) {
        dealerPlays(state);
        settle(state);
        endHand(state);
    }

    /**
     * The next hand starts (unless this was the last one) if anyone can still make
     * the minimum bet: every card is gathered into the draw deck and shuffled, and betting starts again.
     * Otherwise the game ends.
     */
    private void endHand(BlackjackGameState state) {
        int first = nextToBet(state, 0);
        if (state.getRoundCounter() + 1 >= ((BlackjackParameters) state.getGameParameters()).nHands
                || first == state.getNPlayers()) {
            endGame(state);
            return;
        }
        for (int p = 0; p < state.getNPlayers(); p++) {
            List<Deck<FrenchCard>> hands = state.playerHands.get(p);
            for (Deck<FrenchCard> hand : hands)
                gather(state, hand);
            // split hands go, leaving each player one empty hand
            hands.subList(1, hands.size()).clear();
            state.bets.get(p).subList(1, state.bets.get(p).size()).clear();
        }
        gather(state, state.dealerHand);
        gather(state, state.holeCard);
        state.drawDeck.shuffle(state.getRnd());
        state.activeHand = 0;
        state.setGamePhase(Betting);
        endRound(state, first);
    }

    private static void gather(BlackjackGameState state, Deck<FrenchCard> deck) {
        state.drawDeck.add(deck);
        deck.clear();
    }

    /**
     * The dealer turns the hole card up and draws while under 17, standing on 17 or more - except a soft 17 with
     * BlackjackParameters.dealerHitsSoft17.
     * If every player hand is bust or already paid, the results are decided and the dealer does not draw.
     */
    private void dealerPlays(BlackjackGameState state) {
        state.dealerHand.add(state.holeCard.draw());
        boolean anyHandLive = false;
        for (int p = 0; p < state.getNPlayers(); p++)
            for (int h = 0; h < state.getPlayerHands(p).size(); h++)
                anyHandLive |= state.getBet(p, h) > 0 && handValue(state.getPlayerHand(p, h)) <= BLACKJACK;
        if (!anyHandLive)
            return;
        boolean hitsSoft17 = ((BlackjackParameters) state.getGameParameters()).dealerHitsSoft17;
        while (true) {
            int total = handValue(state.dealerHand);
            boolean soft = BlackjackGameState.isSoft(state.dealerHand.getComponents());
            if (total > DEALER_STANDS || total == DEALER_STANDS && !(hitsSoft17 && soft))
                break;
            state.dealerHand.add(state.drawCard());
        }
    }

    /**
     * Settle every bet against the dealer. A bust hand loses. A dealer Blackjack beats every hand except a natural,
     * which pushes. Otherwise a dealer bust or a higher total wins 1:1, or BlackjackParameters.payout21 for a 21 (with
     * BlackjackParameters.payout21NaturalOnly only a natural, and those have already been paid); an equal total is a
     * push (the bet back); a lower total loses.
     */
    private void settle(BlackjackGameState state) {
        BlackjackParameters params = (BlackjackParameters) state.getGameParameters();
        int dealer = handValue(state.dealerHand);
        boolean dealerBlackjack = state.dealerHasBlackjack();
        for (int p = 0; p < state.getNPlayers(); p++) {
            for (int h = 0; h < state.getPlayerHands(p).size(); h++) {
                int total = handValue(state.getPlayerHand(p, h));
                int bet = state.getBet(p, h);
                if (total > BLACKJACK) {
                    // bust: the bet is lost
                } else if (dealerBlackjack) {
                    if (state.isNatural(p, h))
                        state.chips[p] += bet;
                } else if (dealer > BLACKJACK || total > dealer) {
                    boolean pays21 = total == BLACKJACK && (!params.payout21NaturalOnly || state.isNatural(p, h));
                    state.chips[p] += bet + (pays21 ? params.winningsOn21(bet) : bet);
                } else if (total == dealer) {
                    state.chips[p] += bet;
                }
                state.bets.get(p).set(h, 0);
            }
        }
    }

    /**
     * Each player plays against the bank, not the other players: WIN_GAME with more chips than they started with,
     * DRAW_GAME with the same, LOSE_GAME with fewer.
     */
    @Override
    protected void endGame(AbstractGameState gs) {
        BlackjackGameState state = (BlackjackGameState) gs;
        int start = ((BlackjackParameters) state.getGameParameters()).startingChips;
        state.setGameStatus(GAME_END);
        for (int p = 0; p < state.getNPlayers(); p++) {
            int chips = state.getChips(p);
            state.setPlayerResult(chips > start ? WIN_GAME : chips == start ? DRAW_GAME : LOSE_GAME, p);
        }
    }
}

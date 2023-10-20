package games.poker;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import core.components.FrenchCard;
import games.poker.actions.*;
import games.poker.actions.Fold;
import games.poker.components.MoneyPot;
import utilities.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static games.poker.PokerGameState.PokerGamePhase.*;
import static core.CoreConstants.GameResult.LOSE_GAME;


public class PokerForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        PokerGameState pgs = (PokerGameState) firstState;
        PokerGameParameters params = (PokerGameParameters) firstState.getGameParameters();

        pgs.playerMoney = new Counter[firstState.getNPlayers()];
        pgs.playerNeedsToCall = new boolean[firstState.getNPlayers()];
        pgs.playerFold = new boolean[firstState.getNPlayers()];
        pgs.playerBet = new Counter[firstState.getNPlayers()];
        pgs.playerActStreet = new boolean[pgs.getNPlayers()];
        pgs.moneyPots = new ArrayList<>();

        pgs.playerDecks = new ArrayList<>();
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            pgs.playerDecks.add(new Deck<>("Player " + i + " deck", i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            pgs.playerMoney[i] = new Counter(params.nStartingMoney, 0, Integer.MAX_VALUE, "Player " + i + " money");
            pgs.playerBet[i] = new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " money");
        }

        // Create the discard deck, at the beginning it is empty
        pgs.communityCards = new Deck<>("CommunityCards", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        // Player 0 starts the game
        pgs.setFirstPlayer(0);

        // Set up first round
        setupRound(pgs);
    }

    /**
     * Sets up a round for the game, including draw pile, discard deck and player decks, all reset.
     *
     * @param pgs - current game state.
     */
    private void setupRound(PokerGameState pgs) {
        PokerGameParameters params = (PokerGameParameters) pgs.getGameParameters();
        Random r = new Random(params.getRandomSeed() + pgs.getRoundCounter());

        pgs.moneyPots.clear();
        pgs.moneyPots.add(new MoneyPot());

        // Refresh player info
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            pgs.playerDecks.get(i).clear();
            pgs.playerNeedsToCall[i] = false;
            pgs.playerFold[i] = false;
            pgs.playerBet[i].setValue(0);
        }
        pgs.communityCards.clear();

        // Refresh draw deck and shuffle
        pgs.drawDeck = FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        pgs.drawDeck.shuffle(r);

        // Draw new cards for players
        drawCardsToPlayers(pgs);

        // Blinds
        int smallId = pgs.getFirstPlayer();
        int bigId = (pgs.getNPlayers() + smallId + 1) % pgs.getNPlayers();
        while ((pgs.playerFold[bigId] || pgs.getPlayerResults()[bigId] == LOSE_GAME)) {
            bigId = (pgs.getNPlayers() + bigId + 1) % pgs.getNPlayers();
        }
        if (pgs.playerMoney[smallId].getValue() < params.smallBlind) {
            new AllIn(smallId).execute(pgs);
        } else {
            new Bet(smallId, params.smallBlind).execute(pgs);
        }

        if (pgs.playerMoney[bigId].getValue() < params.bigBlind) {
            new AllIn(bigId).execute(pgs);
        } else {
            new Bet(bigId, params.bigBlind).execute(pgs);
        }

        pgs.setGamePhase(Preflop);
        pgs.setBet(false);
    }

    private void drawCardsToPlayers(PokerGameState pgs) {
        for (int player = 0; player < pgs.getNPlayers(); player++) {
            for (int card = 0; card < ((PokerGameParameters) pgs.getGameParameters()).nCardsPerPlayer; card++) {
                pgs.playerDecks.get(player).add(pgs.drawDeck.draw());
            }
        }
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        // Check end of street to add more community cards
  //      System.out.println("Executed action " + action.getString(gameState));
        PokerGameState pgs = (PokerGameState) gameState;
        PokerGameParameters pgp = (PokerGameParameters) gameState.getGameParameters();

        checkMoney(pgs);

        if (action instanceof Fold) {
            fold(pgs, ((Fold) action).playerId);
        }

        pgs.playerActStreet[pgs.getCurrentPlayer()] = true;
        boolean remainingDecisions = false;
        int stillAlive = 0;
        if ((action instanceof Fold || action instanceof Check || action instanceof Call)) {
            for (int i = 0; i < pgs.getNPlayers(); i++) {
                if (pgs.getPlayerResults()[i] != LOSE_GAME && !pgs.playerFold[i]) {
                    stillAlive++;
                    if (pgs.playerNeedsToCall[i] || !pgs.playerActStreet[i]) {
                        remainingDecisions = true;
                    }
                }
            }
            if (stillAlive == 1) {
                // Round is over
                roundEnd(pgs);
            } else if (!remainingDecisions) {
                // Add community cards
                pgs.setTurnOwner(pgs.getFirstPlayer());
                pgs.setBet(false);
                Arrays.fill(pgs.playerActStreet, false);

                if (pgs.getGamePhase() == Preflop) {
                    // Add flop
                    for (int i = 0; i < pgp.nFlopCards; i++) {
                        pgs.communityCards.add(pgs.drawDeck.draw());
                    }
                    pgs.setGamePhase(Flop);
                } else if (pgs.getGamePhase() == Flop) {
                    // Add turn
                    for (int i = 0; i < pgp.nTurnCards; i++) {
                        pgs.communityCards.add(pgs.drawDeck.draw());
                    }
                    pgs.setGamePhase(Turn);
                } else if (pgs.getGamePhase() == Turn) {
                    // Add river
                    for (int i = 0; i < pgp.nRiverCards; i++) {
                        pgs.communityCards.add(pgs.drawDeck.draw());
                    }
                    pgs.setGamePhase(River);
                } else if (pgs.getGamePhase() == River) {
                    // Round is over
                    roundEnd(pgs);
                }
            }
        }
        checkMoney(pgs);

        if (pgs.isNotTerminal())
            endPlayerTurn(pgs);
    }

    /**
     * Called when round is over. Calculate winner of round and distribute money.
     *
     * @param pgs - current game state
     */
    private void roundEnd(PokerGameState pgs) {
        // Calculate winner of round for each of the pots, they earn the money. Ties split money equally.

        Pair<Map<Integer, Integer>, Map<Integer, Set<Integer>>> translated = translatePokerHands(pgs);
        Map<Integer, Integer> ranks = translated.a;
        Map<Integer, Set<Integer>> hands = translated.b;

        for (MoneyPot pot : pgs.moneyPots) {
            // Calculate winners separately for each money pot
            Set<Integer> winners = getWinner(pgs, pot, ranks, hands);
            if (winners.isEmpty()) {
                // then we return to the participants their personal contribution
                for (int i : pot.getPlayerContribution().keySet()) {
                    pgs.playerMoney[i].increment(pot.getPlayerContribution(i));
                }
            } else {
                for (int i : winners) {
                    pgs.playerMoney[i].increment(pot.getValue() / winners.size());
                }
                // We may then have a rounding error, which we give to the first winner
                if (winners.size() > 1) {
                    int remaining = pot.getValue() % winners.size();
                    pgs.playerMoney[winners.iterator().next()].increment(remaining);
                }
            }
            // Then set pot to zero as we have transferred money
            pot.setValue(0);
        }

        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (pgs.playerMoney[i].isMinimum()) {
                // Player is out of the game
                pgs.setPlayerResult(LOSE_GAME, i);
            }
        }
        checkMoney(pgs);

        // Check if game is over
        if (checkGameEnd(pgs)) return;

        // End previous round
        endRound(pgs, (pgs.getCurrentPlayer() + 1) % pgs.getNPlayers());

        Arrays.fill(pgs.playerFold, false);

        // Reset cards for the new round
        setupRound(pgs);
    }

    private void checkMoney(PokerGameState pgs) {
        int personalMoney = Arrays.stream(pgs.playerMoney).mapToInt(Counter::getValue).sum();
        int potMoney = pgs.moneyPots.stream().mapToInt(MoneyPot::getValue).sum();
        int expectedTotal = pgs.getNPlayers() * ((PokerGameParameters) pgs.getGameParameters()).nStartingMoney;
    //    String potDetails = pgs.moneyPots.stream().map(MoneyPot::toString).collect(Collectors.joining(", "));
    //    String playerMoney = Arrays.stream(pgs.playerMoney).map(Counter::toString).collect(Collectors.joining(", "));
    //    System.out.println(potDetails + "\t" + playerMoney);
        if (personalMoney + potMoney != expectedTotal) {
            throw new AssertionError(String.format("Money is not conserved! %d + %d != %d", personalMoney, potMoney, expectedTotal));
        }
    }

    public Pair<Map<Integer, Integer>, Map<Integer, Set<Integer>>> translatePokerHands(PokerGameState pgs) {
        Map<Integer, Integer> ranks = new HashMap<>();
        Map<Integer, Set<Integer>> hands = new HashMap<>();
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (!pgs.playerFold[i] && pgs.getPlayerResults()[i] != LOSE_GAME) {
                Deck<FrenchCard> cardsToEvaluate = pgs.playerDecks.get(i).copy();
                cardsToEvaluate.add(pgs.communityCards.copy());
                Pair<PokerGameState.PokerHand, HashSet<Integer>> hand = PokerGameState.PokerHand.translateHand(cardsToEvaluate);
                if (hand != null) {
                    ranks.put(i, hand.a.rank);
                    hands.put(i, hand.b);
                }
            }
        }
        return new Pair<>(ranks, hands);
    }

    @SuppressWarnings("unchecked")
    public Set<Integer> getWinner(PokerGameState pgs, MoneyPot pot,
                                  Map<Integer, Integer> ranks, Map<Integer, Set<Integer>> hands) {
        // Calculate winners separately for each money pot
        Set<Integer> playersInPot = new HashSet<>(pot.getPlayerContribution().keySet());

        int smallestRank = 11;
        for (int i : playersInPot) {
            if (!pgs.playerFold[i] && pgs.getPlayerResults()[i] != LOSE_GAME && ranks.containsKey(i) && ranks.get(i) < smallestRank) {
                smallestRank = ranks.get(i);
            }
        }
        Set<Integer> winners = new HashSet<>();
        for (int i : playersInPot) {
            if (!pgs.playerFold[i] && pgs.getPlayerResults()[i] != LOSE_GAME) {
                if (ranks.get(i) == smallestRank) winners.add(i);
            }
        }
        if (winners.size() > 1) {
            // A tie in rank, check card values
            ArrayList<Integer>[] cardValues = new ArrayList[pgs.getNPlayers()];
            int nCards = 0;
            for (int i : winners) {
                cardValues[i] = new ArrayList<>(hands.get(i));
                cardValues[i].sort(Collections.reverseOrder());
                nCards = cardValues[i].size();
            }
            for (int j = 0; j < nCards; j++) {
                // Checking card by card, once one player is found the winner we break; could still be a tie
                int maxValue = 0;
                for (int i : winners) {
                    if (cardValues[i].get(j) > maxValue) maxValue = cardValues[i].get(j);
                }
                HashSet<Integer> actualWinners = new HashSet<>();
                for (int i : winners) {
                    if (cardValues[i].get(j) == maxValue) actualWinners.add(i);
                }
                if (actualWinners.size() == 1 || j == nCards - 1) {
                    return actualWinners;
                }
            }
        }
        return winners;
    }

    /**
     * Game ends when a player has the minimum money required to win. Player with most money wins.
     *
     * @param pgs - game state
     * @return - true if game ended, false otherwise
     */
    private boolean checkGameEnd(PokerGameState pgs) {
        PokerGameParameters pgp = (PokerGameParameters) pgs.getGameParameters();

        if (pgp.endMinMoney) {
            int maxMoney = 0;
            for (int playerID = 0; playerID < pgs.getNPlayers(); playerID++) {
                int money = pgs.playerMoney[playerID].getValue();
                if (money >= pgp.nWinMoney && money > maxMoney) {
                    maxMoney = money;
                }
            }
            if (maxMoney > 0) {
                endGame(pgs);
                return true;
            }
        } else {
            if (pgs.getRoundCounter() >= pgp.maxRounds) {
                // Max rounds reached, the player with most money wins
                endGame(pgs);
                return true;
            } else {
                int stillAlive = 0;
                for (int i = 0; i < pgs.getNPlayers(); i++) {
                    if (pgs.getPlayerResults()[i] == CoreConstants.GameResult.GAME_ONGOING) {
                        stillAlive++;
                        if (stillAlive > 1) break;
                    }
                }
                if (stillAlive == 1) {
                    endGame(pgs);
                    return true;
                }
            }
        }
        return false;
    }

    public void fold(PokerGameState pgs, int player) {
        if (player == pgs.getFirstPlayer()) {
            // Move first player to next one
            pgs.setFirstPlayer((pgs.getNPlayers() + pgs.getFirstPlayer() + 1) % pgs.getNPlayers());
            int nTries = 1;
            while ((pgs.playerFold[pgs.getFirstPlayer()] || pgs.getPlayerResults()[pgs.getFirstPlayer()] == LOSE_GAME) && nTries <= pgs.getNPlayers()) {
                pgs.setFirstPlayer((pgs.getNPlayers() + pgs.getFirstPlayer() + 1) % pgs.getNPlayers());
                nTries++;
            }
            if (nTries > pgs.getNPlayers()) {
                endGame(pgs);
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        PokerGameParameters pgp = (PokerGameParameters) gameState.getGameParameters();

        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = pgs.getCurrentPlayer();

        // Check if player can afford to: Bet, Call, Raise. Can also Check, Fold.

        int biggestBet = 0;
        boolean othersAllIn = true;  // True if all others are all in / out of the game, false otherwise
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (pgs.getPlayerBet()[i].getValue() > biggestBet) biggestBet = pgs.getPlayerBet()[i].getValue();
            if (i != player && pgs.getPlayerResults()[i] != LOSE_GAME && !pgs.playerFold[i] && !pgs.playerMoney[i].isMinimum())
                othersAllIn = false;
        }

        if (pgs.playerNeedsToCall[player] && !pgs.getPlayerMoney()[player].isMinimum()) {
            int diff = biggestBet - pgs.getPlayerBet()[player].getValue();

            if (pgs.playerMoney[player].getValue() >= diff) {
                actions.add(new Call(player));

                if (!othersAllIn) {
                    // Only raise if it makes sense
                    for (double r : pgp.raiseMultipliers) {
                        int diffR = diff + (int) (biggestBet * r);
                        if (pgs.playerMoney[player].getValue() >= diffR) {
                            actions.add(new Raise(player, r));
                        }
                    }
                }
            }
        } else {
            actions.add(new Check(player));

            if (!pgs.isBet() && !othersAllIn) {
                if (pgs.playerMoney[player].getValue() >= pgp.bet) {
                    actions.add(new Bet(player, pgp.bet));
                }
            }
        }
        actions.add(new Fold(player));
        if (!pgs.playerMoney[player].isMinimum()) {
            actions.add(new AllIn(player));
        }

        return actions;
    }
}

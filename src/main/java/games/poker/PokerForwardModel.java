package games.poker;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.poker.actions.*;
import games.poker.actions.Fold;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

import static games.poker.PokerGameState.PokerGamePhase.*;
import static utilities.Utils.GameResult.LOSE;


public class PokerForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        PokerGameState pgs = (PokerGameState) firstState;
        PokerGameParameters params = (PokerGameParameters) firstState.getGameParameters();

        pgs.currentMoney = new int[firstState.getNPlayers()];
        pgs.playerNeedsToCall = new boolean[firstState.getNPlayers()];
        pgs.playerFold = new boolean[firstState.getNPlayers()];
        pgs.bets = new int[firstState.getNPlayers()];
        pgs.playerActStreet = new boolean[pgs.getNPlayers()];
        pgs.totalPotMoney = 0;

        pgs.playerDecks = new ArrayList<>();
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            pgs.playerDecks.add(new Deck<>("Player " + i + " deck", i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            pgs.currentMoney[i] = params.nStartingMoney;
        }

        // Create the discard deck, at the beginning it is empty
        pgs.communityCards = new Deck<>("CommunityCards", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        // Player 0 starts the game
        pgs.getTurnOrder().setStartingPlayer(0);

        // Set up first round
        setupRound(pgs);
    }

    /**
     * Sets up a round for the game, including draw pile, discard deck and player decks, all reset.
     * @param pgs - current game state.
     */
    private void setupRound(PokerGameState pgs) {
        PokerGameParameters params = (PokerGameParameters) pgs.getGameParameters();
        Random r = new Random(params.getRandomSeed() + pgs.getTurnOrder().getRoundCounter());

        // Refresh player info
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            pgs.playerDecks.get(i).clear();
            pgs.playerNeedsToCall[i] = false;
            pgs.playerFold[i] = false;
            pgs.bets[i] = 0;
        }
        pgs.communityCards.clear();

        // Refresh draw deck and shuffle
        pgs.drawDeck = FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        pgs.drawDeck.shuffle(r);

        // Draw new cards for players
        drawCardsToPlayers(pgs);

        // Blinds TODO all-in & secondary pot if player can't afford blind
        int smallId = ((PokerTurnOrder)pgs.getTurnOrder()).getRoundFirstPlayer();
        int bigId = (pgs.getNPlayers() + smallId + 1) % pgs.getNPlayers();
        while ((pgs.playerFold[bigId] || pgs.getPlayerResults()[bigId] == LOSE)) {
            bigId = (pgs.getNPlayers() + bigId + 1) % pgs.getNPlayers();
        }

        pgs.currentMoney[smallId] -= params.smallBlind;
        pgs.currentMoney[bigId] -= params.bigBlind;
        pgs.totalPotMoney = params.smallBlind + params.bigBlind;
        pgs.bets[smallId] = params.smallBlind;
        pgs.bets[bigId] = params.bigBlind;
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (i != bigId) {
                pgs.playerNeedsToCall[i] = true;
            }
        }

        pgs.setGamePhase(Preflop);
        pgs.setBet(false);
    }

    private void drawCardsToPlayers(PokerGameState pgs) {
        for (int player = 0; player < pgs.getNPlayers(); player++) {
            for (int card = 0; card < ((PokerGameParameters)pgs.getGameParameters()).nCardsPerPlayer; card++) {
                pgs.playerDecks.get(player).add(pgs.drawDeck.draw());
            }
        }
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action) {
        action.execute(gameState);

        // Check end of street to add more community cards
        PokerGameState pgs = (PokerGameState) gameState;
        PokerGameParameters pgp = (PokerGameParameters) gameState.getGameParameters();

        pgs.playerActStreet[pgs.getCurrentPlayer()] = true;

        if (!(action instanceof Fold || action instanceof Check || action instanceof Call)) {
            gameState.getTurnOrder().endPlayerTurn(gameState);
        } else {
            boolean remainingDecisions = false;
            int stillAlive = 0;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                if (pgs.getPlayerResults()[i] != LOSE && !pgs.playerFold[i]) {
                    stillAlive++;
                    if (pgs.playerNeedsToCall[i] || !pgs.playerActStreet[i]){
                        remainingDecisions = true;
                    }
                }
            }
            if (stillAlive == 1) {
                // Round is over
                roundEnd(pgs);
            } else if (!remainingDecisions) {
                // Add community cards
                gameState.getTurnOrder().setTurnOwner(gameState.getTurnOrder().getFirstPlayer());
                pgs.setBet(false);
                Arrays.fill(pgs.playerActStreet, false);

                if (gameState.getGamePhase() == Preflop) {
                    // Add flop
                    for (int i = 0; i < pgp.nFlopCards; i++) {
                        pgs.communityCards.add(pgs.drawDeck.draw());
                    }
                    gameState.setGamePhase(Flop);
                } else if (gameState.getGamePhase() == Flop) {
                    // Add turn
                    for (int i = 0; i < pgp.nTurnCards; i++) {
                        pgs.communityCards.add(pgs.drawDeck.draw());
                    }
                    gameState.setGamePhase(Turn);
                } else if (gameState.getGamePhase() == Turn) {
                    // Add river
                    for (int i = 0; i < pgp.nRiverCards; i++) {
                        pgs.communityCards.add(pgs.drawDeck.draw());
                    }
                    gameState.setGamePhase(River);
                } else if (gameState.getGamePhase() == River) {
                    // Round is over
                    roundEnd(pgs);
                }
            }
        }
    }

    /**
     * Called when round is over. Calculate winner of round and distribute money.
     * @param pgs - current game state
     */
    private void roundEnd(PokerGameState pgs) {
        PokerGameParameters pgp = (PokerGameParameters) pgs.getGameParameters();
        // Calculate winner of round, they earn the money. Ties split money equally.
        int[] ranks = new int[pgs.getNPlayers()];
        HashSet<Integer>[] hands = new HashSet[pgs.getNPlayers()];
        int smallestRank = 11;
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (!pgs.playerFold[i] && pgs.getPlayerResults()[i] != LOSE) {
                pgs.playerDecks.get(i).add(pgs.communityCards.copy());
                Pair<PokerGameState.PokerHand, HashSet<Integer>> hand = PokerGameState.PokerHand.translateHand(pgs.playerDecks.get(i));
                if (hand != null) {
                    ranks[i] = hand.a.rank;
                    hands[i] = hand.b;
                    if (ranks[i] < smallestRank) {
                        smallestRank = ranks[i];
                    }
                }
            }
        }
        HashSet<Integer> winners = new HashSet<>();
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (!pgs.playerFold[i] && pgs.getPlayerResults()[i] != LOSE) {
                if (ranks[i] == smallestRank) winners.add(i);
            }
        }
        if (winners.size() > 1) {
            // A tie in rank, check card values
            ArrayList<Integer>[] cardValues = new ArrayList[pgs.getNPlayers()];
            int nCards = 0;
            for (int i: winners) {
                cardValues[i] = new ArrayList<>(hands[i]);
                cardValues[i].sort(Collections.reverseOrder());
                nCards = cardValues[i].size();
            }
            for (int j = 0; j < nCards; j++) {
                // Checking card by card, once one player is found the winner we break; could still be a tie
                int maxValue = 0;
                for (int i: winners) {
                    if (cardValues[i].get(j) > maxValue) maxValue = cardValues[i].get(j);
                }
                HashSet<Integer> actualWinners = new HashSet<>();
                for (int i: winners) {
                    if (cardValues[i].get(j) == maxValue) actualWinners.add(i);
                }
                if (actualWinners.size() == 1 || j == nCards-1) {
                    for (int i : actualWinners) {
                        pgs.currentMoney[i] += pgs.totalPotMoney/actualWinners.size();
                    }
                    break;
                }
            }
        } else {
            for (int i : winners) {
                pgs.currentMoney[i] += pgs.totalPotMoney;
            }
        }

        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (pgs.currentMoney[i] <= 0) {
                // Player is out of the game
                pgs.currentMoney[i] = 0;
                pgs.setPlayerResult(LOSE, i);
            }
        }

        // Check if game is over
        if (checkGameEnd(pgs)) return;

        // End previous round
        pgs.getTurnOrder().endRound(pgs);

        // Reset cards for the new round
        setupRound(pgs);
    }

    /**
     * Game ends when a player has the minimum money required to win. Player with most money wins.
     * @param pgs - game state
     * @return - true if game ended, false otherwise
     */
    private boolean checkGameEnd(PokerGameState pgs) {
        PokerGameParameters pgp = (PokerGameParameters) pgs.getGameParameters();

        if (pgp.endMinMoney) {

            int maxMoney = 0;
            for (int playerID = 0; playerID < pgs.getNPlayers(); playerID++) {
                if (pgs.currentMoney[playerID] >= pgp.nWinMoney && pgs.currentMoney[playerID] > maxMoney) {
                    maxMoney = pgs.currentMoney[playerID];
                }
            }
            if (maxMoney > 0) {
                // Game ended
                for (int playerID = 0; playerID < pgs.getNPlayers(); playerID++) {
                    if (pgs.currentMoney[playerID] == maxMoney) {
                        pgs.setPlayerResult(Utils.GameResult.WIN, playerID);
                    } else {
                        pgs.setPlayerResult(LOSE, playerID);
                    }
                }
                pgs.setGameStatus(Utils.GameResult.GAME_END);
                return true;
            }
        } else {
            int stillAlive = 0;
            int id = -1;
            for (int i = 0; i < pgs.getNPlayers(); i++) {
                if (pgs.getPlayerResults()[i] == Utils.GameResult.GAME_ONGOING) {
                    stillAlive ++;
                    id = i;
                    if (stillAlive > 1) break;
                }
            }
            if (stillAlive == 1) {
                pgs.setPlayerResult(Utils.GameResult.WIN, id);
                pgs.setGameStatus(Utils.GameResult.GAME_END);
                return true;
            }
        }
        return false;
    }


    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState)gameState;
        PokerGameParameters pgp = (PokerGameParameters)gameState.getGameParameters();

        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = pgs.getCurrentPlayer();

        // Check if player can afford to: Bet, Call, Raise. Can also Check, Fold. TODO: if on 0 money, it's all-in.
        if (!pgs.playerNeedsToCall[player] && !pgs.isBet()) {
            if (pgs.currentMoney[player] >= pgp.bet) {
                actions.add(new Bet(player, pgp.bet));
            }
        } else {

            int biggestBet = 0;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                if (pgs.getBets()[i] > biggestBet) biggestBet = pgs.getBets()[i];
            }
            int diff = biggestBet - pgs.getBets()[player];

            if (pgs.playerNeedsToCall[player] && pgs.currentMoney[player] >= diff) {
                actions.add(new Call(player));
            }

            for (double r: pgp.raiseMultipliers) {
                diff = biggestBet + (int) (biggestBet * r) - pgs.getBets()[player];
                if (pgs.currentMoney[player] >= diff) {
                    actions.add(new Raise(player, r));
                }
            }
        }
        if (!pgs.playerNeedsToCall[player]) {
            actions.add(new Check(player));
        }
        actions.add(new Fold(player));

        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new PokerForwardModel();
    }
}

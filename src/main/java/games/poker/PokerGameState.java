package games.poker;

import java.util.*;
import java.util.ArrayList;
import java.util.List;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Counter;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import games.GameType;
import games.poker.components.MoneyPot;
import utilities.Pair;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static utilities.Utils.generateCombinations;


public class PokerGameState extends AbstractGameState implements IPrintable {
    List<Deck<FrenchCard>> playerDecks;
    Counter[] playerMoney;
    Counter[] playerBet;

    Deck<FrenchCard> drawDeck;
    Deck<FrenchCard> communityCards;
    List<MoneyPot> moneyPots;  // mapping from all-in bet amount (max a player can put in the pot) to pot counter; -1 for default with no limits

    boolean[] playerNeedsToCall;  // True if player needs to call (can't just check)
    boolean[] playerFold;  // True if player folded
    boolean[] playerAllIn; // True if player is all-in
    boolean[] playerActStreet;  // true if player acted this street, false otherwise
    boolean bet;  // True if a bet was made this street
    protected int bigId; // Stores the id of the previous big blind

    enum PokerGamePhase implements IGamePhase {
        Preflop,
        Flop,
        Turn,
        River
    }

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players for this game.
     */

    public PokerGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Poker;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerDecks);
            add(drawDeck);
            add(communityCards);
            addAll(moneyPots);
            this.addAll(Arrays.asList(playerMoney));
            this.addAll(Arrays.asList(playerBet));
        }};
    }

    public void placeBet(int amount, int player) {
        // Check which pot this player is participating in, update the one that's not reached max
        int m = amount;
        MoneyPot noLimitPot = null;
        for (MoneyPot pot : moneyPots) {
            if (!pot.isNoLimit() && m > 0) {
                // First distribute in money pots that this player has not filled yet
                int capacityLeft = pot.getLimit() - pot.getPlayerContribution(player);
                if (capacityLeft > 0) {
                    int betAdded = Math.min(m, capacityLeft);
                    pot.increment(betAdded, player);
                    m -= betAdded;
                }
            } else {
                noLimitPot = pot;
            }
        }
        if (m > 0) {
            // Add the rest in no-limit pot
            if (noLimitPot == null) {
                throw new AssertionError("No no-limit pot found");
            }
            noLimitPot.increment(m, player);
        }

        playerBet[player].increment(amount);
        playerMoney[player].decrement(amount);

        if (playerMoney[player].isMinimum()) {
            // Check all money pots and balance them out by adding more if needed (caused by all-ins)
            List<MoneyPot> newPots = new ArrayList<>();
            for (MoneyPot pot : moneyPots) {
                int current = pot.getPlayerContribution(player);
                int highest = pot.getHighestContribution();
                if (current != highest) {
                    // Pot is not balanced. Make new pot with limit at lowest contribution, and keep here only overflow
                    MoneyPot newPot = new MoneyPot();
                    newPot.setLimit(current);
                    HashSet<Integer> contributorsToRemove = new HashSet<>();
                    for (int contributor : pot.getPlayerContribution().keySet()) {
                        if (pot.getPlayerContribution(contributor) == 0) {
                            contributorsToRemove.add(contributor);
                            continue;
                        }
                        int overflow = pot.getPlayerContribution(contributor) - current;
                        if (overflow > 0) {
                            // Player bet more than limit, overflow remains here, limit transferred to new pot
                            pot.getPlayerContribution().put(contributor, overflow);
                            newPot.increment(current, contributor);
                        } else {
                            // Player bet equal to or less than limit, transfer all to new pot, remove from current pot
                            contributorsToRemove.add(contributor);
                            newPot.increment(pot.getPlayerContribution(contributor), contributor);
                        }
                    }
                    for (int contributor : contributorsToRemove) {
                        pot.getPlayerContribution().remove(contributor);
                    }
                    newPots.add(newPot);
                }
                // then ensure that the contributions to the pot add up
                pot.setValue(pot.getPlayerContribution().values().stream().mapToInt(Integer::intValue).sum());
            }
            moneyPots.addAll(newPots);
        }
    }

    /**
     * This is a list of MoneyPots in player order
     **/
    public List<MoneyPot> getMoneyPots() {
        return moneyPots;
    }

    /**
     * The currently visible community cards
     **/
    public Deck<FrenchCard> getCommunityCards() {
        return communityCards;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public List<Deck<FrenchCard>> getPlayerDecks() {
        return playerDecks;
    }

    /**
     * A boolean array in player order, true if player needs to call (can't just check)
     **/
    public boolean[] getPlayerNeedsToCall() {
        return playerNeedsToCall;
    }

    /**
     * A boolean array in player order, true if player is 'All In'
     **/
    public boolean[] getPlayerAllIn() {
        return playerAllIn;
    }

    /**
     * A boolean array in player order, true if player has folded
     **/
    public boolean[] getPlayerFold() {
        return playerFold;
    }

    /**
     * An array in player order. Use Counter.getValue() to get the current money of the player
     **/
    public Counter[] getPlayerMoney() {
        return playerMoney;
    }

    /**
     * An array in player order. Use Counter.getValue() to get the current bet of the player
     **/
    public Counter[] getPlayerBet() {
        return playerBet;
    }

    public boolean isBet() {
        return bet;
    }

    public void setBet(boolean bet) {
        this.bet = bet;
    }

    /**
     * The player id of the Big Blind
     **/
    public int getBigId() {
        return bigId;
    }

    /***
     * Player to the right of big blind is small blind
     *
     * @return the id of the player with small blind
     */
    public int getSmallId() {
        return getNextNonBankruptPlayer(getBigId(), -1);
    }

    public boolean isPlayerStillToAct() {
        for (int p = 0; p < getNPlayers(); p++) {
            if (playerFold[p] || playerAllIn[p] || getPlayerResults()[p] == LOSE_GAME)
                continue;
            if (playerNeedsToCall[p] || !playerActStreet[p])
                return true;
        }
        return false;
    }

    public boolean isRoundOver() {
        int stillAlive = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            if (getPlayerResults()[i] != LOSE_GAME && !playerFold[i] && !playerAllIn[i]) {
                stillAlive++;
            }
        }
        if (stillAlive <= 1) {
            return true;
        }
        return false;
    }

    public int getNextActingPlayer(int fromPlayer, int direction) {
        int next = (nPlayers + fromPlayer + direction) % nPlayers;
        int nTries = 0;
        while ((playerFold[next] || playerAllIn[next] || getPlayerResults()[next] == LOSE_GAME)) {
            next = (nPlayers + next + direction) % nPlayers;
            nTries++;
            if (nTries > getNPlayers()) {
                return -1;
            }
        }
        return next;
    }

    public int getNextNonBankruptPlayer(int fromPlayer, int direction) {
        int next = (nPlayers + fromPlayer + direction) % nPlayers;
        while (getPlayerResults()[next] == LOSE_GAME) {
            next = (nPlayers + next + direction) % nPlayers;
        }
        return next;
    }

    public void getPlayerMustCall(int playerId) {
        // Others can't check
        for (int i = 0; i < getNPlayers(); i++) {
            if (i != playerId && !playerFold[i] && !playerAllIn[i] && getPlayerResults()[i] != LOSE_GAME) {
                playerNeedsToCall[i] = true;
            }
        }
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        PokerGameState copy = new PokerGameState(gameParameters.copy(), getNPlayers());
        copy.communityCards = communityCards.copy();
        copy.moneyPots = new ArrayList<>();
        for (MoneyPot pot : moneyPots) {
            copy.moneyPots.add(pot.copy());
        }
        copy.playerDecks = new ArrayList<>();
        copy.playerMoney = new Counter[getNPlayers()];
        copy.playerBet = new Counter[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerDecks.add(playerDecks.get(i).copy());
            copy.playerMoney[i] = playerMoney[i].copy();
            copy.playerBet[i] = playerBet[i].copy();
        }
        copy.drawDeck = drawDeck.copy();
        if (getCoreGameParameters().partialObservable && playerId != -1) {
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    copy.drawDeck.add(copy.playerDecks.get(i));
                    copy.playerDecks.get(i).clear();
                }
            }
            copy.drawDeck.shuffle(redeterminisationRnd);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    for (int j = 0; j < playerDecks.get(i).getSize(); j++) {
                        copy.playerDecks.get(i).add(copy.drawDeck.draw());
                    }
                }
            }
        }
        copy.playerNeedsToCall = playerNeedsToCall.clone();
        copy.playerFold = playerFold.clone();
        copy.playerAllIn = playerAllIn.clone();
        copy.playerActStreet = playerActStreet.clone();
        copy.bet = bet;
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new PokerHeuristic().evaluateState(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return playerMoney[playerId].getValue();
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>() {{
            add(drawDeck.getComponentID());
            for (Component c : drawDeck.getComponents()) {
                add(c.getComponentID());
            }
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    add(playerDecks.get(i).getComponentID());
                    for (Component c : playerDecks.get(i).getComponents()) {
                        add(c.getComponentID());
                    }
                }
            }
        }};
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PokerGameState)) return false;
        if (!super.equals(o)) return false;
        PokerGameState that = (PokerGameState) o;
        return bet == that.bet && Objects.equals(playerDecks, that.playerDecks) &&
                Arrays.equals(playerMoney, that.playerMoney) &&
                Arrays.equals(playerBet, that.playerBet) && Objects.equals(drawDeck, that.drawDeck)
                && Objects.equals(communityCards, that.communityCards) &&
                Objects.equals(moneyPots, that.moneyPots) &&
                Arrays.equals(playerNeedsToCall, that.playerNeedsToCall) &&
                Arrays.equals(playerFold, that.playerFold) &&
                Arrays.equals(playerActStreet, that.playerActStreet) &&
                Arrays.equals(playerAllIn, that.playerAllIn);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerDecks, drawDeck, communityCards, moneyPots, bet);
        result = 31 * result + Arrays.hashCode(playerMoney);
        result = 31 * result + Arrays.hashCode(playerBet);
        result = 31 * result + Arrays.hashCode(playerNeedsToCall);
        result = 31 * result + Arrays.hashCode(playerFold);
        result = 31 * result + Arrays.hashCode(playerActStreet);
        return result;
    }

    @Override
    public String toString() {
        return Objects.hash(gameParameters) + "|" +
                Objects.hash(gameStatus) + "|" +
                Objects.hash(gamePhase) + "|*|" +
                Objects.hash(playerDecks) + "|" +
                Objects.hash(drawDeck) + "|" +
                Objects.hash(communityCards) + "|" +
                Objects.hash(moneyPots) + "|" +
                Objects.hash(bet) + "|*|" +
                Arrays.hashCode(playerMoney) + "|" +
                Arrays.hashCode(playerBet) + "|" +
                Arrays.hashCode(playerNeedsToCall) + "|" +
                Arrays.hashCode(playerFold) + "|" +
                Arrays.hashCode(playerMoney) + "|" +
                Arrays.hashCode(playerResults) + "|";
    }

    enum PokerHand {
        RoyalFlush(1),
        StraightFlush(2),
        FourOfAKind(3),
        FullHouse(4),
        Flush(5),
        Straight(6),
        ThreeOfAKind(7),
        TwoPair(8),
        OnePair(9),
        HighCard(10);

        static final int pokerHandSize = 5;
        final int rank;

        PokerHand(int rank) {
            this.rank = rank;
        }

        static Pair<PokerHand, HashSet<Integer>> translateHand(Deck<FrenchCard> deck) {
            if (deck.getSize() > pokerHandSize) {
                // Make combinations, translate each hand and return the best hand (lowest rank; if tied, highest card values)
                int[] indx = new int[deck.getSize()];
                for (int i = 0; i < indx.length; i++) {
                    indx[i] = i;
                }
                ArrayList<int[]> combinations = generateCombinations(indx, pokerHandSize);
                ArrayList<Pair<Pair<PokerHand, ArrayList<Integer>>, Deck<FrenchCard>>> handOptions = new ArrayList<>();
                int smallestRank = 11;
                for (int[] combo : combinations) {
                    Deck<FrenchCard> temp = new Deck<>("Temp", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
                    for (int j : combo) {
                        temp.add(deck.get(j).copy());
                    }
                    Pair<PokerHand, ArrayList<Integer>> hand = _translateHand(temp);
                    if (hand.a.rank < smallestRank) {
                        handOptions.clear();
                        handOptions.add(new Pair<>(hand, temp));
                    } else if (hand.a.rank == smallestRank) {
                        handOptions.add(new Pair<>(hand, temp));
                    }
                }
                if (handOptions.size() == 1) {
                    deck.clear();
                    for (FrenchCard c : handOptions.get(0).b.getComponents()) {
                        deck.add(c);
                    }
                    return new Pair<>(handOptions.get(0).a.a, new HashSet<>(handOptions.get(0).a.b));
                } else {
                    // Choose the one with highest card values
                    for (int i = 0; i < pokerHandSize; i++) {
                        int maxValue = 0;
                        for (Pair<Pair<PokerHand, ArrayList<Integer>>, Deck<FrenchCard>> handOption : handOptions) {
                            int value = handOption.a.b.get(i);
                            if (value > maxValue) maxValue = value;
                        }
                        HashSet<Integer> best = new HashSet<>();
                        for (int j = 0; j < handOptions.size(); j++) {
                            int value = handOptions.get(j).a.b.get(i);
                            if (value == maxValue) best.add(j);
                        }
                        if (best.size() == 1 || i == pokerHandSize - 1) {
                            int option = best.iterator().next();
                            deck.clear();
                            for (FrenchCard c : handOptions.get(option).b.getComponents()) {
                                deck.add(c);
                            }
                            return new Pair<>(handOptions.get(option).a.a, new HashSet<>(handOptions.get(option).a.b));
                        }
                    }
                    return null;
                }
            } else {
                // Only one combination, just return its rank
                Pair<PokerHand, ArrayList<Integer>> hand = _translateHand(deck);
                return new Pair<>(hand.a, new HashSet<>(hand.b));
            }
        }

        static Pair<PokerHand, ArrayList<Integer>> _translateHand(Deck<FrenchCard> deck) {
            HashSet<FrenchCard.Suite> suites = new HashSet<>();
            HashSet<Integer> numberSet = new HashSet<>();
            HashMap<Integer, Integer> numberCount = new HashMap<>();
            ArrayList<Integer> numbers = new ArrayList<>();
            for (FrenchCard card : deck.getComponents()) {
                suites.add(card.suite);
                numbers.add(card.number);
                numberSet.add(card.number);
                if (numberCount.containsKey(card.number)) {
                    numberCount.put(card.number, numberCount.get(card.number) + 1);
                } else {
                    numberCount.put(card.number, 1);
                }
            }
            Collections.sort(numbers);
            boolean consecutive = isListConsecutive(numbers);
            if (suites.size() == 1) {
                // Flush
                // Check straight
                if (consecutive) {
                    // Check royal
                    if (numberSet.contains(FrenchCard.FrenchCardType.Ace.getNumber())) {
                        return new Pair<>(RoyalFlush, numbers);
                    } else {
                        return new Pair<>(StraightFlush, numbers);
                    }
                } else {
                    return new Pair<>(Flush, numbers);
                }
            }
            if (numberSet.size() == 2) {
                // Full house or four of a kind
                int maxCount = maxCount(numberCount);
                if (maxCount == 4) return new Pair<>(FourOfAKind, numbers);
                else return new Pair<>(FullHouse, numbers);
            } else if (numberSet.size() == 3) {
                // Three of a kind or two pair
                int maxCount = maxCount(numberCount);
                if (maxCount == 3) return new Pair<>(ThreeOfAKind, numbers);
                else return new Pair<>(TwoPair, numbers);
            } else if (numberSet.size() == 4) {
                return new Pair<>(OnePair, numbers);
            } else {
                if (consecutive) return new Pair<>(Straight, numbers);
                else return new Pair<>(HighCard, numbers);
            }
        }

        static boolean isListConsecutive(ArrayList<Integer> numbers) {
            for (int i = 0; i < numbers.size() - 1; i++) {
                if (numbers.get(i + 1) - numbers.get(i) != 1) return false;
            }
            return true;
        }

        static int maxCount(HashMap<Integer, Integer> map) {
            int max = 0;
            for (int c : map.values()) {
                if (c > max) max = c;
            }
            return max;
        }
    }

}

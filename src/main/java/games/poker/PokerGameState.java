package games.poker;

import java.util.*;
import java.util.ArrayList;
import java.util.List;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import core.turnorders.AlternatingTurnOrder;
import games.GameType;

import static core.CoreConstants.PARTIAL_OBSERVABLE;


public class PokerGameState extends AbstractGameState implements IPrintable {
    List<Deck<FrenchCard>>  playerDecks;
    Deck<FrenchCard>        drawDeck;
    Deck<FrenchCard>        communityCards;
    int[]                   currentMoney;
    int[]                   bets;
    boolean[]               playerNeedsToCall;
    boolean[]               playerFold;
    int                     totalPotMoney;
    boolean                 bet;

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
     * @param nPlayers      - number of players for this game.
     */

    public PokerGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new PokerTurnOrder(nPlayers), GameType.Poker);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerDecks);
            add(drawDeck);
            add(communityCards);
        }};
    }

    public void updateTotalPotMoney(int money) {
        totalPotMoney += money;
    }

    public void setTotalPotMoney(int totalPotMoney) {
        this.totalPotMoney = totalPotMoney;
    }

    public int getTotalPotMoney() {
        return totalPotMoney;
    }

    public Deck<FrenchCard> getCommunityCards() {
        return communityCards;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public List<Deck<FrenchCard>> getPlayerDecks() {
        return playerDecks;
    }

    public boolean[] getPlayerNeedsToCall() {
        return playerNeedsToCall;
    }

    public boolean[] getPlayerFold() {
        return playerFold;
    }

    public int[] getCurrentMoney() {
        return currentMoney;
    }

    public int[] getBets() {
        return bets;
    }

    public boolean isBet() {
        return bet;
    }

    public void setBet(boolean bet) {
        this.bet = bet;
    }

    /*
              #    spacer,  A,  K,  Q,  J, 10, 9,  8,  7,  6,  5,  4,  3,  2
               spade : [ 0, 0,  4,  8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48 ],
                heart: [ 0, 1,  5, 9,  13, 17, 21, 25, 29, 33, 37, 41, 45, 49 ],
              diamond: [ 0, 2,  6, 10, 14, 18, 22, 26, 30, 34, 38, 42, 46, 50 ],
                 club: [ 0, 3,  7, 11, 15, 19, 23, 27, 31, 35, 39, 43, 47, 51 ],
         */

    @Override
    protected AbstractGameState _copy(int playerId) {
        PokerGameState copy = new PokerGameState(gameParameters.copy(), getNPlayers());
        copy.playerDecks = new ArrayList<>();
        for (Deck<FrenchCard> d : playerDecks) {
            copy.playerDecks.add(d.copy());
        }
        copy.drawDeck = drawDeck.copy();
        if (PARTIAL_OBSERVABLE && playerId != -1) {
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    copy.drawDeck.add(copy.playerDecks.get(i));
                    copy.playerDecks.get(i).clear();
                }
            }
            copy.drawDeck.shuffle(new Random(copy.gameParameters.getRandomSeed()));
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    copy.playerDecks.get(i).add(copy.drawDeck.draw());
                }
            }
        }
        copy.currentMoney = currentMoney.clone();
        copy.bets = bets.clone();
        copy.playerNeedsToCall = playerNeedsToCall.clone();
        copy.playerFold = playerFold.clone();
        copy.communityCards = communityCards.copy();
        copy.totalPotMoney = totalPotMoney;
        copy.bet = bet;
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new PokerHeuristic().evaluateState(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return currentMoney[playerId];
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>() {{
            add(drawDeck.getComponentID());
            for (Component c: drawDeck.getComponents()) {
                add(c.getComponentID());
            }
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    add(playerDecks.get(i).getComponentID());
                    for (Component c: playerDecks.get(i).getComponents()) {
                        add(c.getComponentID());
                    }
                }
            }
        }};
    }

    @Override
    protected void _reset() {
        playerDecks = new ArrayList<>();
        drawDeck = null;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PokerGameState)) return false;
        if (!super.equals(o)) return false;
        PokerGameState that = (PokerGameState) o;
        return totalPotMoney == that.totalPotMoney && bet == that.bet && Objects.equals(playerDecks, that.playerDecks) && Objects.equals(drawDeck, that.drawDeck) && Objects.equals(communityCards, that.communityCards) && Arrays.equals(currentMoney, that.currentMoney) && Arrays.equals(bets, that.bets) && Arrays.equals(playerNeedsToCall, that.playerNeedsToCall) && Arrays.equals(playerFold, that.playerFold);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerDecks, drawDeck, communityCards, totalPotMoney, bet);
        result = 31 * result + Arrays.hashCode(currentMoney);
        result = 31 * result + Arrays.hashCode(bets);
        result = 31 * result + Arrays.hashCode(playerNeedsToCall);
        result = 31 * result + Arrays.hashCode(playerFold);
        return result;
    }

    enum PokerHand {
        RoyalFlush (1),
        StraightFlush (2),
        FourOfAKind (3),
        FullHouse (4),
        Flush (5),
        Straight (6),
        ThreeOfAKind (7),
        TwoPair (8),
        OnePair (9),
        HighCard (10);

        int rank;
        PokerHand(int rank) {
            this.rank = rank;
        }

        static PokerHand translateHand(Deck<FrenchCard> deck) {
            HashSet<FrenchCard.Suite> suites = new HashSet<>();
            HashSet<Integer> numberSet = new HashSet<>();
            HashMap<Integer, Integer> numberCount = new HashMap<>();
            ArrayList<Integer> numbers = new ArrayList<>();
            for (FrenchCard card: deck.getComponents()) {
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
                        return RoyalFlush;
                    } else {
                        return StraightFlush;
                    }
                } else {
                    return Flush;
                }
            }
            if (numberSet.size() == 2) {
                // Full house or four of a kind
                int maxCount = maxCount(numberCount);
                if (maxCount == 4) return FourOfAKind;
                else return FullHouse;
            } else if (numberSet.size() == 3) {
                // Three of a kind or two pair
                int maxCount = maxCount(numberCount);
                if (maxCount == 3) return ThreeOfAKind;
                else return TwoPair;
            } else if (numberSet.size() == 4) {
                return OnePair;
            } else {
                if (consecutive) return Straight;
                else return HighCard;
            }
        }

        static boolean isListConsecutive(ArrayList<Integer> numbers) {
            for (int i = 0; i < numbers.size()-1; i++) {
                if (numbers.get(i+1) - numbers.get(i) != 1) return false;
            }
            return true;
        }

        static int maxCount(HashMap<Integer, Integer> map) {
            int max = 0;
            for (int c: map.values()) {
                if (c > max) max = c;
            }
            return max;
        }
    }

}

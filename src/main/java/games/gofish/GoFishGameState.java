package games.gofish;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import games.GameType;

import java.util.*;

public class GoFishGameState extends AbstractGameState implements IPrintable {

    // --- Components ---
    List<Deck<FrenchCard>> playerHands;   // each player's hand (owner-visible)
    Deck<FrenchCard> drawDeck;            // central draw pile
    List<Deck<FrenchCard>> playerBooks;   // completed books per player (public)

    // --- Flow flags read by ForwardModel ---
    public boolean continuePlayerTurn = false;
    public boolean mustDraw = false;
    public int lastRequestedRank = -1;

    public GoFishGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() { return GameType.GoFish; }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        if (drawDeck != null) components.add(drawDeck);
        if (playerHands != null) components.addAll(playerHands);
        if (playerBooks != null) components.addAll(playerBooks);
        return components;
    }

    @Override
    protected GoFishGameState _copy(int playerId) {
        GoFishGameState copy = new GoFishGameState(gameParameters.copy(), getNPlayers());

        // Core status
        copy.gameStatus = this.gameStatus;
        if (this.playerResults != null) copy.playerResults = this.playerResults.clone();
        // Note: not copying turn order/current player here (your TAG build doesn’t expose those hooks).

        // Components
        copy.drawDeck = (this.drawDeck == null) ? null : this.drawDeck.copy();

        copy.playerHands = new ArrayList<>();
        if (this.playerHands != null) {
            for (Deck<FrenchCard> hand : this.playerHands) {
                copy.playerHands.add(hand == null ? null : hand.copy());
            }
        }

        copy.playerBooks = new ArrayList<>();
        if (this.playerBooks != null) {
            for (Deck<FrenchCard> books : this.playerBooks) {
                copy.playerBooks.add(books == null ? null : books.copy());
            }
        }

        // Flags
        copy.continuePlayerTurn = this.continuePlayerTurn;
        copy.mustDraw = this.mustDraw;
        copy.lastRequestedRank = this.lastRequestedRank;

        // Redeterminisation (hide others’ hands if partial observable)
        if (getCoreGameParameters().partialObservable && playerId != -1 && copy.drawDeck != null) {
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId && copy.playerHands.get(i) != null) {
                    copy.drawDeck.add(copy.playerHands.get(i));
                    copy.playerHands.get(i).clear();
                }
            }
            copy.drawDeck.shuffle(redeterminisationRnd);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    int handSize = this.playerHands.get(i).getSize();
                    for (int j = 0; j < handSize && copy.drawDeck.getSize() > 0; j++) {
                        copy.playerHands.get(i).add(copy.drawDeck.draw());
                    }
                }
            }
        }
        return copy;
    }



    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            int books = getPlayerBooks().get(playerId).getSize() / 4;
            int cardsInHand = getPlayerHands().get(playerId).getSize();
            return books - 0.1 * cardsInHand;
        }
        return getPlayerResults()[playerId].value;
    }

    @Override
    public double getGameScore(int playerId) {
        return getPlayerBooks().get(playerId).getSize() / 4.0;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoFishGameState)) return false;
        if (!super.equals(o)) return false;
        GoFishGameState that = (GoFishGameState) o;
        return continuePlayerTurn == that.continuePlayerTurn
                && mustDraw == that.mustDraw
                && lastRequestedRank == that.lastRequestedRank
                && Objects.equals(playerHands, that.playerHands)
                && Objects.equals(drawDeck, that.drawDeck)
                && Objects.equals(playerBooks, that.playerBooks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, drawDeck, playerBooks,
                continuePlayerTurn, mustDraw, lastRequestedRank);
    }

    @Override
    public void printToConsole() {
        System.out.println("== Go Fish ==");
        System.out.println("Status: " + getGameStatus());
        System.out.println("Current Player: " + getCurrentPlayer());
        System.out.println("Draw Deck: " + (drawDeck == null ? 0 : drawDeck.getSize()));
        for (int i = 0; i < getNPlayers(); i++) {
            int handSz = playerHands == null ? 0 : playerHands.get(i).getSize();
            int books = playerBooks == null ? 0 : playerBooks.get(i).getSize() / 4;
            System.out.println("P" + i + " Hand=" + handSz + " Books=" + books);
        }
        if (continuePlayerTurn) System.out.println("Flag: continuePlayerTurn");
        if (mustDraw) System.out.println("Flag: mustDraw");
        if (lastRequestedRank != -1) System.out.println("Last rank asked: " + lastRequestedRank);
    }

    // Getters
    public List<Deck<FrenchCard>> getPlayerHands() { return playerHands; }
    public Deck<FrenchCard> getDrawDeck() { return drawDeck; }
    public List<Deck<FrenchCard>> getPlayerBooks() { return playerBooks; }

    // Helpers
    public boolean playerHasRank(int playerId, int rank) {
        for (FrenchCard c : playerHands.get(playerId).getComponents())
            if (c.number == rank) return true;
        return false;
    }

    public List<FrenchCard> removeCardsOfRank(int playerId, int rank) {
        List<FrenchCard> removed = new ArrayList<>();
        Deck<FrenchCard> hand = playerHands.get(playerId);
        for (int i = hand.getSize() - 1; i >= 0; i--) {
            if (hand.get(i).number == rank) removed.add(hand.pick(i));
        }
        return removed;
    }

    public List<FrenchCard> takeAllRankFromPlayer(int playerId, int rank) {
        return removeCardsOfRank(playerId, rank);
    }

    public Set<Integer> ranksInHand(int playerId) {
        Set<Integer> ranks = new HashSet<>();
        for (FrenchCard c : playerHands.get(playerId).getComponents()) ranks.add(c.number);
        return ranks;
    }

    public void checkAndCollectBooks(int playerId) {
        Map<Integer, Integer> counts = new HashMap<>();
        Deck<FrenchCard> hand = playerHands.get(playerId);
        for (FrenchCard c : hand.getComponents()) counts.merge(c.number, 1, Integer::sum);

        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() >= 4) {
                List<FrenchCard> book = removeCardsOfRank(playerId, e.getKey());
                for (int i = 0; i < Math.min(4, book.size()); i++) playerBooks.get(playerId).add(book.get(i));
                for (int i = 4; i < book.size(); i++) hand.add(book.get(i));
            }
        }
    }
}

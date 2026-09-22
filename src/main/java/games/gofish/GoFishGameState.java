package games.gofish;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.*;

/**
 * <p>Game state for Go Fish. Data-only: all initialisation and rule logic lives in {@link GoFishForwardModel}.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerHands - one PartialObservableDeck per player, VISIBLE_TO_OWNER. A card the owner has shown to the
 *     table is visible to all.</li>
 *     <li>drawDeck - face down, HIDDEN_TO_ALL</li>
 *     <li>playerBooks - the books each player has laid down, as their cards, VISIBLE_TO_ALL</li>
 *     <li>extraTurn - whether the current player takes another turn after their ask</li>
 *     <li>knownVoids - the ranks each player is publicly known not to hold</li>
 * </ul>
 */
public class GoFishGameState extends AbstractGameState implements IPrintable {

    List<PartialObservableDeck<FrenchCard>> playerHands;
    Deck<FrenchCard> drawDeck;
    List<Deck<FrenchCard>> playerBooks;
    boolean extraTurn;
    GoFishKnownVoids knownVoids;

    public GoFishGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.GoFish;
    }

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
        GoFishGameState copy = new GoFishGameState(gameParameters, getNPlayers());
        copy.drawDeck = drawDeck.copy();
        copy.playerHands = new ArrayList<>();
        for (PartialObservableDeck<FrenchCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.playerBooks = new ArrayList<>();
        for (Deck<FrenchCard> books : playerBooks)
            copy.playerBooks.add(books.copy());
        copy.extraTurn = extraTurn;
        copy.knownVoids = knownVoids.copy();

        if (getCoreGameParameters().partialObservable && playerId != -1) {
            List<Deck<FrenchCard>> decks = new ArrayList<>();
            decks.add(copy.drawDeck);
            decks.addAll(copy.playerHands);
            // cards shown to the table stay where they are, and no player is given a rank they are known not to hold
            DeterminisationUtilities.reshuffle(playerId, decks, c -> true, redeterminisationRnd, copy.knownVoids::permits);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal())
            return getBooks(playerId) / 13.0;
        return getPlayerResults()[playerId].value;
    }

    @Override
    public double getGameScore(int playerId) {
        return getBooks(playerId);
    }

    public int getBooks(int playerId) {
        return playerBooks.get(playerId).getSize() / 4;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof GoFishGameState that)) return false;
        return extraTurn == that.extraTurn
                && Objects.equals(knownVoids, that.knownVoids)
                && Objects.equals(playerHands, that.playerHands)
                && Objects.equals(drawDeck, that.drawDeck)
                && Objects.equals(playerBooks, that.playerBooks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, drawDeck, playerBooks, extraTurn, knownVoids);
    }

    @Override
    public void printToConsole() {
        System.out.println("== Go Fish ==");
        System.out.println("Status: " + getGameStatus());
        System.out.println("Current Player: " + getCurrentPlayer());
        System.out.println("Draw Deck: " + (drawDeck == null ? 0 : drawDeck.getSize()));
        for (int i = 0; i < getNPlayers(); i++) {
            int handSz = playerHands == null ? 0 : playerHands.get(i).getSize();
            int books = playerBooks == null ? 0 : getBooks(i);
            System.out.println("P" + i + " Hand=" + handSz + " Books=" + books);
        }
    }

    public List<PartialObservableDeck<FrenchCard>> getPlayerHands() {
        return playerHands;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public List<Deck<FrenchCard>> getPlayerBooks() {
        return playerBooks;
    }

    public GoFishKnownVoids getKnownVoids() {
        return knownVoids;
    }

    public boolean isExtraTurn() {
        return extraTurn;
    }

    public void setExtraTurn(boolean extraTurn) {
        this.extraTurn = extraTurn;
    }

    public boolean playerHasRank(int playerId, int rank) {
        for (FrenchCard c : playerHands.get(playerId).getComponents())
            if (c.number == rank) return true;
        return false;
    }
}

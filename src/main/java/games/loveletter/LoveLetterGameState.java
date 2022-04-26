package games.loveletter;

import core.AbstractParameters;
import core.AbstractGameState;
import core.components.Component;
import core.interfaces.IGamePhase;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.GameType;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.*;

import static games.loveletter.LoveLetterGameState.LoveLetterGamePhase.Draw;

public class LoveLetterGameState extends AbstractGameState implements IPrintable {

    // Love letter adds one game phase on top of default phases
    public enum LoveLetterGamePhase implements IGamePhase {
        Draw
    }

    // List of cards in player hands
    List<PartialObservableDeck<LoveLetterCard>> playerHandCards;

    // Discarded cards
    List<Deck<LoveLetterCard>> playerDiscardCards;

    // Cards in draw pile
    PartialObservableDeck<LoveLetterCard> drawPile;

    // Cards in the reserve
    PartialObservableDeck<LoveLetterCard> reserveCards;

    // If true: player cannot be effected by any card effects
    boolean[] effectProtection;

    // Affection tokens per player
    int[] affectionTokens;

    /**
     * For unit testing
     * @param playerId
     */
    public void addAffectionToken(int playerId) {
        affectionTokens[playerId]++;
    }

    public LoveLetterGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new LoveLetterTurnOrder(nPlayers), GameType.LoveLetter);
        gamePhase = Draw;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(playerHandCards);
        components.addAll(playerDiscardCards);
        components.add(drawPile);
        components.add(reserveCards);
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        LoveLetterGameState llgs = new LoveLetterGameState(gameParameters.copy(), getNPlayers());
        llgs.drawPile = drawPile.copy();
        llgs.reserveCards = reserveCards.copy();
        llgs.playerHandCards = new ArrayList<>();
        llgs.playerDiscardCards = new ArrayList<>();
        for (int i = 0; i < getNPlayers(); i++) {
            llgs.playerHandCards.add(playerHandCards.get(i).copy());
            llgs.playerDiscardCards.add(playerDiscardCards.get(i).copy());
        }
        llgs.effectProtection = effectProtection.clone();
        llgs.affectionTokens = affectionTokens.clone();

        if (getCoreGameParameters().partialObservable && playerId != -1) {
            // Draw pile, some reserve cards and other player's hand is possibly hidden. Mix all together and draw randoms
            HashSet<Integer>[] cardsNotVisible = new HashSet[getNPlayers()];
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    PartialObservableDeck<LoveLetterCard> deck = llgs.playerHandCards.get(i);
                    cardsNotVisible[i] = new HashSet<>();
                    for (int j = 0; j < deck.getSize(); j++) {
                        if (!deck.getVisibilityForPlayer(j, playerId)) {
                            // Hide!
                            cardsNotVisible[i].add(j);
                        }
                    }
                    for (int j: cardsNotVisible[i]) {
                        llgs.drawPile.add(llgs.playerHandCards.get(i).pick(j));
                    }
                }
            }
            for (int i = 0; i < llgs.reserveCards.getSize(); i++) {
                if (!llgs.reserveCards.isComponentVisible(i, playerId)) {
                    // Hide!
                    llgs.drawPile.add(llgs.reserveCards.get(i));
                }
            }
            Random r = new Random(llgs.getGameParameters().getRandomSeed());
            llgs.drawPile.shuffle(r);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    // New random cards
                    for (int j = 0; j < cardsNotVisible[i].size(); j++) {
                        llgs.playerHandCards.get(i).add(llgs.drawPile.draw());
                    }
                }
            }
            for (int i = 0; i < llgs.reserveCards.getSize(); i++) {
                if (!llgs.reserveCards.isComponentVisible(i, playerId)) {
                    // New random card
                    llgs.reserveCards.setComponent(i, llgs.drawPile.draw());
                }
            }
        }
        return llgs;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new LoveLetterHeuristic().evaluateState(this, playerId);
    }

    /**
     * This provides the current score in game turns. This will only be relevant for games that have the concept
     * of victory points, etc.
     * If a game does not support this directly, then just return 0.0
     *
     * @param playerId
     * @return - double, score of current state
     */
    @Override
    public double getGameScore(int playerId) {
        return affectionTokens[playerId];
    }

    @Override
    protected void _reset() {
        gamePhase = Draw;
        playerHandCards = new ArrayList<>();
        playerDiscardCards = new ArrayList<>();
        drawPile = null;
        reserveCards = null;
        effectProtection = new boolean[getNPlayers()];
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoveLetterGameState)) return false;
        if (!super.equals(o)) return false;
        LoveLetterGameState that = (LoveLetterGameState) o;
        return Objects.equals(playerHandCards, that.playerHandCards) &&
                Objects.equals(playerDiscardCards, that.playerDiscardCards) &&
                Objects.equals(drawPile, that.drawPile) &&
                Objects.equals(reserveCards, that.reserveCards) &&
                Arrays.equals(effectProtection, that.effectProtection) &&
                Arrays.equals(affectionTokens, that.affectionTokens);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerHandCards, playerDiscardCards, drawPile, reserveCards);
        result = 31 * result + Arrays.hashCode(effectProtection);
        result = 31 * result + Arrays.hashCode(affectionTokens);
        return result;
    }

    /**
     * Updates components after round setup.
     */
    void updateComponents() {
        this.addAllComponents();
    }

    /**
     * Checks if the countess needs to be forced to play.
     * @param playerDeck - deck of player to check
     * @return - true if countess should be forced, false otherwise.
     */
    boolean needToForceCountess(Deck<LoveLetterCard> playerDeck){
        boolean ownsCountess = false;
        for (LoveLetterCard card : playerDeck.getComponents()) {
            if (card.cardType == LoveLetterCard.CardType.Countess){
                ownsCountess = true;
                break;
            }
        }

        boolean forceCountess = false;
        if (ownsCountess)
        {
            for (LoveLetterCard card: playerDeck.getComponents()) {
                if (card.cardType == LoveLetterCard.CardType.Prince || card.cardType == LoveLetterCard.CardType.King){
                    forceCountess = true;
                    break;
                }
            }
        }
        return forceCountess;
    }

    /**
     * Sets this player as dead and updates game and player status
     * @param playerID - ID of player dead
     */
    public void killPlayer(int playerID){
        setPlayerResult(Utils.GameResult.LOSE, playerID);

        // a losing player needs to discard all cards
        while (playerHandCards.get(playerID).getSize() > 0)
            playerDiscardCards.get(playerID).add(playerHandCards.get(playerID).draw());
    }

    // Getters, Setters
    public LoveLetterCard getReserveCard(){
        return reserveCards.draw();
    }
    public PartialObservableDeck<LoveLetterCard> getReserveCards() {
        return reserveCards;
    }
    public boolean isNotProtected(int playerID){
        return !effectProtection[playerID];
    }
    public void setProtection(int playerID, boolean protection){
        effectProtection[playerID] = protection;
    }
    public int getRemainingCards(){
        return drawPile.getSize();
    }
    public List<PartialObservableDeck<LoveLetterCard>> getPlayerHandCards() {
        return playerHandCards;
    }
    public List<Deck<LoveLetterCard>> getPlayerDiscardCards() {
        return playerDiscardCards;
    }
    public PartialObservableDeck<LoveLetterCard> getDrawPile() {
        return drawPile;
    }
    public int[] getAffectionTokens() {
        return affectionTokens;
    }

    /**
     * Prints the game state.
     */
    public void printToConsole() {
        System.out.println("======================");
        System.out.println("Love Letter Game-State");
        System.out.println("----------------------");

        for (int i = 0; i < playerHandCards.size(); i++){
            if (getCurrentPlayer() == i)
                System.out.print(">>> Player " + i + ":");
            else
                System.out.print("Player " + i + ": ");
            System.out.print(playerHandCards.get(i).toString(this, getCurrentPlayer()));
            System.out.print(";\t Discarded: ");
            System.out.print(playerDiscardCards.get(i));

            System.out.print(";\t Protected: ");
            System.out.print(effectProtection[i]);
            System.out.print(";\t Affection: ");
            System.out.print(affectionTokens[i]);
            System.out.print(";\t Status: ");
            System.out.println(playerResults[i]);
        }

        System.out.println("\nDrawPile" + ":" + drawPile.toString(this, getCurrentPlayer()));
        System.out.println("ReserveCards" + ":" + reserveCards.toString(this, getCurrentPlayer()));

        System.out.println("Current GamePhase: " + gamePhase);
        System.out.println("======================");
    }
}

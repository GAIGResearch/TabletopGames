package games.loveletter;

import core.AbstractParameters;
import core.AbstractGameState;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import core.interfaces.IVectorisable;
import games.GameType;
import games.loveletter.cards.LoveLetterCard;

import java.util.*;


public class LoveLetterGameState extends AbstractGameState implements IPrintable, IVectorisable {

    // List of cards in player hands
    List<PartialObservableDeck<LoveLetterCard>> playerHandCards;

    // Discarded cards
    List<Deck<LoveLetterCard>> playerDiscardCards;

    // Cards in draw pile
    PartialObservableDeck<LoveLetterCard> drawPile;

    // Cards in the reserve
    Deck<LoveLetterCard> reserveCards;
    LoveLetterCard removedCard;

    // If true: player cannot be effected by any card effects
    boolean[] effectProtection;

    // Affection tokens per player
    int[] affectionTokens;

    /**
     * For unit testing
     * @param playerId - ID of player queried
     */
    public void addAffectionToken(int playerId) {
        affectionTokens[playerId]++;
    }

    public LoveLetterGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.LoveLetter;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(playerHandCards);
        components.addAll(playerDiscardCards);
        components.add(drawPile);
        components.add(reserveCards);
        components.add(removedCard);
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        LoveLetterGameState llgs = new LoveLetterGameState(gameParameters.copy(), getNPlayers());
        llgs.drawPile = drawPile.copy();
        llgs.reserveCards = reserveCards.copy();
        llgs.removedCard = removedCard.copy();
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
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    PartialObservableDeck<LoveLetterCard> deck = llgs.playerHandCards.get(i);
                    for (int j = 0; j < deck.getSize(); j++) {
                        if (!deck.getVisibilityForPlayer(j, playerId)) {
                            // Hide!
                            llgs.drawPile.add(deck.get(j));
                        }
                    }
                }
            }
            Random r = new Random(llgs.getGameParameters().getRandomSeed());
            llgs.drawPile.shuffle(r);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    // New random cards
                    PartialObservableDeck<LoveLetterCard> deck = llgs.playerHandCards.get(i);
                    for (int j = 0; j < deck.getSize(); j++) {
                        if (!deck.getVisibilityForPlayer(j, playerId)) {
                            llgs.playerHandCards.get(i).setComponent(j, llgs.drawPile.draw());
                        }
                    }
                    deck.shuffle(r);
                }
            }
        }
        return llgs;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new LoveLetterHeuristic().evaluateState(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return affectionTokens[playerId];
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
                Objects.equals(removedCard, that.removedCard) &&
                Arrays.equals(effectProtection, that.effectProtection) &&
                Arrays.equals(affectionTokens, that.affectionTokens);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerHandCards, playerDiscardCards, drawPile, reserveCards, removedCard);
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
     * @return - card type of the card that forces the countess to be played, null if countess not forced
     */
    public LoveLetterCard.CardType needToForceCountess(Deck<LoveLetterCard> playerDeck){
        boolean ownsCountess = false;
        for (LoveLetterCard card : playerDeck.getComponents()) {
            if (card.cardType == LoveLetterCard.CardType.Countess){
                ownsCountess = true;
                break;
            }
        }

        if (ownsCountess)
        {
            for (LoveLetterCard card: playerDeck.getComponents()) {
                if (card.cardType == LoveLetterCard.CardType.Prince || card.cardType == LoveLetterCard.CardType.King){
                    return card.cardType;
                }
            }
        }
        return null;
    }

    /**
     * Sets this player as dead and updates game and player status
     * @param whoKill - ID of player killing
     * @param targetPlayer - ID of player killed
     * @param cardType - card used to kill
     */
    public void killPlayer(int whoKill, int targetPlayer, LoveLetterCard.CardType cardType){
        setPlayerResult(CoreConstants.GameResult.LOSE_ROUND, targetPlayer);

        // a losing player needs to discard all cards
        while (playerHandCards.get(targetPlayer).getSize() > 0)
            playerDiscardCards.get(targetPlayer).add(playerHandCards.get(targetPlayer).draw());

        logEvent("Killed player: " + whoKill + "," + targetPlayer + "," + cardType);
    }

    // Getters, Setters
    public LoveLetterCard getRemovedCard() {
        return removedCard;
    }
    public Deck<LoveLetterCard> getReserveCards() {
        return reserveCards;
    }
    public boolean isProtected(int playerID){
        return effectProtection[playerID];
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
        if (reserveCards != null && reserveCards.getSize() > 0) {
            System.out.println("ReserveCards" + ":" + reserveCards);
        }

        System.out.println("Current GamePhase: " + gamePhase);
        System.out.println("======================");
    }

    @Override
    public String getObservationJson() {
        return null;
    }

    @Override
    public double[] getObservationVector() {

        // Schema
        // [0-7]: Player hand cards (per card type)
        // [8]: Number of cards in draw pile
        // [9-12]: Number of cards in each player discard pile
        // [13-16]: Affection tokens per player

        int playerID = getCurrentPlayer();
        double[] observationSpace = new double[18];
        PartialObservableDeck<LoveLetterCard> playerHandCards = getPlayerHandCards().get(playerID);

        // Player Hand Cards
        for (LoveLetterCard card : playerHandCards.getComponents()) {
            observationSpace[card.cardType.getValue() - 1] = 1;
        }

        // Draw Pile

        observationSpace[8] = drawPile.getSize();

        // Discard Piles
        int i = 9;
        for (PartialObservableDeck<LoveLetterCard> deck : getPlayerHandCards()) {
            observationSpace[i] += deck.getSize();
            i++;
        }

        // Affection Tokens
        for (int j = 0; j < affectionTokens.length; j++) {
            observationSpace[13 + j] = affectionTokens[j];
        }

        return observationSpace;

    }

    @Override
    public double[] getNormalizedObservationVector() {
        // Schema
        // [0-7]: Player hand cards (per card type)
        // [8]: Number of cards in draw pile
        // [9-12]: Number of cards in each player discard pile
        // [13-16]: Affection tokens per player

        LoveLetterParameters params = (LoveLetterParameters) getGameParameters();
        int playerID = getCurrentPlayer();
        double[] observationSpace = new double[16];
        PartialObservableDeck<LoveLetterCard> playerHandCards = getPlayerHandCards().get(playerID);

        // Player Hand Cards
        for (LoveLetterCard card : playerHandCards.getComponents()) {
            observationSpace[card.cardType.getValue() - 1] = 1;
        }

        // Draw Pile

        double noCards = 0;
        for (Integer cardAmount : params.cardCounts.values()){
            noCards += cardAmount;
        }
        observationSpace[8] = drawPile.getSize() / noCards;

        // Discard Piles
        int i = 9;
        for (PartialObservableDeck<LoveLetterCard> deck : getPlayerHandCards()) {
            observationSpace[i] += deck.getSize() / noCards;
            i++;
        }

        // Affection Tokens
        double nTokensWin = params.nTokensWin2;
        for (int j = 0; j < affectionTokens.length; j++) {
            observationSpace[13 + j] = affectionTokens[j] / nTokensWin;
        }

        return observationSpace;
    }

    @Override
    public int getObservationSpace() {
        return 16;
    }
}

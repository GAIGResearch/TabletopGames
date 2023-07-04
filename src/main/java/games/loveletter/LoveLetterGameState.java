package games.loveletter;

import core.AbstractParameters;
import core.AbstractGameState;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import core.interfaces.IStateFeatureJSON;
import evaluation.metrics.Event;
import games.GameType;
import games.loveletter.cards.LoveLetterCard;

import java.util.*;


public class LoveLetterGameState extends AbstractGameState implements IPrintable, IStateFeatureJSON {

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
     *
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
     *
     * @param playerDeck - deck of player to check
     * @return - card type of the card that forces the countess to be played, null if countess not forced
     */
    public LoveLetterCard.CardType needToForceCountess(Deck<LoveLetterCard> playerDeck) {
        boolean ownsCountess = false;
        for (LoveLetterCard card : playerDeck.getComponents()) {
            if (card.cardType == LoveLetterCard.CardType.Countess) {
                ownsCountess = true;
                break;
            }
        }

        if (ownsCountess) {
            for (LoveLetterCard card : playerDeck.getComponents()) {
                if (card.cardType == LoveLetterCard.CardType.Prince || card.cardType == LoveLetterCard.CardType.King) {
                    return card.cardType;
                }
            }
        }
        return null;
    }

    /**
     * Sets this player as dead and updates game and player status
     *
     * @param whoKill      - ID of player killing
     * @param targetPlayer - ID of player killed
     * @param cardType     - card used to kill
     */
    public void killPlayer(int whoKill, int targetPlayer, LoveLetterCard.CardType cardType) {
        setPlayerResult(CoreConstants.GameResult.LOSE_ROUND, targetPlayer);

        // a losing player needs to discard all cards
        while (playerHandCards.get(targetPlayer).getSize() > 0)
            playerDiscardCards.get(targetPlayer).add(playerHandCards.get(targetPlayer).draw());

        logEvent(Event.GameEvent.GAME_EVENT, "Killed player: " + whoKill + "," + targetPlayer + "," + cardType + "," + getCurrentPlayer());
    }

    // Getters, Setters
    public LoveLetterCard getRemovedCard() {
        return removedCard;
    }

    public Deck<LoveLetterCard> getReserveCards() {
        return reserveCards;
    }

    public boolean isProtected(int playerID) {
        return effectProtection[playerID];
    }

    public void setProtection(int playerID, boolean protection) {
        effectProtection[playerID] = protection;
    }

    public int getRemainingCards() {
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

        for (int i = 0; i < playerHandCards.size(); i++) {
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
        // [9-16]: discarded card types
        // [16-20]: Affection tokens per player

        int playerID = getCurrentPlayer();
        double[] observationSpace = new double[getObservationSpace()];
        PartialObservableDeck<LoveLetterCard> playerHandCards = getPlayerHandCards().get(playerID);

        // Player Hand Cards
        for (LoveLetterCard card : playerHandCards.getComponents()) {
            observationSpace[card.cardType.getValue() - 1] = 1;
        }

        // Draw Pile

        observationSpace[8] = drawPile.getSize();

        // Discard Piles
        int i = 9;
        for (Deck<LoveLetterCard> deck : getPlayerDiscardCards()) {
            for (LoveLetterCard card : deck.getComponents()) {
                observationSpace[i + card.cardType.getValue() - 1] += 1;
            }
//            observationSpace[i] += deck.getSize();
            i++;
        }

        // Affection Tokens
        for (int j = 0; j < affectionTokens.length; j++) {
            observationSpace[16 + j] = affectionTokens[j];
        }

        return observationSpace;

    }

    @Override
    public double[] getNormalizedObservationVector() {
        final double maxCards = 16;
        double[] results = getObservationVector();
        results[8] = results[8] / maxCards;
        for (int i = 0; i < LoveLetterCard.CardType.values().length; i++) {
            // todo 5 is the max, which is guard other cards only have 1 each - should get it somehow
            results[9+i] = results[9+i] / 5; // ((LoveLetterParameters) gameParameters).cardCounts.get(LoveLetterCard.CardType.values()[i]);
//            results[i] = LoveLetterCard.CardType.values()[i]
        }
        int nTokensWin = ((LoveLetterParameters) gameParameters).nTokensWin2;
        switch (nPlayers) {
            case 3:
                nTokensWin = ((LoveLetterParameters) gameParameters).nTokensWin3;
                break;
            case 4:
                nTokensWin = ((LoveLetterParameters) gameParameters).nTokensWin4;
                break;
        }
        for (int i = 0; i < 4; i++) {
            results[16+i] = results[16+i] / nTokensWin;
        }

        return results;
    }

    @Override
    public int getObservationSpace() {
        return 20;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"Players\":[");

        for (int i = 0; i < playerHandCards.size(); i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append("{\"Player Number\": ").append(i).append(",");
            if (getCurrentPlayer() == i)
                sb.append("\"Player Type\": \"Player\",");
            else
                sb.append("\"Player Type\": \"Opponent\",");

            sb.append("\"Cards\":[");
            generateJsonTextList(sb, playerHandCards.get(i).toString(this, getCurrentPlayer()));
            sb.append("],");

            sb.append("\"Discarded\":[");
            generateJsonTextList(sb, playerDiscardCards.get(i).toString());
            sb.append("],");

            sb.append("\"Protected\":").append(effectProtection[i]).append(",");
            sb.append("\"Affection\":").append(affectionTokens[i]).append(",");
            sb.append("\"Status\":\"").append(playerResults[i]).append("\"}");
        }

        sb.append("],");
        sb.append("\"DrawPile\":[");
        generateJsonTextList(sb, drawPile.toString(this, getCurrentPlayer()));
        sb.append("],");
        sb.append("\"ReserveCards\":[");
        generateJsonTextList(sb, reserveCards.toString());
        sb.append("],");
        sb.append("\"Current GamePhase\":\"").append(gamePhase).append("\"}");

        return sb.toString();
    }

    private void generateJsonTextList(StringBuilder sb, String cards) {
        String[] cardLists = cards.split(",");
        for (int j = 0; j < cardLists.length; j++) {
            if (j != 0) {
                sb.append(",");
            }
            sb.append("\"").append(cardLists[j]).append("\"");
        }
    }
}

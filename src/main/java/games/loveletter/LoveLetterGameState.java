package games.loveletter;

import core.AbstractParameters;
import core.AbstractGameState;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import evaluation.metrics.Event;
import games.GameType;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;

import java.util.*;


public class LoveLetterGameState extends AbstractGameState implements IPrintable {

    // List of cards in player hands
    List<PartialObservableDeck<LoveLetterCard>> playerHandCards;

    // Discarded cards
    List<Deck<LoveLetterCard>> playerDiscardCards;

    // Cards in draw pile
    PartialObservableDeck<LoveLetterCard> drawPile;

    // Cards in the reserve
    Deck<LoveLetterCard> reserveCards;
    LoveLetterCard removedCard;
    Random redeterminisationRnd = new Random(System.currentTimeMillis());

    boolean[] currentlyActive; // If true: player is currently active in the game (not knocked out)

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
        llgs.currentlyActive = currentlyActive.clone();
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
            llgs.drawPile.shuffle(redeterminisationRnd);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    // New random cards
                    PartialObservableDeck<LoveLetterCard> deck = llgs.playerHandCards.get(i);
                    for (int j = 0; j < deck.getSize(); j++) {
                        if (!deck.getVisibilityForPlayer(j, playerId)) {
                            llgs.playerHandCards.get(i).setComponent(j, llgs.drawPile.draw());
                        }
                    }
                    deck.shuffle(redeterminisationRnd);
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
    /** Returns the score of the player (the number of affection tokens) */
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
                Arrays.equals(affectionTokens, that.affectionTokens) &&
                Arrays.equals(currentlyActive, that.currentlyActive);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerHandCards, playerDiscardCards, drawPile, reserveCards, removedCard);
        result = 31 * result + Arrays.hashCode(effectProtection);
        result = 31 * result + Arrays.hashCode(affectionTokens);
        result = 31 * result + Arrays.hashCode(currentlyActive);
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
    public CardType needToForceCountess(Deck<LoveLetterCard> playerDeck) {
        boolean ownsCountess = false;
        for (LoveLetterCard card : playerDeck.getComponents()) {
            if (card.cardType == CardType.Countess) {
                ownsCountess = true;
                break;
            }
        }

        if (ownsCountess) {
            for (LoveLetterCard card : playerDeck.getComponents()) {
                if (card.cardType == CardType.Prince || card.cardType == CardType.King) {
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
    public void killPlayer(int whoKill, int targetPlayer, CardType cardType) {
        currentlyActive[targetPlayer] = false;

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

    /** Player is protected by the Handmaid */
    public boolean isProtected(int playerID) {
        return effectProtection[playerID];
    }

    public boolean isCurrentlyActive(int playerID) {
        return currentlyActive[playerID];
    }

    public void setProtection(int playerID, boolean protection) {
        effectProtection[playerID] = protection;
    }

    /** Number of cards left in the draw deck */
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

    /**
     * Returns the affection tokens for each player in an array.
     * The index of the array corresponds to the player ID.
     */
    public int[] getAffectionTokens() {
        return affectionTokens;
    }

    public List<Integer> getRoundWinners() {
        // Highest number in hand wins the round
        List<Integer> bestPlayers = new ArrayList<>();
        int bestValue = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            if (isCurrentlyActive(i)) {
                int points = playerHandCards.get(i).peek().cardType.getValue();
                if (points > bestValue) {
                    bestValue = points;
                    bestPlayers.clear();
                    bestPlayers.add(i);
                } else if (points == bestValue) {
                    bestPlayers.add(i);
                }
            }
        }

        if (bestPlayers.size() == 1) {
            // This is the winner of the round, 1 affection token
            return bestPlayers;
        } else {
            // If tie, add numbers in discard pile, highest wins
            bestValue = 0;
            List<Integer> bestPlayersByDiscardPoints = new ArrayList<>();
            for (int i : bestPlayers) {
                int points = 0;
                for (LoveLetterCard card : playerDiscardCards.get(i).getComponents()) {
                    points += card.cardType.getValue();
                }
                if (points > bestValue) {
                    bestValue = points;
                    bestPlayersByDiscardPoints.clear();
                    bestPlayersByDiscardPoints.add(i);
                } else if (points == bestValue) {
                    bestPlayersByDiscardPoints.add(i);
                }
            }
            // Everyone tied for most points here wins the round
            return bestPlayersByDiscardPoints;
        }
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

package games.uno;

import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.GameType;
import games.uno.cards.*;

import java.util.*;

import static games.uno.UnoGameParameters.UnoScoring.*;
import static games.uno.cards.UnoCard.UnoCardType.Wild;
import static core.CoreConstants.GameResult.*;

public class UnoGameState extends AbstractGameState implements IPrintable {
    List<Deck<UnoCard>> playerDecks;
    Deck<UnoCard> drawDeck;
    Deck<UnoCard> discardDeck;
    UnoCard currentCard;
    String currentColor;
    int[] playerScore;
    int[] expulsionRound;

    // Turn order data
    boolean skipTurn;
    int direction;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players for this game.
     */
    public UnoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Uno;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerDecks);
            add(drawDeck);
            add(discardDeck);
            add(currentCard);
        }};
    }

    boolean isWildCard(UnoCard card) {
        return card.type == Wild;
    }

    boolean isNumberCard(UnoCard card) {
        return card.type == UnoCard.UnoCardType.Number;
    }

    public void updateCurrentCard(UnoCard card) {
        currentCard = card;
        currentColor = card.color;
    }

    public void updateCurrentCard(UnoCard card, String color) {
        currentCard = card;
        currentColor = color;
    }

    public Deck<UnoCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<UnoCard> getDiscardDeck() {
        return discardDeck;
    }

    public List<Deck<UnoCard>> getPlayerDecks() {
        return playerDecks;
    }

    public UnoCard getCurrentCard() {
        return currentCard;
    }

    public String getCurrentColor() {
        return currentColor;
    }

    public int[] getPlayerScore() {
        return playerScore;
    }

    public int getNextPlayer() {
        int playersToMove = skipTurn ? 2 : 1;
        int nextOwner = turnOwner;
        do {
            nextOwner = (nPlayers + nextOwner + direction) % nPlayers;
            if (isNotTerminalForPlayer(nextOwner))
                playersToMove--;
        } while (playersToMove > 0);
        return nextOwner;
    }

    /**
     * Calculates points for all players, as sum of values of cards in the hands of all other players in the game.
     * Note that this is formally a UNO variant (but stops the game going on for ever)
     *
     * @param playerID - ID of player to calculate points for
     * @return - integer, point total
     */
    public int calculatePlayerPoints(int playerID, boolean selfOnly) {
        UnoGameParameters ugp = (UnoGameParameters) getGameParameters();
        int nPoints = 0;
        for (int otherPlayer = 0; otherPlayer < getNPlayers(); otherPlayer++) {
            if ((selfOnly && otherPlayer == playerID) || (!selfOnly && otherPlayer != playerID)) {
                for (UnoCard card : playerDecks.get(otherPlayer).getComponents()) {
                    switch (card.type) {
                        case Number:
                            nPoints += card.number;
                            break;
                        case Skip:
                            nPoints += ugp.nSkipPoints;
                            break;
                        case Reverse:
                            nPoints += ugp.nReversePoints;
                            break;
                        case Draw:
                            nPoints += ugp.nDraw2Points;
                            break;
                        case Wild:
                            if (card.drawN == 0) nPoints += ugp.nWildPoints;
                            else nPoints += ugp.nWildDrawPoints;
                            break;
                    }
                }
            }
        }
        return nPoints;
    }

    public boolean isSkipTurn() {
        return skipTurn;
    }

    public void setSkipTurn(boolean skipTurn) {
        this.skipTurn = skipTurn;
    }

    public int getDirection() {
        return direction;
    }

    public void reverseDirection() {
        this.direction *= -1;
    }

    @Override
    protected UnoGameState _copy(int playerId) {
        UnoGameState copy = new UnoGameState(gameParameters.copy(), getNPlayers());
        copy.playerDecks = new ArrayList<>();

        for (Deck<UnoCard> d : playerDecks) {
            copy.playerDecks.add(d.copy());
        }
        copy.drawDeck = drawDeck.copy();

        copy.skipTurn = skipTurn;
        copy.direction = direction;

        if (getCoreGameParameters().partialObservable && playerId != -1) {
            // Other player cards and the draw deck are unknown.
            // Combine all into one deck, shuffle, then deal random cards to the other players (hand size kept)
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    copy.drawDeck.add(copy.playerDecks.get(i));
                }
            }
            copy.drawDeck.shuffle(redeterminisationRnd);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    Deck<UnoCard> d = copy.playerDecks.get(i);
                    int nCards = d.getSize();
                    d.clear();
                    for (int j = 0; j < nCards; j++) {
                        d.add(copy.drawDeck.draw());
                    }
                }
            }
        }

        copy.discardDeck = discardDeck.copy();
        copy.currentCard = (UnoCard) currentCard.copy();
        copy.currentColor = currentColor;
        copy.playerScore = playerScore.clone();
        copy.expulsionRound = expulsionRound.clone();
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new UnoHeuristic().evaluateState(this, playerId);
    }

    /**
     * This provides the current score in game turns. This will only be relevant for games that have the concept
     * of victory points, etc.
     * If a game does not support this directly, then just return 0.0
     *
     * @param playerId - player ID to retrieve game score for
     * @return - double, score of current state
     */
    @Override
    public double getGameScore(int playerId) {
        // Only CLASSIC scoring has a positive score as beneficial
        // so we return the negative score to indicate a high score is poor
        int score = playerScore[playerId];
        UnoGameParameters ugp = (UnoGameParameters) gameParameters;

        if (gameStatus == GAME_ONGOING && ugp.scoringMethod != CLASSIC)
            score += calculatePlayerPoints(playerId, true);
        if (ugp.scoringMethod != CLASSIC)
            score = -score;
        return score;
    }

    @Override
    public int getOrdinalPosition(int playerId) {
        if (playerResults[playerId] == CoreConstants.GameResult.WIN_GAME)
            return 1;
        UnoGameParameters ugp = (UnoGameParameters) gameParameters;
        if (ugp.scoringMethod == CHALLENGE) {
            double playerScore = getGameScore(playerId);
            int ordinal = 1;
            for (int i = 0, n = getNPlayers(); i < n; i++) {
                if (expulsionRound[i] > expulsionRound[playerId]) {
                    ordinal++;
                } else if (expulsionRound[i] == expulsionRound[playerId]) {
                    double otherScore = getGameScore(i);
                    if (otherScore > playerScore)
                        ordinal++;
                    else if (otherScore == playerScore) {
                        if (getTiebreak(i, 1) > getTiebreak(playerId, 1))
                            ordinal++;
                    }
                }
            }
            return ordinal;
        } else {
            return super.getOrdinalPosition(playerId);
        }
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnoGameState)) return false;
        if (!super.equals(o)) return false;
        UnoGameState that = (UnoGameState) o;
        return skipTurn == that.skipTurn && direction == that.direction && Objects.equals(playerDecks, that.playerDecks) && Objects.equals(drawDeck, that.drawDeck) && Objects.equals(discardDeck, that.discardDeck) && Objects.equals(currentCard, that.currentCard) && Objects.equals(currentColor, that.currentColor) && Arrays.equals(playerScore, that.playerScore) && Arrays.equals(expulsionRound, that.expulsionRound);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerDecks, drawDeck, discardDeck, currentCard, currentColor, skipTurn, direction);
        result = 31 * result + Arrays.hashCode(playerScore);
        result = 31 * result + Arrays.hashCode(expulsionRound);
        return result;
    }

    @Override
    public void printToConsole() {

        String[] strings = new String[6];

        strings[0] = "----------------------------------------------------";
        strings[1] = "Current Card: " + currentCard.toString() + " [" + currentColor + "]";
        strings[2] = "----------------------------------------------------";

        strings[3] = "Player      : " + getCurrentPlayer();
        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand : ");

        for (UnoCard card : playerDecks.get(getCurrentPlayer()).getComponents()) {
            sb.append(card.toString());
            sb.append(" ");
        }
        strings[4] = sb.toString();
        strings[5] = "----------------------------------------------------";

        for (String s : strings) {
            System.out.println(s);
        }
    }
}


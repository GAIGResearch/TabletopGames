package games.uno;

import core.AbstractGameParameters;
import core.components.Component;
import core.components.Deck;
import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.uno.cards.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.PARTIAL_OBSERVABLE;
import static games.uno.cards.UnoCard.UnoCardType.Wild;

public class UnoGameState extends AbstractGameState implements IPrintable {
    List<Deck<UnoCard>>  playerDecks;
    Deck<UnoCard>        drawDeck;
    Deck<UnoCard>        discardDeck;
    UnoCard              currentCard;
    String               currentColor;
    int[]                playerScore;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - number of players for this game.
     */
    public UnoGameState(AbstractGameParameters gameParameters, int nPlayers) {
        super(gameParameters, new UnoTurnOrder(nPlayers));
    }

    @Override
    protected List<Component> _getAllComponents()
    {
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
        currentCard  = card;
        currentColor = card.color;
    }

    public void updateCurrentCard(UnoCard card, String color) {
        currentCard  = card;
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

    /**
     * Calculates points for all players, as sum of values of cards in the hands of all other players in the game.
     * @param playerID - ID of player to calculate points for
     * @return - integer, point total
     */
    public int calculatePlayerPoints(int playerID) {
        UnoGameParameters ugp = (UnoGameParameters) getGameParameters();
        int nPoints = 0;
        for (int otherPlayer = 0; otherPlayer < getNPlayers(); otherPlayer++) {
            if (otherPlayer != playerID) {
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

    @Override
    protected AbstractGameState _copy(int playerId) {
        UnoGameState copy = new UnoGameState(gameParameters.copy(), getNPlayers());
        copy.playerDecks = new ArrayList<>();

        for (Deck<UnoCard> d : playerDecks) {
            copy.playerDecks.add(d.copy());
        }
        copy.drawDeck = drawDeck.copy();

        if (PARTIAL_OBSERVABLE && playerId != -1) {
            // Other player cards and the draw deck are unknown.
            // Combine all into one deck, shuffle, then deal random cards to the other players (hand size kept)
            Random r = new Random(copy.gameParameters.getGameSeed());
            for (Deck<UnoCard> d: copy.playerDecks) {
                copy.drawDeck.add(d);
            }
            copy.drawDeck.shuffle(r);
            for (Deck<UnoCard> d: copy.playerDecks) {
                int nCards = d.getSize();
                d.clear();
                for (int i = 0; i < nCards; i++) {
                    d.add(copy.drawDeck.draw());
                }
            }
        }

        copy.discardDeck = discardDeck.copy();
        copy.currentCard = (UnoCard) currentCard.copy();
        copy.currentColor = currentColor;
        copy.playerScore = playerScore.clone();
        return copy;
    }

    @Override
    protected double _getScore(int playerId) {
        return new UnoHeuristic().evaluateState(this, playerId);
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>() {{
            add(drawDeck.getComponentID());
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    add(playerDecks.get(i).getComponentID());
                }
            }
        }};
    }

    @Override
    protected void _reset() {
        playerDecks = new ArrayList<>();
        drawDeck = null;
        discardDeck = null;
        currentCard = null;
        currentColor = null;
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

        for (String s : strings){
            System.out.println(s);
        }
    }
}


package games.uno;

import core.AbstractGameParameters;
import core.components.Deck;
import core.AbstractGameState;
import core.observations.VectorObservation;
import core.turnorders.TurnOrder;
import games.uno.cards.*;
import utilities.Utils;

import java.util.HashMap;
import java.util.List;

import static games.uno.cards.UnoCard.UnoCardType.Wild;

public class UnoGameState extends AbstractGameState {
    List<Deck<UnoCard>>  playerDecks;
    Deck<UnoCard>        drawDeck;
    Deck<UnoCard>        discardDeck;
    UnoCard              currentCard;
    String currentColor;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param turnOrder      - turn order for this game.
     */
    public UnoGameState(AbstractGameParameters gameParameters, TurnOrder turnOrder) {
        super(gameParameters, turnOrder);
    }

    public void addAllComponents()
    {
        allComponents.putComponents(playerDecks);
        allComponents.putComponent(drawDeck);
        allComponents.putComponent(discardDeck);
        allComponents.putComponent(currentCard);
    }

    boolean isWildCard(UnoCard card) {
        return card.type == Wild;
    }

    boolean isNumberCard(UnoCard card) {
        return card.type == UnoCard.UnoCardType.Number;
    }

//    @Override
//    public IObservation getObservation(int playerID) {
//        Deck<UnoCard> playerHand = playerDecks.get(playerID);
//        ArrayList<Integer> cardsLeft = new ArrayList<>();
//        for( int i = 0; i < getNPlayers(); i++) {
//            int nCards = playerDecks.get(playerID).getComponents().size();
//            cardsLeft.add(nCards);
//        }
//        return new UnoObservation(currentCard, currentColor, playerHand, discardDeck, playerID, cardsLeft);
//    }

    public int getCurrentPlayerID() {
        return turnOrder.getTurnOwner();
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

    @Override
    protected AbstractGameState copy(int playerId) {
        return null;
    }

    @Override
    public VectorObservation getVectorObservation() {
        return null;
    }

    @Override
    public double[] getDistanceFeatures(int playerId) {
        return new double[0];
    }

    @Override
    public HashMap<HashMap<Integer, Double>, Utils.GameResult> getTerminalFeatures(int playerId) {
        return null;
    }

    @Override
    public double getScore(int playerId) {
        return 0;
    }
}


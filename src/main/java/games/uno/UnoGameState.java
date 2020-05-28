package games.uno;

import core.components.Deck;
import core.AbstractGameState;
import core.AbstractGameParameters;
import core.observations.VectorObservation;
import games.uno.cards.*;
import utilities.Utils;

import java.util.HashMap;
import java.util.List;


public class UnoGameState extends AbstractGameState {
    List<Deck<UnoCard>>  playerDecks;
    Deck<UnoCard>        drawDeck;
    Deck<UnoCard>        discardDeck;
    UnoCard              currentCard;
    UnoCard.UnoCardColor currentColor;

    public UnoGameState(AbstractGameParameters gameParameters, int nPlayers){
        super(gameParameters, new UnoTurnOrder(nPlayers));
    }

    @Override
    public void addAllComponents() {
        allComponents.putComponent(drawDeck);
        allComponents.putComponent(discardDeck);
        allComponents.putComponent(currentCard);
        allComponents.putComponents(drawDeck.getComponents());
        allComponents.putComponents(discardDeck.getComponents());
        allComponents.putComponents(playerDecks);
        for (Deck<UnoCard> d: playerDecks) {
            allComponents.putComponents(d.getComponents());
        }
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

    boolean isWildCard(UnoCard card) {
        return card instanceof UnoWildCard || card instanceof UnoWildDrawFourCard;
    }

    boolean isNumberCard(UnoCard card) {
        return card instanceof UnoNumberCard;
    }

//    @Override
//    public AbstractGameState getObservation(int playerID) {
//        Deck<UnoCard> playerHand = playerDecks.get(playerID);
//        ArrayList<Integer> cardsLeft = new ArrayList<>();
//        for( int i = 0; i < getNPlayers(); i++) {
//            int nCards = playerDecks.get(playerID).getComponents().size();
//            cardsLeft.add(nCards);
//        }
//        return this;
//        return new UnoObservation(currentCard, currentColor, playerHand, discardDeck, playerID, cardsLeft);
//    }

    public int getCurrentPlayerID() {
        return turnOrder.getTurnOwner();
    }

    public void updateCurrentCard(UnoCard card) {
        currentCard  = card;
        currentColor = card.color;
    }

    public void updateCurrentCard(UnoCard card, UnoCard.UnoCardColor color) {
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

    public UnoCard.UnoCardColor getCurrentColor() {
        return currentColor;
    }
}


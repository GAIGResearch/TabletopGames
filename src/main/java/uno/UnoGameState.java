package uno;

import actions.Action;
import components.Card;
import components.Deck;
import core.Area;
import core.GameState;
import uno.Constants;

import java.util.ArrayList;
import java.util.List;

public class UnoGameState extends GameState {

    private int    direction = 1;
    private String mainColor;
    private int    mainNumber;

    public String GetMainColor()  { return mainColor; }
    public int    GetMainNumber() { return mainNumber; }

    @Override
    public GameState copy() {
        //TODO: copy uno game state
        return this;
    }

    @Override
    public GameState createNewGameState() {
        return new UnoGameState();
    }

    @Override
    public void copyTo(GameState dest, int playerId) {
        // TODO
    }

    @Override
    public int nPossibleActions() {
        // TODO
        return 1;
    }

    @Override
    public List<Action> possibleActions() {
        // TODO
        // Create a list for possible actions
        ArrayList<Action> actions = new ArrayList<>();
        return actions;
    }

    // UNo has one area for player and one common area
    public void setComponents() {
        UnoParameters parameters = (UnoParameters) this.gameParameters;

        // For each player, initialize their own areas
        for (int i = 0; i < nPlayers; i++) {
            Area playerArea = new Area();
            playerArea.setOwner(i);
            playerArea.addComponent(Constants.playerHandHash, new Deck(parameters.max_cards));
            areas.put(i, playerArea);
        }

        // Create common area
        Area commonArea = new Area();
        commonArea.setOwner(-1);
        commonArea.addComponent(Constants.drawDeckHash, new Deck(parameters.max_cards));
        commonArea.addComponent(Constants.mainDeckHash, new Deck(parameters.max_cards));
        areas.put(-1, commonArea);
    }

    public void skipTurn()
    {
        if (direction == 1) {
            activePlayer += 2;
            if (activePlayer >= nPlayers) {
                activePlayer = nPlayers - activePlayer;
            }
        }
        else {
            activePlayer -= 2;
            if (activePlayer < 0) {
                activePlayer = nPlayers + activePlayer;
            }
        }
    }

    public void changeDirection()
    {
        direction *= -1;
    }

    public void changeMainColor(String newColor)
    {
        mainColor = newColor;
    }

    public void changeMainNumber(int newNumber)
    {
        mainNumber = newNumber;
    }

    public void drawCardsFromDeck(Deck playerDeck, int nCards)
    {
        Deck drawnDeck = (Deck) findDeck("drawDeck");

        for (int i=0; i< nCards; i++) {
            // Remove from drawDeck
            Card c = drawnDeck.pick(0);

            // Add to playerDeck
            playerDeck.add(c);
        }
    }

    public void playCardOnHand(Deck playerDeck, int idCard)
    {
        Deck mainDeck = (Deck) findDeck("mainDeck");

        // Remover from playerDeck
        Card c = playerDeck.pick(idCard);

        // Add to mainDeck
        mainDeck.add(c);
    }
}

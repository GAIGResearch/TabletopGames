package games.uno;


import core.AbstractGameState;
import core.ForwardModel;
import core.GameParameters;
import core.actions.IAction;
import core.components.Deck;
import core.observations.IObservation;
import games.uno.actions.NoCards;
import games.uno.actions.PlayCard;
import games.uno.actions.PlayWild;
import games.uno.cards.*;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class UnoGameState extends AbstractGameState {
    public  List<Deck<UnoCard>>  playerDecks;
    private Deck<UnoCard>        drawDeck;
    private Deck<UnoCard>        discardDeck;
    public  UnoCard              currentCard;
    public  UnoCard.UnoCardColor currentColor;
    private int                  nPlayers;

    private final int initialNumberOfCardsForEachPLayer = 7;

    public UnoGameState(GameParameters gameParameters, ForwardModel model, int nPlayers){
        super(gameParameters, model, nPlayers, new UnoTurnOrder(nPlayers));

        this.nPlayers = nPlayers;
    }

    private void gameSetUp()
    {
        // Create the draw deck with all the cards
        drawDeck = new Deck<>("DrawDeck");
        CreateCards();

        // Shuffle the deck
        int seed = 1;                        // To be removed after debugging
        //drawDeck.shuffle();
        drawDeck.shuffle(new Random(seed));  // To be removed after debugging

        // Create the discard deck, at the beginning it is empty
        discardDeck = new Deck<>("DiscardDeck");

        playerDecks = new ArrayList<>(nPlayers);
        DrawCardsToPlayers();

        // get current card and set the current card and color
        currentCard  = drawDeck.draw();
        currentColor = currentCard.color;

        // The first card cannot be a wild.
        // In case, add to draw deck and shuffle again
        while (isWildCard(currentCard))
        {
            System.out.println("First card wild");
            seed ++;                        // To be removed after debugging
            drawDeck.add(currentCard);
            drawDeck.shuffle(new Random(seed));
            //drawDeck.shuffle();
            currentCard = drawDeck.draw();
            currentColor = currentCard.color;
        }

        // If the first card is Skip, Reverse or DrawTwo, play the card
        if (!isNumberCard(currentCard)) {
            System.out.println("First card no number " + currentColor.toString());
            if (currentCard instanceof UnoReverseCard) {
                reverseTurn();
            }
            else if (currentCard instanceof UnoDrawTwoCard) {
                int player = getCurrentPLayerID();
                playerDecks.get(player).add(drawDeck.draw());
                playerDecks.get(player).add(drawDeck.draw());
            }
            turnOrder.endPlayerTurn(this);
        }

        // add current card to discard deck
        discardDeck.add(currentCard);
    }

    private boolean isWildCard(UnoCard card) {
        return card instanceof UnoWildCard || card instanceof UnoWildDrawFourCard;
    }

    private boolean isNumberCard(UnoCard card) {
        return card instanceof UnoNumberCard;
    }

    // Create all the cards and include them into the drawPile
    private void CreateCards() {
        // Create the number cards
        for (UnoCard.UnoCardColor color : UnoCard.UnoCardColor.values()) {
            if (color == UnoCard.UnoCardColor.Wild)
                continue;

            // one card 0, two cards of 1, 2, ... 9
            for (int number = 0; number<10; number++) {
                drawDeck.add(new UnoNumberCard(color, number));
                if (number > 0)
                    drawDeck.add(new UnoNumberCard(color, number));
            }
        }

        // Create the DrawTwo, Reverse and Skip cards for each color
        for (UnoCard.UnoCardColor color : UnoCard.UnoCardColor.values()) {
            if (color == UnoCard.UnoCardColor.Wild)
                continue;

            drawDeck.add(new UnoSkipCard(color));
            drawDeck.add(new UnoSkipCard(color));
            drawDeck.add(new UnoReverseCard(color));
            drawDeck.add(new UnoReverseCard(color));
            drawDeck.add(new UnoDrawTwoCard(color));
            drawDeck.add(new UnoDrawTwoCard(color));
        }

        // Create the wild cards, 4 of each type
        for (int i = 0; i < 4; i++) {
            drawDeck.add(new UnoWildCard());
            drawDeck.add(new UnoWildDrawFourCard());
        }

    }

    private void DrawCardsToPlayers() {
        for (int player = 0; player < nPlayers; player++) {
            String playerDeckName = "Player" + player + "Deck";
            playerDecks.add(new Deck<>(playerDeckName));
            for (int card = 0; card < initialNumberOfCardsForEachPLayer; card++) {
                playerDecks.get(player).add(drawDeck.draw());
            }
        }
    }

    @Override
    public void endGame() {
        System.out.println("Game Results:");
        for (int playerID = 0; playerID < nPlayers; playerID++) {
            if (playerResults[playerID] == Utils.GameResult.GAME_WIN) {
                System.out.println("The winner is the player : " + playerID);
                break;
            }
        }
    }

    @Override
    public List<IAction> computeAvailableActions() {
        ArrayList<IAction> actions = new ArrayList<>();
        int player = getCurrentPLayerID();

        Deck<UnoCard> playerHand = playerDecks.get(player);
        for (UnoCard card : playerHand.getCards()) {
            if (card.isPlayable(this)) {
                if (isWildCard(card)) {
                    actions.add(new PlayWild<>(card, discardDeck, playerHand, UnoCard.UnoCardColor.Red));
                    actions.add(new PlayWild<>(card, discardDeck, playerHand, UnoCard.UnoCardColor.Blue));
                    actions.add(new PlayWild<>(card, discardDeck, playerHand, UnoCard.UnoCardColor.Green));
                    actions.add(new PlayWild<>(card, discardDeck, playerHand, UnoCard.UnoCardColor.Yellow));
                }
                else {
                    actions.add(new PlayCard<>(card, discardDeck, playerHand));
                }
            }
        }

        if (actions.isEmpty())
            actions.add(new NoCards(drawDeck, discardDeck, playerHand));

        return actions;
    }

    @Override
    public void setComponents() {
        gameSetUp();
    }

    @Override
    public IObservation getObservation(int playerID) {
        Deck<UnoCard> playerHand = playerDecks.get(playerID);
        return new UnoObservation(currentCard, currentColor, playerHand, playerID);
    }

    // The game is ended if there is a player without cards
    public void checkWinCondition() {
        for (int playerID = 0; playerID < nPlayers; playerID++) {
            int nCards = playerDecks.get(playerID).getCards().size();
            if (nCards == 0) {
                for (int i = 0; i < nPlayers; i++) {
                    if (i == playerID)
                        playerResults[i] = Utils.GameResult.GAME_WIN;
                    else
                        playerResults[i] = Utils.GameResult.GAME_LOSE;
                }
                gameStatus = Utils.GameResult.GAME_END;
            }
        }
    }

    public int getCurrentPLayerID() {
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

    public void endTurn() {
        turnOrder.endPlayerTurn(this);
    }

    public void reverseTurn() {
        ((UnoTurnOrder) turnOrder).reverse();
    }

    public void skipTurn() {
        ((UnoTurnOrder) turnOrder).skip();
    }

    public void drawTwo() {
        int nextPlayer = turnOrder.nextPlayer(this);
        for (int i=  0; i < 2; i ++) {
            if (drawDeck.getSize() == 0)
                drawDeckEmpty();
            playerDecks.get(nextPlayer).add(drawDeck.draw());
        }
        ((UnoTurnOrder) turnOrder).skip();
    }

    public void drawFour() {
        int nextPlayer = turnOrder.nextPlayer(this);
        for (int i = 0; i < 4; i ++) {
            if (drawDeck.getSize() == 0)
                drawDeckEmpty();
            playerDecks.get(nextPlayer).add(drawDeck.draw());
        }

        ((UnoTurnOrder) turnOrder).skip();
    }

    // When draw deck is empty, all the cards of discard deck (less the current one) are inserted in the draw deck
    // and it is shuffle
    public void drawDeckEmpty()
    {
        for (UnoCard card : discardDeck.getCards())
        {
            if (card != currentCard)
                drawDeck.add(card);
        }
        drawDeck.shuffle();
    }

    // Add a new action to next player, to believe or not to the previous player
    /*public void setTrick(UnoWildDrawFourCard isTrick) {

    }*/
}


package uno;


import core.AbstractGameState;
import core.ForwardModel;
import core.GameParameters;
import core.actions.IAction;
import core.components.Deck;
import core.observations.IObservation;
import uno.actions.NoCards;
import uno.actions.PlayCard;
import uno.actions.PlayWild;
import uno.cards.*;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class UnoGameState extends AbstractGameState {
    public  List<Deck<UnoCard>>  playerDecks;
    private Deck<UnoCard>        drawPile;
    private Deck<UnoCard>        discardPile;
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
        drawPile = new Deck<>();
        CreateCards();

        // Shuffle the deck
        int seed = 2;                        // To be removed after debugging
        // drawPile.shuffle();
        drawPile.shuffle(new Random(seed));  // To be removed after debugging

        // Create the discard deck, at the beginning it is empty
        discardPile = new Deck<>();

        playerDecks = new ArrayList<>(nPlayers);
        DrawCardsToPlayers();

        // get current card and set the current card and color
        currentCard  = drawPile.draw();
        currentColor = currentCard.color;

        // The first card cannot be a wild
        /*
        while (isWildCard(currentCard))
        {
            seed ++;                        // To be removed after debugging
            drawPile.add(currentCard);
            drawPile.shuffle(new Random(seed));
            currentCard = drawPile.draw();
        }

        discardPile.add(currentCard);
        currentColor = currentCard.color;
        currentNumber = currentCard.number;

          // If the first card is Skip, Reverse or DrawTwo, play the card
        if (!isNumberCard(currentCard)) {
            playFirstCard(currentCard);
        }
        */

    }

    private boolean isWildCard(UnoCard card) {
        return card instanceof UnoWildCard || card instanceof UnoWildDrawFourCard;
    }

    private boolean isNumberCard(UnoCard card) {
        return card instanceof UnoNumberCard;
    }

/*
    private void playFirstCard(UnoCard card) {
        if (card instanceof UnoSkipCard) {
            ((AlternatingTurnOrder) turnOrder).skip();
        }
        else if (card instanceof UnoReverseCard) {
            ((AlternatingTurnOrder) turnOrder).reverse();
        }
        else if (card instanceof UnoDrawTwoCard) {
            AbstractPlayer nextPlayer = ((AlternatingTurnOrder) turnOrder).getNextPlayer();
            int playerID = nextPlayer.playerID;

            playerDecks.get(playerID).add(drawPile.draw());
            playerDecks.get(playerID).add(drawPile.draw());
        }
    }
*/
    // Create all the cards and include them into the drawPile
    private void CreateCards() {
        // Create the number cards
        for (UnoCard.UnoCardColor color : UnoCard.UnoCardColor.values()) {
            if (color == UnoCard.UnoCardColor.Wild)
                continue;

            // one card 0, two cards of 1, 2, ... 9
            for (int number = 0; number< 10; number++) {
                drawPile.add(new UnoNumberCard(color, number));
                if (number > 0)
                    drawPile.add(new UnoNumberCard(color, number));
            }
        }

        // Create the DrawTwo, Reverse and Skip cards for each color
        for (UnoCard.UnoCardColor color : UnoCard.UnoCardColor.values()) {
            if (color == UnoCard.UnoCardColor.Wild)
                continue;

            drawPile.add(new UnoSkipCard(color));
            drawPile.add(new UnoSkipCard(color));
            drawPile.add(new UnoReverseCard(color));
            drawPile.add(new UnoReverseCard(color));
            drawPile.add(new UnoDrawTwoCard(color));
            drawPile.add(new UnoDrawTwoCard(color));
        }

        // Create the wild cards, 4 of each type
        for (int i = 0; i < 4; i++) {
            drawPile.add(new UnoWildCard());
            //drawPile.add(new UnoWildDrawFourCard()); TODO add this card
        }

    }

    private void DrawCardsToPlayers() {
        for (int player = 0; player < nPlayers; player++) {
            playerDecks.add(new Deck<>());
            for (int card = 0; card < initialNumberOfCardsForEachPLayer; card++) {
                playerDecks.get(player).add(drawPile.draw());
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
                    actions.add(new PlayWild<>(card, discardPile, playerHand, UnoCard.UnoCardColor.Red));
                    actions.add(new PlayWild<>(card, discardPile, playerHand, UnoCard.UnoCardColor.Blue));
                    actions.add(new PlayWild<>(card, discardPile, playerHand, UnoCard.UnoCardColor.Green));
                    actions.add(new PlayWild<>(card, discardPile, playerHand, UnoCard.UnoCardColor.Yellow));
                }
                else {
                    actions.add(new PlayCard<>(card, discardPile, playerHand));
                }
            }
        }

        if (actions.isEmpty())
            actions.add(new NoCards(drawPile, discardPile, playerHand));

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
        playerDecks.get(nextPlayer).add(drawPile.draw());
        playerDecks.get(nextPlayer).add(drawPile.draw());

        ((UnoTurnOrder) turnOrder).skip();
    }
}


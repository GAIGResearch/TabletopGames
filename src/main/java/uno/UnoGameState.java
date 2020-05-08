package uno;

import actions.IAction;
import components.Deck;
import core.AbstractGameState;
import core.GameParameters;
import turnorder.AlternatingTurnOrder;
import turnorder.TurnOrder;
import uno.actions.NoCards;
import uno.actions.PlayCard;
import uno.cards.*;
import observations.Observation;
import players.AbstractPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class UnoGameState extends AbstractGameState {
    public List<Deck<UnoCard>> playerDecks;
    private Deck<UnoCard> drawPile;
    private Deck<UnoCard> discardPile;

    public int currentNumber;
    public UnoCard.UnoCardColor currentColor;
    public UnoCard currentCard;

    public TurnOrder turnOrder;
    int nPlayers;

    private final int initialNumberOfCardsForEachPLayer = 3;

    public UnoGameState(GameParameters gameParameters, TurnOrder turnOrder) {
        super(gameParameters);
        this.turnOrder = turnOrder;
        nPlayers = gameParameters.nPlayers;

        gameSetUp(nPlayers);
    }

    private void gameSetUp(int nPlayers)
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

        // get current card and set the current number and color
        currentCard = drawPile.draw();

        // The first card cannot be a wild
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
    }

    private boolean isWildCard(UnoCard card) {
        return card instanceof UnoWildCard || card instanceof UnoWildDrawFourCard;
    }

    private boolean isNumberCard(UnoCard card) {
        return card instanceof UnoNumberCard;
    }

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

        /* SIMPLIFICATION ONLY WITH NUMBER CARDS
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
            drawPile.add(new UnoWildDrawFourCard());
        }
        */

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
    public Observation getObservation(AbstractPlayer player) {
        Deck<UnoCard> playerHand = playerDecks.get(player.playerID);
        return new UnoObservation(currentCard, currentColor, currentNumber, playerHand);
    }

    // TODO
    @Override
    public void endGame() {

    }

      // The game is ended if there is a player without cards
    public boolean isEnded() {
        for (int playerID = 0; playerID < nPlayers; playerID++) {
            int nCards = playerDecks.get(playerID).getCards().size();
            if (nCards == 0)
                return true;
        }
        return false;
    }

    // For each card on player hand, we add an action if the card is playable
    @Override
    public List<IAction> getActions(AbstractPlayer player) {
        ArrayList<IAction> actions = new ArrayList<>();

        Deck<UnoCard> playerHand = playerDecks.get(player.playerID);
        for (UnoCard card : playerHand.getCards()) {
            if (card.isPlayable(this))
                actions.add(new PlayCard<>(card, playerHand, discardPile));
        }

        if (actions.isEmpty())
            actions.add(new NoCards(drawPile, playerHand));

        return actions;
    }

    public int GetCurrentPLayerID()
    {
        return turnOrder.getCurrentPlayer(this).playerID;
    }

    public void UpdateCurrentCard(UnoCard card)
    {
        currentCard = card;
        currentColor = card.color;
        currentNumber = card.number;
    }

    public int getWinnerID()
    {
        for (int playerID = 0; playerID < nPlayers; playerID++) {
            int nCards = playerDecks.get(playerID).getCards().size();
            if (nCards == 0)
                return playerID;
        }
        return -1;
    }
}


package uno;

import actions.IAction;
import components.Deck;
import core.AbstractGameState;
import core.GameParameters;
import uno.actions.PlayCard;
import uno.cards.*;
import observations.Observation;
import players.AbstractPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class UnoGameState extends AbstractGameState {
    private List<Deck<UnoCard>> playerDecks;
    private Deck<UnoCard> drawPile;
    private Deck<UnoCard> discardPile;

    public int currentNumber;
    public UnoCard.UnoCardColor currentColor;
    private UnoCard currentCard;

    public UnoGameState(GameParameters gameParameters) {
        super(gameParameters);

        int nPlayers = gameParameters.nPlayers;

        // Create the draw deck with all the cards
        drawPile = new Deck<>();
        CreateCards();

        // Shuffle the deck
        int seed = 2;
        // drawPile.shuffle();
        drawPile.shuffle(new Random(seed));

        // Create the discard deck, at the beginning it is empty
        discardPile = new Deck<>();

        // Create a deck for each player and draw 7 cards to each one
        playerDecks = new ArrayList<>(nPlayers);

        for (int player = 0; player < nPlayers; player++) {
            playerDecks.add(new Deck<>());
            for (int card = 0; card < 7; card++) {
                playerDecks.get(player).add(drawPile.draw());
            }
        }

        // get current card and set the current number and color
        currentCard = drawPile.draw();
        discardPile.add(currentCard);
        currentColor = currentCard.color;
        currentNumber = currentCard.number;
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
    }

    @Override
    public Observation getObservation(AbstractPlayer player) {
        Deck<UnoCard> playerHand = playerDecks.get(player.playerID);
        return new UnoObservation(currentCard, currentColor, currentNumber, playerHand);
    }

    @Override
    public void endGame() {

    }

    // TODO
    // The game is ended if there is a player without cards
    public boolean isEnded() {
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

        return actions;
    }
}


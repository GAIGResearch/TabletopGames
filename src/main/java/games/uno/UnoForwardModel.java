package games.uno;

import core.actions.AbstractAction;
import core.AbstractGameState;
import core.AbstractForwardModel;
import core.components.Deck;
import games.uno.cards.*;
import utilities.Utils;

import java.util.ArrayList;

import static core.CoreConstants.VERBOSE;

public class UnoForwardModel extends AbstractForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {
        UnoGameState ugs = (UnoGameState) firstState;

        // Create the draw deck with all the cards
        ugs.drawDeck = new Deck<>("DrawDeck");
        createCards(ugs);

        // Shuffle the deck
        ugs.drawDeck.shuffle();

        // Create the discard deck, at the beginning it is empty
        ugs.discardDeck = new Deck<>("DiscardDeck");

        ugs.playerDecks = new ArrayList<>(ugs.getNPlayers());
        drawCardsToPlayers(ugs);

        // get current card and set the current card and color
        ugs.currentCard  = ugs.drawDeck.draw();
        ugs.currentColor = ugs.currentCard.color;

        // The first card cannot be a wild.
        // In case, add to draw deck and shuffle again
        while (ugs.isWildCard(ugs.currentCard))
        {
            if (VERBOSE) {
                System.out.println("First card wild");
            }
            ugs.drawDeck.add(ugs.currentCard);
            ugs.drawDeck.shuffle();
            ugs.currentCard = ugs.drawDeck.draw();
            ugs.currentColor = ugs.currentCard.color;
        }

        // If the first card is Skip, Reverse or DrawTwo, play the card
        if (!ugs.isNumberCard(ugs.currentCard)) {
            if (VERBOSE) {
                System.out.println("First card no number " + ugs.currentColor);
            }
            if (ugs.currentCard.type == UnoCard.UnoCardType.Reverse) {
                ((UnoTurnOrder) ugs.getTurnOrder()).reverse();
            }
            else if (ugs.currentCard.type == UnoCard.UnoCardType.Draw) {
                int player = ugs.getCurrentPlayerID();
                ugs.playerDecks.get(player).add(ugs.drawDeck.draw());
                ugs.playerDecks.get(player).add(ugs.drawDeck.draw());
            }
            ugs.getTurnOrder().endPlayerTurn(ugs);
        }

        // add current card to discard deck
        ugs.discardDeck.add(ugs.currentCard);
    }

    @Override
    public void next(AbstractGameState gameState, AbstractAction action) {
        action.execute(gameState);
        checkGameEnd((UnoGameState)gameState);
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING)
            gameState.getTurnOrder().endPlayerTurn(gameState);
    }

    // Create all the cards and include them into the drawPile
    private void createCards(UnoGameState ugs) {
        UnoGameParameters ugp = (UnoGameParameters)ugs.getGameParameters();
        for (String color : ugp.colors) {
            if (!color.equals("Wild")) {

                // Create the number cards
                for (int number = 0; number < ugp.nNumberCards; number++) {
                    ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Number, color, number));
                    if (number > 0)
                        ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Number, color, number));
                }

                // Create the DrawTwo, Reverse and Skip cards for each color
                for (int i = 0; i < ugp.nSkipCards; i++) {
                    ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Skip, color));
                }
                for (int i = 0; i < ugp.nReverseCards; i++) {
                    ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Reverse, color));
                }
                for (int i = 0; i < ugp.nDrawCards; i++) {
                    for (int n : ugp.specialDrawCards) {
                        ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Draw, color, n));
                    }
                }
            }
        }

        // Create the wild cards, N of each type
        for (int i = 0; i < ugp.nWildCards; i++) {
            for (int n : ugp.specialWildDrawCards) {
                ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Wild, "Wild", n));
            }
        }
    }

    private void drawCardsToPlayers(UnoGameState ugs) {
        for (int player = 0; player < ugs.getNPlayers(); player++) {
            String playerDeckName = "Player" + player + "Deck";
            ugs.playerDecks.add(new Deck<>(playerDeckName));
            for (int card = 0; card < ((UnoGameParameters)ugs.getGameParameters()).nCardsPerPlayer; card++) {
                ugs.playerDecks.get(player).add(ugs.drawDeck.draw());
            }
        }
    }

    // The game is ended if there is a player without cards
    private void checkGameEnd(UnoGameState ugs) {
        for (int playerID = 0; playerID < ugs.getNPlayers(); playerID++) {
            int nCards = ugs.playerDecks.get(playerID).getComponents().size();
            if (nCards == 0) {
                for (int i = 0; i < ugs.getNPlayers(); i++) {
                    if (i == playerID)
                        ugs.setPlayerResult(Utils.GameResult.GAME_WIN, i);
                    else
                        ugs.setPlayerResult(Utils.GameResult.GAME_LOSE, i);
                }
                ugs.setGameStatus(Utils.GameResult.GAME_END);
            }
        }
    }

}


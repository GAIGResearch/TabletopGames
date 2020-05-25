package games.uno;

import core.actions.AbstractAction;
import core.AbstractGameState;
import core.ForwardModel;
import core.components.Deck;
import games.uno.cards.UnoCard;
import games.uno.cards.UnoNumberCard;
import games.uno.cards.UnoReverseCard;
import games.uno.cards.UnoSkipCard;
import utilities.Utils;

import java.util.ArrayList;

public class UnoForwardModel extends ForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {
        UnoGameState ugs = (UnoGameState) firstState;
        UnoGameParameters ugp = (UnoGameParameters) firstState.getGameParameters();

        ugs.drawPile = new Deck<>("Draw Pile");

        for (UnoCard.UnoCardColor color : UnoCard.UnoCardColor.values())
        {
            if (color == UnoCard.UnoCardColor.Wild)
                continue;

            for (int i = 0; i < ugp.nCardsPerColor; i++)
            {
                ugs.drawPile.add(new UnoNumberCard(color, UnoCard.UnoCardType.Number, i));
                if (i > 0)
                    ugs.drawPile.add(new UnoNumberCard(color, UnoCard.UnoCardType.Number, i));
            }
            ugs.drawPile.add(new UnoSkipCard(color, UnoCard.UnoCardType.Skip));
            ugs.drawPile.add(new UnoSkipCard(color, UnoCard.UnoCardType.Skip));
            ugs.drawPile.add(new UnoReverseCard(color, UnoCard.UnoCardType.Reverse));
            ugs.drawPile.add(new UnoReverseCard(color, UnoCard.UnoCardType.Reverse));
        }

        ugs.drawPile.shuffle();
        // todo add action cards step-by-step

        ugs.discardPile = new Deck<>("Discard Pile");

        ugs.playerDecks = new ArrayList<>(ugs.getNPlayers());
        for (int i = 0; i < ugs.getNPlayers(); i++){
            ugs.playerDecks.add(new Deck<>("Player Deck"));
            for (int j = 0; j < ugp.nCardsPerPlayer; j++){
                ugs.playerDecks.get(i).add(ugs.drawPile.draw());
            }
        }

        ugs.currentCard = ugs.drawPile.draw();
        ugs.discardPile.add(ugs.currentCard);
    }

    @Override
    public void next(AbstractGameState gameState, AbstractAction action) {
        action.execute(gameState);
        gameState.getTurnOrder().endPlayerTurn(gameState);
        UnoGameParameters ugp = (UnoGameParameters) gameState.getGameParameters();

        if (gameState.getTurnOrder().getRoundCounter() == ugp.maxRounds) {
            gameState.setGameStatus(Utils.GameResult.GAME_END);
        }

        checkGameEnd((UnoGameState) gameState);
    }

    /**
     * Checks if this game has ended.
     * @param gameState - game state to check for end game.
     */
    public void checkGameEnd(UnoGameState gameState) {
        for (int i = 0; i < gameState.getNPlayers(); i++)
        {
            if (gameState.playerDecks.get(i).getSize() == 0)
                gameState.registerWinner(i);
        }
    }


}

package games.uno;

import core.ForwardModel;
import core.actions.IAction;
import core.components.Deck;
import core.AbstractGameState;
import core.GameParameters;
import core.turnorder.AlternatingTurnOrder;
import games.uno.cards.*;
import core.observations.IObservation;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnoGameState extends AbstractGameState {
    List<Deck<UnoCard>> playerDecks;
    Deck<UnoCard> drawPile;
    Deck<UnoCard> discardPile;
    UnoCard currentCard;

    public UnoGameState(GameParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, model, new AlternatingTurnOrder(nPlayers));
    }

    @Override
    public IObservation getObservation(int player) {

        int[] cardsPerPlayer = new int[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++)
            cardsPerPlayer[i] = playerDecks.get(i).getSize();

        return new UnoObservation(currentCard, playerDecks.get(player), discardPile,
                cardsPerPlayer, drawPile.getSize());
    }

    @Override
    public void endGame() {
        gameStatus = Utils.GameResult.GAME_DRAW;
        Arrays.fill(playerResults, Utils.GameResult.GAME_DRAW);
    }

    @Override
    public List<IAction> computeAvailableActions() {
        ArrayList<IAction> actions = new ArrayList<>();
        int player = turnOrder.getCurrentPlayer(this);
        Deck<UnoCard> playerDeck = playerDecks.get(player);
        for (UnoCard card : playerDeck.getCards()){
            if (card.isPlayable(this))
            {
                if (card instanceof UnoNumberCard)
                    actions.add(new PlayCard<>(card, playerDeck, discardPile));
                if (card instanceof UnoSkipCard)
                    actions.add(new PlayCard<>(card, playerDeck, discardPile, new UnoSkipCard.SkipCardEffect()));
                if (card instanceof UnoReverseCard)
                    actions.add(new PlayCard<>(card, playerDeck, discardPile, new UnoReverseCard.ReverseCardEffect()));
            }
        }
        actions.add(new DrawCards<>(drawPile, playerDecks.get(player), discardPile, 1));
        return actions;
    }

    /**
     * Inform the game state this player has won.
     * @param playerID - ID of player who won.
     */
    public void registerWinner(int playerID){
        gameStatus = Utils.GameResult.GAME_END;
        for (int i = 0; i < getNPlayers(); i++)
        {
            if (i == playerID)
                playerResults[i] = Utils.GameResult.GAME_WIN;
            else
                playerResults[i] = Utils.GameResult.GAME_LOSE;
        }
    }

    public Deck<UnoCard> getDiscardPile() {
        return discardPile;
    }

    public Deck<UnoCard> getDrawPile() {
        return drawPile;
    }

    public List<Deck<UnoCard>> getPlayerDecks() {
        return playerDecks;
    }

    public UnoCard getCurrentCard() {
        return currentCard;
    }
}

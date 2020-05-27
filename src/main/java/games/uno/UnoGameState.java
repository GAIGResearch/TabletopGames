package games.uno;

import core.ForwardModel;
import core.actions.AbstractAction;
import core.actions.DrawCard;
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

    @Override
    public void addAllComponents() {
        allComponents.putComponent(drawPile);
        allComponents.putComponent(discardPile);
        allComponents.putComponent(currentCard);
        allComponents.putComponents(drawPile.getComponents());
        allComponents.putComponents(discardPile.getComponents());
        allComponents.putComponents(playerDecks);
        for (Deck<UnoCard> d: playerDecks) {
            allComponents.putComponents(d.getComponents());
        }
    }

    public UnoGameState(GameParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers));
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
    public List<AbstractAction> computeAvailableActions() {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = turnOrder.getCurrentPlayer(this);
        Deck<UnoCard> playerDeck = playerDecks.get(player);
        for (int c = 0; c < playerDeck.getSize(); c++){
            UnoCard card = playerDeck.getComponents().get(c);
            if (card.isPlayable(this))
            {
                if (card instanceof UnoNumberCard)
                    actions.add(new PlayCard(playerDeck.getComponentID(), discardPile.getComponentID(), c));
                if (card instanceof UnoSkipCard)
                    actions.add(new PlayCard(playerDeck.getComponentID(), discardPile.getComponentID(), c, new UnoSkipCard.SkipCardEffect()));
                if (card instanceof UnoReverseCard)
                    actions.add(new PlayCard(playerDeck.getComponentID(), discardPile.getComponentID(), c, new UnoReverseCard.ReverseCardEffect()));
            }
        }
        actions.add(new DrawCard(drawPile.getComponentID(), playerDecks.get(player).getComponentID(), 0));
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

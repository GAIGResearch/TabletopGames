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
    public UnoCard currentCard;

    public UnoGameState(GameParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, model, nPlayers, new AlternatingTurnOrder(nPlayers));

        drawPile = new Deck<>("Draw Pile");

        for (UnoCard.UnoCardColor color : UnoCard.UnoCardColor.values())
        {
            if (color == UnoCard.UnoCardColor.Wild)
                continue;

            for (int i = 0; i < 10; i++)
            {
                drawPile.add(new UnoNumberCard(color, UnoCard.UnoCardType.Number, i));
                if (i > 0)
                    drawPile.add(new UnoNumberCard(color, UnoCard.UnoCardType.Number, i));
            }
            drawPile.add(new UnoSkipCard(color, UnoCard.UnoCardType.Skip));
            drawPile.add(new UnoSkipCard(color, UnoCard.UnoCardType.Skip));
            drawPile.add(new UnoReverseCard(color, UnoCard.UnoCardType.Reverse));
            drawPile.add(new UnoReverseCard(color, UnoCard.UnoCardType.Reverse));
        }

        drawPile.shuffle();
        // todo add action cards step-by-step

        discardPile = new Deck<>("Discard Pile");

        playerDecks = new ArrayList<>(getNPlayers());
        for (int i = 0; i < getNPlayers(); i++){
            playerDecks.add(new Deck<>("Player Deck"));
            for (int j = 0; j < 7; j++){
                playerDecks.get(i).add(drawPile.draw());
            }
        }

        currentCard = drawPile.draw();
        discardPile.add(currentCard);
    }

    @Override
    public IObservation getObservation(int player) {

        int[] cardsPerPlayer = new int[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++)
            cardsPerPlayer[i] = playerDecks.get(i).getElements().size();

        return new UnoObservation(currentCard, playerDecks.get(player), discardPile,
                cardsPerPlayer, drawPile.getElements().size());
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
        for (UnoCard card : playerDeck.getElements()){
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
}

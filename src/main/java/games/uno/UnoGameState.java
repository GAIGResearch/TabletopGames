package games.uno;

import core.actions.IAction;
import core.components.Deck;
import core.AbstractGameState;
import core.GameParameters;
import core.gamestates.PlayerResult;
import core.turnorder.AlternatingTurnOrder;
import games.uno.cards.*;
import core.observations.Observation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnoGameState extends AbstractGameState {
    List<Deck<UnoCard>> playerDecks;
    Deck<UnoCard> drawPile;
    Deck<UnoCard> discardPile;
    public UnoCard currentCard;

    public UnoGameState(GameParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        setTurnOrder(new AlternatingTurnOrder(nPlayers));

        drawPile = new Deck<>();

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

        discardPile = new Deck<>();

        playerDecks = new ArrayList<>(getNPlayers());
        for (int i = 0; i < getNPlayers(); i++){
            playerDecks.add(new Deck<>());
            for (int j = 0; j < 7; j++){
                playerDecks.get(i).add(drawPile.draw());
            }
        }

        currentCard = drawPile.draw();
        discardPile.add(currentCard);
    }

    @Override
    public Observation getObservation(int player) {

        int[] cardsPerPlayer = new int[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++)
            cardsPerPlayer[i] = playerDecks.get(i).getCards().size();

        return new UnoObservation(currentCard, playerDecks.get(player), discardPile,
                cardsPerPlayer, drawPile.getCards().size());
    }

    @Override
    public void endGame() {
        terminalState = true;
        Arrays.fill(playerResults, PlayerResult.Draw);
    }

    @Override
    public List<IAction> computeAvailableActions(int player) {
        ArrayList<IAction> actions = new ArrayList<>();
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

    public void registerWinner(int playerID){
        terminalState = true;
        for (int i = 0; i < getNPlayers(); i++)
        {
            if (i == playerID)
                playerResults[i] = PlayerResult.Winner;
            else
                playerResults[i] = PlayerResult.Loser;
        }
    }
}

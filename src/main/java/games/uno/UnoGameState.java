package games.uno;

import actions.IAction;
import components.Deck;
import core.AbstractGameState;
import core.GameParameters;
import gamestates.PlayerResult;
import games.uno.cards.*;
import observations.Observation;
import players.AbstractPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnoGameState extends AbstractGameState {
    List<Deck<UnoCard>> playerDecks;
    Deck<UnoCard> drawPile;
    Deck<UnoCard> discardPile;
    public UnoCard currentCard;

    public UnoGameState(GameParameters gameParameters) {
        super(gameParameters);

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
    public Observation getObservation(AbstractPlayer player) {

        int[] cardsPerPlayer = new int[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++)
            cardsPerPlayer[i] = playerDecks.get(i).getCards().size();

        return new UnoObservation(currentCard, playerDecks.get(player.playerID), discardPile,
                cardsPerPlayer, drawPile.getCards().size());
    }

    @Override
    public List<IAction> getActions(AbstractPlayer player) {
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<UnoCard> playerDeck = playerDecks.get(player.playerID);
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
        actions.add(new DrawCards<>(drawPile, playerDecks.get(player.playerID), discardPile, 1));
        return actions;
    }

    @Override
    public void endGame() {
        terminalState = true;
        Arrays.fill(playerResults, PlayerResult.Draw);
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

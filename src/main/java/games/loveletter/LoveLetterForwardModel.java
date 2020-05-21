package games.loveletter;

import core.AbstractGameState;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.gamephase.DefaultGamePhase;
import core.ForwardModel;
import core.actions.IAction;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static games.loveletter.LoveLetterGameState.LoveLetterGamePhase.Draw;
import static utilities.CoreConstants.PARTIAL_OBSERVABLE;
import static utilities.CoreConstants.VERBOSE;

public class LoveLetterForwardModel extends ForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {
        LoveLetterGameState llgs = (LoveLetterGameState)firstState;
        LoveLetterParameters llp = (LoveLetterParameters)firstState.getGameParameters();

        llgs.drawPile = new PartialObservableDeck<>("drawPile", llgs.getNPlayers());
        llgs.effectProtection = new boolean[llgs.getNPlayers()];

        // Add all cards to the draw pile
        for (HashMap.Entry<LoveLetterCard.CardType, Integer> entry : llp.cardCounts.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                LoveLetterCard card = new LoveLetterCard(entry.getKey());
                llgs.drawPile.add(card);
            }
        }

        llgs.reserveCards = new PartialObservableDeck<>("reserveCards", llgs.getNPlayers());
        llgs.drawPile.shuffle();
        llgs.reserveCards.add(llgs.drawPile.draw());

        // Give each player a single card
        llgs.playerHandCards = new ArrayList<>(llgs.getNPlayers());
        llgs.playerDiscardCards = new ArrayList<>(llgs.getNPlayers());
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            boolean[] visibility = new boolean[llgs.getNPlayers()];
            Arrays.fill(visibility, !PARTIAL_OBSERVABLE);
            visibility[i] = true;

            PartialObservableDeck<LoveLetterCard> playerCards = new PartialObservableDeck<>("playerHand" + i, visibility);
            for (int j = 0; j < llp.nCardsPerPlayer; j++) {
                playerCards.add(llgs.drawPile.draw());
            }
            llgs.playerHandCards.add(playerCards);

            Arrays.fill(visibility, true);
            Deck<LoveLetterCard> discardCards = new Deck<>("discardPlayer" + i);
            llgs.playerDiscardCards.add(discardCards);
        }
    }

    @Override
    public void next(AbstractGameState gameState, IAction action) {
        if (VERBOSE) {
            System.out.println(action.toString());
        }

        LoveLetterGameState llgs = (LoveLetterGameState) gameState;
        action.execute(gameState);
        if (llgs.getGamePhase() == Draw)
            llgs.setGamePhase(DefaultGamePhase.Main);
        else{
            llgs.setGamePhase(Draw);
            checkEndOfGame(llgs);
            if (llgs.getGameStatus() != Utils.GameResult.GAME_END)
                llgs.getTurnOrder().endPlayerTurn(gameState);
        }
    }

    /**
     * Checks all game end conditions for the game.
     * @param llgs - game state to check if terminal.
     */
    private void checkEndOfGame(LoveLetterGameState llgs) {
        int playersAlive = 0;
        for (Utils.GameResult result : llgs.getPlayerResults())
            if (result != Utils.GameResult.GAME_LOSE)
                playersAlive += 1;

        if (playersAlive == 1) {
            // game ends because only a single player is left
            llgs.setGameStatus(Utils.GameResult.GAME_END);
        }
        else if (llgs.getRemainingCards() == 0){
            // game needs to end because their are no cards left
            llgs.setGameStatus(Utils.GameResult.GAME_END);
        }
    }
}

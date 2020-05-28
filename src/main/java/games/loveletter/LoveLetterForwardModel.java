package games.loveletter;

import core.AbstractGameState;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.interfaces.IGamePhase;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static games.loveletter.LoveLetterGameState.LoveLetterGamePhase.Draw;
import static core.CoreConstants.PARTIAL_OBSERVABLE;
import static core.CoreConstants.VERBOSE;


public class LoveLetterForwardModel extends AbstractForwardModel {

    /**
     * Creates the initial game-state of Love Letter.
     * @param firstState - state to be modified
     */
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

        // Put one card to the side, such that player's won't know all cards in the game
        llgs.reserveCards = new PartialObservableDeck<>("reserveCards", llgs.getNPlayers());
        llgs.drawPile.shuffle();
        llgs.reserveCards.add(llgs.drawPile.draw());

        // Give each player a single card
        llgs.playerHandCards = new ArrayList<>(llgs.getNPlayers());
        llgs.playerDiscardCards = new ArrayList<>(llgs.getNPlayers());
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            // Setup player deck to be fully/partial observable
            boolean[] visibility = new boolean[llgs.getNPlayers()];
            Arrays.fill(visibility, !PARTIAL_OBSERVABLE);
            visibility[i] = true;

            // add a single random card to the player's hand
            PartialObservableDeck<LoveLetterCard> playerCards = new PartialObservableDeck<>("playerHand" + i,
                    visibility.clone());
            for (int j = 0; j < llp.nCardsPerPlayer; j++) {
                playerCards.add(llgs.drawPile.draw());
            }
            llgs.playerHandCards.add(playerCards);

            // create a player's discard pile, which is visible to all players
            Arrays.fill(visibility, true);
            Deck<LoveLetterCard> discardCards = new Deck<>("discardPlayer" + i);
            llgs.playerDiscardCards.add(discardCards);
        }

        llgs.setGamePhase(Draw);
    }

    @Override
    public void next(AbstractGameState gameState, AbstractAction action) {
        if (VERBOSE) {
            System.out.println(action.toString());
        }

        // each turn begins with the player drawing a card after which one card will be played
        // switch the phase after each executed action
        LoveLetterGameState llgs = (LoveLetterGameState) gameState;
        action.execute(gameState);

        IGamePhase gamePhase = llgs.getGamePhase();
        if (gamePhase == Draw)
            llgs.setGamePhase(AbstractGameState.DefaultGamePhase.Main);
        else if (gamePhase == AbstractGameState.DefaultGamePhase.Main){
            llgs.setGamePhase(Draw);
            checkEndOfGame(llgs);
            if (llgs.getGameStatus() != Utils.GameResult.GAME_END)
                llgs.getTurnOrder().endPlayerTurn(gameState);
        } else
            throw new IllegalArgumentException("The gamestate " + llgs.getGamePhase() +
                    " is not know by LoveLetterForwardModel");
    }

    /**
     * Checks all game end conditions for the game.
     * @param llgs - game state to check if terminal.
     */
    private void checkEndOfGame(LoveLetterGameState llgs) {
        // count the number of active players
        int playersAlive = 0;
        for (Utils.GameResult result : llgs.getPlayerResults())
            if (result != Utils.GameResult.GAME_LOSE)
                playersAlive += 1;

        // game ends because only a single player is left
        if (playersAlive == 1) {
            llgs.setGameStatus(Utils.GameResult.GAME_END);
        }
        else if (llgs.getRemainingCards() == 0){
            // game needs to end because their are no cards left
            llgs.setGameStatus(Utils.GameResult.GAME_END);
        }
    }
}

package games.sushigo;

import core.AbstractGameState;
import core.components.Deck;
import core.turnorders.StandardTurnOrder;
import games.sushigo.cards.SGCard;
import utilities.Utils;

import java.util.Random;

public class SGTurnOrder extends StandardTurnOrder {
    public SGTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
    }
    public SGTurnOrder() {}

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if(gameState.getGameStatus() != Utils.GameResult.GAME_ONGOING) return;
        turnCounter++;
        moveToNextPlayer(gameState, nextPlayer(gameState));
    }

    @Override
    protected SGTurnOrder _copy() {
        return new SGTurnOrder();
    }

    @Override
    public void _endRound(AbstractGameState gameState) {
        SGGameState gs = (SGGameState) gameState;

        // Apply card end of round rules
        for (SGCard.SGCardType type: SGCard.SGCardType.values()) {
            type.onRoundEnd(gs);
        }

        // Clear played hands if they get discarded between rounds, they go in the discard pile
        for (int i = 0; i < gs.getNPlayers(); i++) {
            Deck<SGCard> cardsToKeep = gs.playedCards.get(i).copy();
            cardsToKeep.clear();
            for (SGCard card : gs.playedCards.get(i).getComponents()) {
                if (card.type.isDiscardedBetweenRounds()) {
                    gs.discardPile.add(card);
                    gs.playedCardTypes[i].get(card.type).setValue(0);
                } else {
                    cardsToKeep.add(card);
                }
            }
            gs.playedCards.get(i).clear();
            gs.playedCards.get(i).add(cardsToKeep);
        }
    }

    @Override
    public void _startRound(AbstractGameState gameState) {
        SGGameState gs = (SGGameState) gameState;

        //Draw new hands for players
        for (int i = 0; i < gs.getNPlayers(); i++){
            for (int j = 0; j < gs.nCardsInHand; j++)
            {
                if (gs.drawPile.getSize() == 0) {
                    // Reshuffle discard into draw pile
                    gs.drawPile.add(gs.discardPile);
                    gs.discardPile.clear();
                    gs.drawPile.shuffle(new Random(gs.getGameParameters().getRandomSeed()));
                }
                gs.playerHands.get(i).add(gs.drawPile.draw());
            }
            gs.deckRotations = 0;
        }
    }
}

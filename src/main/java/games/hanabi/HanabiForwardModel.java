package games.hanabi;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.Counter;
import core.components.PartialObservableDeck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import games.hanabi.actions.Discard;
import games.hanabi.actions.Play;
import games.hanabi.actions.Hint;

import static core.CoreConstants.VisibilityMode.*;
import static core.CoreConstants.GameResult.*;

public class HanabiForwardModel extends StandardForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        HanabiGameState hbgs = (HanabiGameState) firstState;
        HanabiParameters hbp = (HanabiParameters) hbgs.getGameParameters();
        hbgs.playerDecks = new ArrayList<>();
        for (int i = 0; i < hbgs.getNPlayers(); i++) {
            boolean[] visibility = new boolean[hbgs.getNPlayers()];
            for (int j = 0; j < hbgs.getNPlayers(); j++){
                visibility[j] = i != j;
            }
            hbgs.playerDecks.add(new PartialObservableDeck<>("Player" + i, i, visibility));
        }
        hbgs.drawDeck = new Deck<>("DrawDeck", HIDDEN_TO_ALL);
        createCards(hbgs);
        hbgs.discardDeck = new Deck<>("DiscardDeck", VISIBLE_TO_ALL);
        hbgs.hintCounter = new Counter(hbp.hintCounter,0,hbp.hintCounter, "Hint Counter");
        hbgs.failCounter = new Counter(hbp.failCounter,0,hbp.failCounter, "Fail Counter");
        hbgs.currentCard = new ArrayList<>();

        drawCardsToPlayers(hbgs);
    }

    private void createCards(HanabiGameState hbgs) {
        HanabiParameters hbp = (HanabiParameters) hbgs.getGameParameters();
        for (String color : hbp.color) {
                // Create the number cards
                for (int number = 1; number <= hbp.nNumberCards; number++) {
                    if (number == 1){
                        for(int i = 0; i<hbp.nCards1; i++){
                            hbgs.drawDeck.add(new HanabiCard(HanabiCard.HanabiCardType.Number, color, number));
                        }
                    }
                    else if(number == 2) {
                        for (int i = 0; i<hbp.nCards2; i++) {
                            hbgs.drawDeck.add(new HanabiCard(HanabiCard.HanabiCardType.Number, color, number));
                        }
                    }
                    else if(number == 3) {
                        for (int i = 0; i<hbp.nCards3; i++) {
                            hbgs.drawDeck.add(new HanabiCard(HanabiCard.HanabiCardType.Number, color, number));
                        }
                    }
                    else if(number == 4) {
                        for (int i = 0; i<hbp.nCards4; i++) {
                            hbgs.drawDeck.add(new HanabiCard(HanabiCard.HanabiCardType.Number, color, number));
                        }
                    }
                    else if(number == 5){
                        for (int i = 0; i<hbp.nCards5; i++) {
                            hbgs.drawDeck.add(new HanabiCard(HanabiCard.HanabiCardType.Number, color, number));
                        }
                    }
                }
        }

    }

    private void drawCardsToPlayers(HanabiGameState hgs) {
        hgs.drawDeck.shuffle(new Random(hgs.getGameParameters().getRandomSeed()));
        for (int player = 0; player < hgs.getNPlayers(); player++) {
            for (int card = 0; card < ((HanabiParameters) hgs.getGameParameters()).nHandCards; card++) {
                hgs.playerDecks.get(player).add(hgs.drawDeck.draw());
            }
        }
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
        if (checkGameEnd((HanabiGameState) currentState)) {
            return;
        }
        if (currentState.getGameStatus() == GAME_ONGOING) {
            endPlayerTurn(currentState);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        HanabiGameState hbgs = (HanabiGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = hbgs.getCurrentPlayer();
        Deck<HanabiCard> playerHand = hbgs.playerDecks.get(player);
        List<PartialObservableDeck<HanabiCard>> playerDecks = hbgs.playerDecks;

        for (HanabiCard card : playerHand.getComponents()) {
            int cardIdx = playerHand.getComponents().indexOf(card);
            if (hbgs.hintCounter.getValue() != hbgs.hintCounter.getMaximum()) {
                actions.add(new Discard(playerHand.getComponentID(), hbgs.discardDeck.getComponentID(), cardIdx));
            }

            actions.add(new Play(playerHand.getComponentID(), cardIdx, card.color, card.number));

        }

        if (hbgs.hintCounter.getValue() != hbgs.hintCounter.getMinimum()){
            for (PartialObservableDeck<HanabiCard> d : playerDecks){
                if (d.getComponentID() != playerHand.getComponentID()) {
                    actions.add(new Hint(d,1));
                    actions.add(new Hint(d,2));
                    actions.add(new Hint(d,3));
                    actions.add(new Hint(d,4));
                    actions.add(new Hint(d,5));
                    actions.add(new Hint(d,"Red"));
                    actions.add(new Hint(d,"Yellow"));
                    actions.add(new Hint(d,"Green"));
                    actions.add(new Hint(d,"White"));
                    actions.add(new Hint(d,"Blue"));

                }
            }

        }
        return actions;
    }

    @Override
    protected void endGame(AbstractGameState gs) {
        HanabiGameState hbgs = (HanabiGameState) gs;
        int total = 0;
        for(HanabiCard cd: hbgs.currentCard){
            total += cd.number;
        }
        if (hbgs.failCounter.getValue() == 0){
            System.out.println("fail counter has reached 0");
            System.out.println("Point was " + total);
        }
        if (total == 25){
            System.out.println("reached maximum point");
            System.out.println("Point was " + total);
        }
        if(hbgs.endTurn == 0){
            System.out.println("run out of card and every player had one turn");
            System.out.println("Point was " + total);
        }
    }

    private boolean checkGameEnd(HanabiGameState hbgs){
        int total = 0;
        for(HanabiCard cd: hbgs.currentCard){
            total += cd.number;
        }
        if (hbgs.failCounter.getValue() == 0){
            endPlayerTurn(hbgs);
            hbgs.setGameStatus(CoreConstants.GameResult.GAME_END);
//            System.out.println("fail counter has reached 0");
//            System.out.println("Point was " + total);
            for (int i = 0; i < hbgs.getNPlayers(); i++) {
                hbgs.setPlayerResult(LOSE_GAME, i);
            }
            return true;
        }
        if (total == 25){
            endPlayerTurn(hbgs);
            hbgs.setGameStatus(CoreConstants.GameResult.GAME_END);
//            System.out.println("reached maximum point");
//            System.out.println("Point was " + total);
            for (int i = 0; i < hbgs.getNPlayers(); i++) {
                hbgs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, i);
            }
            return true;
        }
        if(hbgs.drawDeck.getComponents().size() == 0){
            hbgs.endTurn -= 1;
        }
        if(hbgs.endTurn == 0){
            endPlayerTurn(hbgs);
            hbgs.setGameStatus(CoreConstants.GameResult.GAME_END);
//            System.out.println("run out of card and every player had one turn");
//            System.out.println("Point was " + total);
            for (int i = 0; i < hbgs.getNPlayers(); i++) {
                hbgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
            }
            return true;
        }
        return false;
    }
}

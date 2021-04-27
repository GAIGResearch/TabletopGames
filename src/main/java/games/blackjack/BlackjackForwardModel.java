package games.blackjack;

//import com.sun.javafx.scene.text.TextLayout;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
//import games.blackjack.actions.noCard;
import utilities.Utils;

import javax.rmi.CORBA.Util;
import java.io.Console;
import java.util.ArrayList;
import java.util.*;

import static core.CoreConstants.VERBOSE;

public class BlackjackForwardModel extends AbstractForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        BlackjackGameState bjgs = (BlackjackGameState) firstState;

        bjgs.Score = new int[firstState.getNPlayers()];

        //Create a deck
        bjgs.playerDecks = new ArrayList<>();

        //Create a hand for each player
        for (int i = 0; i< bjgs.getNPlayers(); i++){
            bjgs.playerDecks.add(new Deck<>("Player " + i + " deck", i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
        }

        //create the playing deck
        //bjgs.drawDeck = new Deck<>("DrawDeck");
        bjgs.drawDeck = new Deck<>("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        createCards(bjgs);
        //shuffle the cards
        bjgs.drawDeck.shuffle(new Random((bjgs.getGameParameters().getRandomSeed())));

        //create the table deck to discard the decks onto
        bjgs.tableDeck = new Deck<>("TableDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

        //Draw cards to the players
        drawPlayerCards(bjgs);

        bjgs.getTurnOrder().setStartingPlayer(0);


        bjgs.getCurrentPlayer();
        //checkPoints(bjgs);

    }

    private void createCards(BlackjackGameState bjgs) {
        BlackjackParameters bjgp = (BlackjackParameters) bjgs.getGameParameters();
        for (String suite: bjgp.suite){
            for (int number = 2; number <= bjgp.totalNumberCards; number++){
                bjgs.drawDeck.add(new FrenchCard(FrenchCard.FrenchCardType.Number,suite, number));
                }

            bjgs.DrawDeck().add(new FrenchCard(FrenchCard.FrenchCardType.Ace, suite));
            bjgs.DrawDeck().add(new FrenchCard(FrenchCard.FrenchCardType.Jack, suite));
            bjgs.DrawDeck().add(new FrenchCard(FrenchCard.FrenchCardType.Queen, suite));
            bjgs.DrawDeck().add(new FrenchCard(FrenchCard.FrenchCardType.King, suite));
            }
        }

    private void drawPlayerCards(BlackjackGameState bjgs) {
        for (int player = 0; player < bjgs.getNPlayers(); player++) {
            for (int card = 0; card < ((BlackjackParameters)bjgs.getGameParameters()).nCardsPerPlayer; card++){
                bjgs.playerDecks.get(player).add(bjgs.drawDeck.draw());
            }
        }
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action){
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        action.execute(gameState);
        if (checkGameEnd((BlackjackGameState) gameState)){
            //endGame(gameState);
            return;
        }
        if (action.getString(gameState) == "Stand"){
            gameState.getTurnOrder().endPlayerTurn(gameState);
        }
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING){
            //gameState.getTurnOrder().endPlayerTurn(gameState);
            return;
        }

    }

    private boolean checkGameEnd(BlackjackGameState bjgs){
        BlackjackParameters bjgp = (BlackjackParameters) bjgs.getGameParameters();
        boolean aWinner = false;
        boolean push = false;
        boolean bust = false;
        int bjWin = bjgp.blackJack;

        for (int j = 0; j < bjgs.getNPlayers(); j++){
            bjgs.Score[j] += bjgs.calcPoint(j);
        }

        int dealerPlayer = -1;
        for (int dealer = 0; dealer < bjgs.getNPlayers(); dealer++){
            dealerPlayer++;
        }

        HashSet<Integer> winners = new HashSet<>();

        if (bjgs.Score[0] == 21){
            winners.add(0);
            aWinner = true;
        }
        else if (bjgs.Score[0] > 21){
            winners.add(dealerPlayer);
            aWinner = true;
        }
        else if (bjgs.Score[dealerPlayer] > 21 && bjgs.Score[0] > 21){
            aWinner = true;
            push = true;
        }
        else if (bjgs.Score[dealerPlayer] >= 17 && bjgs.Score[dealerPlayer] > bjgs.Score[0]){
            winners.add(dealerPlayer);
            aWinner = true;
        }
        else if (bjgs.Score[dealerPlayer] >= 17 && bjgs.Score[dealerPlayer] < bjgs.Score[0]){
            winners.add(0);
            aWinner = true;
        }

        if (aWinner){
            if (push){
                bjgs.setPlayerResult(Utils.GameResult.DRAW, 0);
            }
            else if (bust){
                bjgs.setPlayerResult(Utils.GameResult.WIN, 0);
            }

            else if (winners.contains(0)){
                bjgs.setPlayerResult(Utils.GameResult.WIN, 0);
            }
            else if (winners.contains(dealerPlayer)){
                bjgs.setPlayerResult(Utils.GameResult.WIN, dealerPlayer);
                bjgs.setPlayerResult(Utils.GameResult.LOSE, 0);
            }
            else{
                bjgs.setPlayerResult(Utils.GameResult.LOSE, 0);
            }

            bjgs.setGameStatus(Utils.GameResult.GAME_END);
            return true;
        }
        bjgs.setGameStatus(Utils.GameResult.GAME_ONGOING);
        return false;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = bjgs.getCurrentPlayer();
        Deck<FrenchCard> currentHand =  bjgs.playerDecks.get(player);


        int dealerPlayer = -1;
        for (int dealer = 0; dealer < bjgs.getNPlayers(); dealer++){
            dealerPlayer++;
        }


        if (bjgs.getCurrentPlayer() == dealerPlayer){
/*            if(bjgs.calcPoint(dealerPlayer) >)*/

            if(bjgs.getGameScore(bjgs.getCurrentPlayer()) == 21){
                System.out.println("Dealer Blackjack!");
                //endGame(gameState);
            }
            else if (bjgs.getGameScore(bjgs.getCurrentPlayer()) >= 17){
                //System.out.println("Dealer stands on 17");
                actions.add(new Hit(currentHand.getComponentID()));
                actions.add(new Stand());
                return actions;
                //endGame(gameState);
                /*actions.add(new Stand());
                return actions;*/
            }
            else{
                //System.out.println("Dealer Hits");
                actions.add(new Hit(currentHand.getComponentID()));
                actions.add(new Stand());
                return actions;
            }
        }
        else{
            if(bjgs.getGameScore(bjgs.getCurrentPlayer()) < 21){
                actions.add(new Hit(currentHand.getComponentID()));
                actions.add(new Stand());
                return actions;
            }
            else if(bjgs.getGameScore(bjgs.getCurrentPlayer()) == 21){
                System.out.println("Blackjack!");
            }
/*        else if (bjgs.point == 21){
            System.out.println("BlackJack!");
            bjgs.setPlayerResult(Utils.GameResult.WIN, bjgs.getCurrentPlayer());
            bjgs.setGameStatus(Utils.GameResult.GAME_END);
        }
        else{
            System.out.println("Push");
            bjgs.setPlayerResult(Utils.GameResult.LOSE, bjgs.getCurrentPlayer());
            bjgs.setGameStatus(Utils.GameResult.GAME_END);
        }*/
        }
        return  actions;
    }

    @Override
    protected void endGame(AbstractGameState gameState){
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        //if (VERBOSE){
        System.out.println("Game Results:");
        for (int playerID = 0; playerID < gameState.getNPlayers(); playerID++){
            if (gameState.getPlayerResults()[playerID] == Utils.GameResult.WIN){
                System.out.println("The winner is player : " + playerID);

                String[] strings = new String[2];

                StringBuilder sb = new StringBuilder();
                sb.append("Player Hand: ");
                for (FrenchCard card : bjgs.playerDecks.get(0).getComponents()){
                    sb.append(card.toString());
                    sb.append(" ");
                }

                StringBuilder sb1 = new StringBuilder();
                sb1.append("Dealer Hand: ");
                for (FrenchCard card : bjgs.playerDecks.get(1).getComponents()){
                    sb1.append(card.toString());
                    sb1.append(" ");
                }
                strings[0] = sb.toString();
                strings[1] = sb1.toString();
                for (String s : strings){
                    System.out.println(s);
                }
                break;
                }

            }
        }
   // }

    @Override
    protected AbstractForwardModel _copy() {
        return new BlackjackForwardModel();
    }

}
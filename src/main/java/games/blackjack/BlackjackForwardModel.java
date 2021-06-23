package games.blackjack;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import java.util.ArrayList;
import java.util.*;


public class BlackjackForwardModel extends AbstractForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        BlackjackGameState bjgs = (BlackjackGameState) firstState;
        bjgs.dealerPlayer = bjgs.getNPlayers() - 1;  // Dealer player is last

        //Create a deck
        bjgs.playerDecks = new ArrayList<>();

        //create the playing deck
        bjgs.drawDeck = FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        //shuffle the cards
        bjgs.drawDeck.shuffle(new Random((bjgs.getGameParameters().getRandomSeed())));

        bjgs.getTurnOrder().setStartingPlayer(0);

        //Create a hand for each player
        boolean[] visibility = new boolean[firstState.getNPlayers()];
        Arrays.fill(visibility, true);
        for (int i = 0; i < bjgs.getNPlayers(); i++){
            PartialObservableDeck<FrenchCard> playerDeck = new PartialObservableDeck<>("Player " + i + " deck", i, visibility);
            bjgs.playerDecks.add(playerDeck);
            for (int card = 0; card < ((BlackjackParameters)bjgs.getGameParameters()).nCardsPerPlayer; card++) {
                if (i == bjgs.dealerPlayer && i < ((BlackjackParameters)bjgs.getGameParameters()).nDealerCardsHidden) {
                    new Hit(i, false, true).execute(bjgs);
                } else {
                    new Hit(i).execute(bjgs);
                }
            }
        }
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action){
        action.execute(gameState);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = bjgs.getCurrentPlayer();

        //Check if current player is the dealer
        //dealer must hit if score is <=16 otherwise must stand
        if (bjgs.getCurrentPlayer() == bjgs.dealerPlayer){
            if (bjgs.calculatePoints(bjgs.dealerPlayer) >= ((BlackjackParameters) bjgs.getGameParameters()).dealerStand){
//                System.out.println("Stand");
                actions.add(new Stand());
            }
            else {
//                System.out.println("Hit");
                actions.add(new Hit(player, true, false));
            }
        }
        else {
            actions.add(new Hit(player, true, false));
            actions.add(new Stand());
        }
        return actions;
    }

//    @Override
//    protected void endGame(AbstractGameState gameState){
//        BlackjackGameState bjgs = (BlackjackGameState) gameState;
//        System.out.println("Game Results:");
//        for (int playerID = 0; playerID < gameState.getNPlayers(); playerID++){
//
//            StringBuilder sb = new StringBuilder();
//            sb.append(playerID == bjgs.dealerPlayer ? "Dealer" : "Player").append(" Hand: ");
//            for (FrenchCard card : bjgs.playerDecks.get(playerID).getComponents()){
//                sb.append(card.toString());
//                sb.append(" ");
//            }
//            System.out.println(sb);
//
//            if (gameState.getPlayerResults()[playerID] == Utils.GameResult.WIN){
//                System.out.println("The winner is player : " + playerID);
//            } else if(gameState.getPlayerResults()[playerID] == Utils.GameResult.DRAW){
//                System.out.println("Push");
//            }
//        }
//    }

    @Override
    protected AbstractForwardModel _copy() {
        return new BlackjackForwardModel();
    }

}
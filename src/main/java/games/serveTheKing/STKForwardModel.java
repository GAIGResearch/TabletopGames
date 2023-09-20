package games.serveTheKing;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.serveTheKing.actions.*;
import games.serveTheKing.components.PlateCard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class STKForwardModel extends StandardForwardModel {

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        STKGameState stkgs = (STKGameState)firstState;

        stkgs.mainDeck = new Deck<PlateCard>("mainDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        stkgs.discardPile = new Deck<PlateCard>("discardPile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        stkgs.playersHands= new ArrayList<>(stkgs.getNPlayers());
        stkgs.playersPlates = new ArrayList<>(stkgs.getNPlayers());
        stkgs.playerCalledServe=-1;
        //TODO : implement the different kings.
        setupRound(stkgs);
    }

    private void setupRound(STKGameState stkgs){
        // make sure game is not infinite
        stkgs.getCoreGameParameters().setMaxRounds(20*stkgs.getNPlayers());
        // Reset player status
        for (int i = 0; i < stkgs.getNPlayers(); i++) {
            stkgs.setPlayerResult(CoreConstants.GameResult.GAME_ONGOING, i);
        }
        // Create Draw Pile
        stkgs.mainDeck.clear();
        for (int i =0 ; i<11;i++) {
            for (int j = 0; j < 4; j++) {
                PlateCard card = new PlateCard(i);
                stkgs.mainDeck.add(card);
            }
        }
        for (int j = 0; j < 3; j++) {
            PlateCard card = new PlateCard(-1);
            stkgs.mainDeck.add(card);
            PlateCard card2 = new PlateCard(15);
            stkgs.mainDeck.add(card2);
        }
        Random rnd = new Random(stkgs.getGameParameters().getRandomSeed());
        stkgs.mainDeck.shuffle(rnd);

        //Create Player Plate area
        for (int i = 0; i < stkgs.getNPlayers(); i++) {
            boolean[] plateVisible = new boolean[stkgs.getNPlayers()];
            // add random cards to the player's hand
            PartialObservableDeck<PlateCard> playerCards = new PartialObservableDeck<>("playerPlate" + i, i, plateVisible);
            for (int j = 0; j < 4; j++) {
                playerCards.add(stkgs.mainDeck.draw());
            }
            stkgs.playersPlates.add(playerCards);
            // a player look at the two first cards
            stkgs.playersPlates.get(i).setVisibilityOfComponent(0,i,true);
            stkgs.playersPlates.get(i).setVisibilityOfComponent(1,i,true);
        }

        //Create players hands (empty)
        for (int i = 0; i < stkgs.getNPlayers(); i++) {
            boolean[] handVisible = new boolean[stkgs.getNPlayers()];
            PartialObservableDeck<PlateCard> playerHand = new PartialObservableDeck<>("playerHand" + i, i, handVisible);
            stkgs.playersHands.add(playerHand);
        }
        // Create discard pile
        stkgs.discardPile.clear();

        // Reset the player that called the Serve
        stkgs.playerCalledServe =-1;

        //start at draw phase for first player
        stkgs.setGamePhase(STKGameState.STKGamePhase.Draw);
    };


    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        if (gameState.isActionInProgress()) return;
        STKGameState stkgs = (STKGameState) gameState;

        int playerID = stkgs.getCurrentPlayer();
        if(!checkEndOfRound(stkgs,action)) {
            switch (stkgs.getGamePhase().toString()) {
                case "Draw":
                    if (action instanceof ChooseToDraw) {
                        stkgs.setGamePhase(STKGameState.STKGamePhase.Play);
                        // player get a card
                        Deck<PlateCard> mainDeck = stkgs.mainDeck;
                        Deck<PlateCard> discard = stkgs.discardPile;
                        if(mainDeck.getSize()<1){
                            int discardSize = discard.getSize();
                            for(int i =0; i<discardSize;i++){
                                PlateCard c = discard.draw();
                                mainDeck.add(c);
                            }
                        }
                        PlateCard cardDraw = stkgs.mainDeck.draw();
                        System.out.println("[STKForwardModdel] made player "+playerID+" draw this card: "+cardDraw);
                        stkgs.playersHands.get(playerID).add(cardDraw);
                        //System.out.println("Player "+playerID+" hand size is: "+stkgs.playersHands.get(playerID).getComponents().size());
                    } else {
                        // set current player as caller and end the turn
                        stkgs.playerCalledServe = playerID;
                        int nextPlayer = (gameState.getCurrentPlayer() + 1) % stkgs.getNPlayers();
                        System.out.println("[STKForwardModdel] Served the king, passing to player "+nextPlayer);
                        endPlayerTurn(stkgs, nextPlayer);
                    }
                    break;
                case "Play":
                    int nextPlayer = (gameState.getCurrentPlayer() + 1 )% stkgs.getNPlayers();
                    stkgs.setGamePhase(STKGameState.STKGamePhase.Draw);
                    System.out.println("[STKForwardModdel] Played my turn, passing to player "+nextPlayer);
                    endPlayerTurn(stkgs, nextPlayer);
                    break;
                default:
                    throw new AssertionError("Unknown Game Phase " + stkgs.getGamePhase());

            }

        }
    }


    private boolean checkEndOfRound(STKGameState state, AbstractAction action){
        boolean result = false;
        System.out.println("[STKForwardModdel] Current turn is "+state.getTurnCounter());
        if(state.getCurrentPlayer()+1 == state.playerCalledServe || state.getTurnCounter()>state.getCoreGameParameters().getMaxRounds()){
            int minimumScore= 60;
            int playerScores[] = new int[state.getNPlayers()];
            for (int i =0 ; i<state.getNPlayers(); i++){
                int score =0;
                PartialObservableDeck<PlateCard> playerPlate = state.getPlayersPlates().get(i);
                for(PlateCard c: playerPlate.getComponents()){
                    score = score + c.getValue();
                }
                playerScores[i]=score;
                if (score<minimumScore){
                    minimumScore=score;
                }
            }
            HashSet<Integer> winners = new HashSet<Integer>();
            for (int i =0 ; i<state.getNPlayers(); i++) {
                if(playerScores[i]==minimumScore){
                    winners.add(i);
                }
            }
            int trueWin=-1;
            for (int i = (state.playerCalledServe+1)%state.getNPlayers();i<state.getNPlayers();i++){
                if(winners.contains(i)){
                    state.setPlayerResult(CoreConstants.GameResult.WIN_ROUND,i);
                    trueWin=i;
                }
                else {
                    state.setPlayerResult(CoreConstants.GameResult.LOSE_ROUND,i);
                }
            }
            endRound(state,trueWin);
            state.setGameStatus(CoreConstants.GameResult.GAME_END);
            result = true;
        }
        return result;
    }
    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        STKGameState stkgs = (STKGameState) gameState;
        int currentPlayer = stkgs.getCurrentPlayer();
        switch (stkgs.getGamePhase().toString()) {
            case "Draw":
                actions.add(new ChooseToDraw());
                if(stkgs.playerCalledServe<0){
                    actions.add(new Serve());
                }
                break;
            case "Play":
                PartialObservableDeck<PlateCard> playerPlate = stkgs.getPlayersPlates().get(currentPlayer);
                for(int i =0; i< playerPlate.getComponents().size();i++){
                    Exchange action = new Exchange(currentPlayer,0,i);
                    actions.add(action);
                }
                actions.add(new UseAbility(currentPlayer));
                break;
            default:
                throw new AssertionError("Unknown Game Phase " + stkgs.getGamePhase());
        }
        return actions;
    }
}

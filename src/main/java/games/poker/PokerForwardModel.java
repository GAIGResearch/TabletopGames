package games.poker;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
//import games.poker.actions.NoCards;
import games.poker.actions.*;
import games.poker.actions.Fold;
import games.poker.actions.Raise;
import games.poker.actions.NoCards;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.VERBOSE;

public class PokerForwardModel extends AbstractForwardModel {

    boolean[] playerCheck;
    int[] playerPoints;
    boolean checkBets = false;

    @Override
    protected void _setup(AbstractGameState firstState) {
        PokerGameState pgs = (PokerGameState) firstState;

        // Set up scores for all players, initially 0
        playerPoints = new int[pgs.getNPlayers()];
        pgs.playerHand = new int[firstState.getNPlayers()];
        pgs.communityCards = new FrenchCard[5];
        pgs.currentMoney = new int[firstState.getNPlayers()];
        pgs.playerCheck = new boolean[firstState.getNPlayers()];
        pgs.totalPotMoney = 0;
        pgs.turnNumber = 0;
        pgs.previousBet = 0;
        pgs.smallBlind = 0;
        pgs.bigBlind = 1;
        pgs.equalBets = false;
        playerCheck = new boolean[firstState.getNPlayers()];
        //pgs.blindsFinished = false;

        for (int i = 0; i < firstState.getNPlayers(); i++) {
            pgs.currentMoney[i] = 0;
        }

        for (int i = 0; i < firstState.getNPlayers(); i++) {
            playerCheck[i] = false;
        }

        pgs.playerDecks = new ArrayList<>();
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            pgs.playerDecks.add(new Deck<>("Player " + i + " deck", i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
        }





        // Create the draw deck with all the cards
        pgs.drawDeck = new Deck<>("DrawDeck", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        createCards(pgs);

        // Create the discard deck, at the beginning it is empty
        pgs.discardDeck = new Deck<>("DiscardDeck", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        //pgs.communityCards = new Deck<>("CommunityCards");

        // Player 0 starts the game
        pgs.getTurnOrder().setStartingPlayer(0);

        // Set up first round
        setupRound(pgs);
    }

    /**
     * Create all the cards and include them into the drawPile.
     * @param pgs - current game state.
     */
    private void createCards(PokerGameState pgs) {
        PokerGameParameters pgp = (PokerGameParameters)pgs.getGameParameters();
        for (String suite : pgp.suite) {

            // Create the number cards for each suite
            for (int number = 2; number <= pgp.nNumberCards; number++) {
                pgs.drawDeck.add(new FrenchCard(FrenchCard.FrenchCardType.Number, suite, number));
            }
            // Create the Ace, Queen, King and Jack cards for each suite
            for (int i = 0; i < pgp.AceCards; i++) {
                pgs.drawDeck.add(new FrenchCard(FrenchCard.FrenchCardType.Ace, suite));
            }
            for (int i = 0; i < pgp.QueenCards; i++) {
                pgs.drawDeck.add(new FrenchCard(FrenchCard.FrenchCardType.Queen, suite));
            }
            for (int i = 0; i < pgp.KingCards; i++) {
                pgs.drawDeck.add(new FrenchCard(FrenchCard.FrenchCardType.King, suite));
            }
            for (int i = 0; i < pgp.JackCards; i++) {
                pgs.drawDeck.add(new FrenchCard(FrenchCard.FrenchCardType.Jack, suite));
            }
        }


    }

    private void drawCardsToPlayers(PokerGameState pgs) {
        for (int player = 0; player < pgs.getNPlayers(); player++) {
            for (int card = 0; card < ((PokerGameParameters)pgs.getGameParameters()).nCardsPerPlayer; card++) {
                pgs.playerDecks.get(player).add(pgs.drawDeck.draw());
            }
        }
    }

    public boolean getCheck(int player) {
        return playerCheck[player];
    }

    public boolean isCheckEqual(int getNPlayers) {
        for (int i = 0; i < getNPlayers; i++) {
            if (getCheck(0) != getCheck(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets up a round for the game, including draw pile, discard deck and player decks, all reset.
     * @param pgs - current game state.
     */
    private void setupRound(PokerGameState pgs) {
        Random r = new Random(pgs.getGameParameters().getRandomSeed() + pgs.getTurnOrder().getRoundCounter());

        // Refresh player decks
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            pgs.drawDeck.add(pgs.playerDecks.get(i));
            pgs.playerDecks.get(i).clear();
        }

        for (int i = 0; i < pgs.getNPlayers(); i++) {
            pgs.playerCheck[i] = false;
        }

        // Refresh draw deck and shuffle
        pgs.drawDeck.add(pgs.discardDeck);
        pgs.discardDeck.clear();
        pgs.drawDeck.shuffle(r);

        for (int i = 0; i < 5; i++) {
            pgs.communityCards[i] = pgs.drawDeck.draw();
        }

        // Draw new cards for players
        drawCardsToPlayers(pgs);

        pgs.currentCard = pgs.drawDeck.draw();
        pgs.currentSuite = pgs.currentCard.suite;
        if (VERBOSE) {
            System.out.println("First card " + pgs.currentSuite);
        }

        // add current card to discard deck
        pgs.discardDeck.add(pgs.currentCard);
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action) {
        action.execute(gameState);
        if (checkRoundEnd((PokerGameState)gameState)) {
            return;
        }

        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            gameState.getTurnOrder().endPlayerTurn(gameState);
        }
    }

    /**
     * Checks if the round ended (when one player the max number of cards). On round end, points for all players are added up
     * and next round is set up.
     * @param pgs - current game state
     * @return true if round ended, false otherwise
     */
    private boolean checkRoundEnd(PokerGameState pgs) {
        // Did any player run out of cards?
        boolean roundEnd = false;
        boolean maxRounds = false;
        /*System.out.println(pgs.getTurnOrder().getRoundCounter());
        System.out.println(pgs.turnNumber);*/
        int[] playerDeckSize = new int[pgs.getNPlayers()];

        if (pgs.getNPlayers() == 2) {
            for (int playerID = 0; playerID < pgs.getNPlayers(); playerID++) {
                if (pgs.getPlayerResults()[playerID] == Utils.GameResult.LOSE && pgs.getNPlayers() == 2) {
                    if (playerID == 0) {
                        pgs.setPlayerResult(Utils.GameResult.WIN, 1);
                    } else {
                        pgs.setPlayerResult(Utils.GameResult.WIN, 0);
                    }
                    pgs.setGameStatus(Utils.GameResult.GAME_END);
                    return true;
                }
        }

        for (int i = 0; i < pgs.getNPlayers(); i++) {
            pgs.updateTotalPot(pgs.getPlayerMoney(i));
        }

        if (pgs.getTurnOrder().getRoundCounter() == 4 && pgs.getCurrentPlayer() == (pgs.playerDecks.size() - 1)) {
            maxRounds = true;
        }

        for (int playerID = 0; playerID < pgs.getNPlayers(); playerID++) {
            if (pgs.getPlayerResults()[playerID] == Utils.GameResult.LOSE && pgs.getNPlayers() == 2) {
                if (playerID == 0) {
                    pgs.setPlayerResult(Utils.GameResult.WIN, 1);
                }
                else {
                    pgs.setPlayerResult(Utils.GameResult.WIN, 0);
                }
                roundEnd = true;
                break;
            }
        }

        if (maxRounds) {
            for (int playerID = 0; playerID < pgs.getNPlayers(); playerID++) {
                if (pgs.getPlayerResults()[playerID] == Utils.GameResult.GAME_ONGOING) {
                    roundEnd = true;
                    break;
                }
            }
        }

        if (roundEnd) {
            pgs.getTurnOrder().endRound(pgs);

            // Did this player just hit N points to win? Win condition check!
            if (checkGameEnd(pgs, pgs.playerHand)) return true;


            // Reset cards for the new round
            setupRound(pgs);

            return false;
        }

        }

        return false;
    }

    private boolean checkGameEnd(PokerGameState pgs, int[] playerScores) {
        PokerGameParameters pgp = (PokerGameParameters) pgs.getGameParameters();

        playerPoints = pgs.calculatePlayerHand();
        int max = 0;
        int idMaxScore = -1;

        for (int playerID = 0; playerID < pgs.getNPlayers(); playerID++) {
            if (playerPoints[playerID] > max){
                max = playerPoints[playerID];
                idMaxScore = playerID;
            }
        }
        // A winner!
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (i == idMaxScore){
                pgs.setPlayerResult(Utils.GameResult.WIN, i);
            }
            else {
                pgs.setPlayerResult(Utils.GameResult.LOSE, i);
            }
        }

        pgs.setGameStatus(Utils.GameResult.GAME_END);
        //System.out.println(Arrays.toString(playerPoints));
        return true;
    }


    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState)gameState;

        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = pgs.getCurrentPlayer();

        Deck<FrenchCard> playerHand = pgs.playerDecks.get(player);
        if (pgs.checkBets) { //check if there have been ANY checks done
            if (pgs.playerCheck[pgs.getCurrentPlayer()]) { //goes through an array of the players, stopping at each player that has done a check
                actions.add(new Call(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new Raise(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new RaiseBy4(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new RaiseBy8(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new Fold(playerHand.getComponentID()));
                pgs.playerCheck[pgs.getCurrentPlayer()] = false; //sets that players check value to false as they have placed a bet
            }
        }
        else {
            if (pgs.getTurnOrder().getRoundCounter() == 0 && pgs.getCurrentPlayer() == 0){ //small blind
                actions.add(new Blind(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new Fold(playerHand.getComponentID()));
            }

            else if (pgs.getTurnOrder().getRoundCounter() == 0 && pgs.getCurrentPlayer() == 1){ //big blind
                actions.add(new Blind(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new Fold(playerHand.getComponentID()));
                pgs.updateBlindsFinished();
            }
            else { //if it a normal round and they have not checked (done when betting the first time)
                actions.add(new Call(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new Check(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new Raise(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new RaiseBy4(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new RaiseBy8(playerHand.getComponentID(), pgs.discardDeck.getComponentID(), pgs.playerDecks.get(player).getComponents().size(), pgs.currentMoney[pgs.getCurrentPlayer()]));
                actions.add(new Fold(playerHand.getComponentID()));
            }
        }

        if (!pgs.isEqualBets() && player == (pgs.getNPlayers() - 1)) { //checks if bets are equal, if not then it redoes the round so bets can be equal
            pgs.turnNumber = pgs.turnNumber - 1;
        }

        if (!pgs.isCheckEqual() && player == (pgs.getNPlayers() - 1)) { //checks if any player has checked, and if they have then it sets a flag which redoes the round
            pgs.checkBets = true;
            pgs.playerCheck[pgs.getCurrentPlayer()] = checkBets;
            pgs.turnNumber = pgs.turnNumber - 1;
        }

        else if (pgs.isCheckEqual() && player == (pgs.getNPlayers() - 1)) { //if no checks happened then resets all the flags
            pgs.checkBets = false;
            pgs.setCheckToFalse(pgs.getNPlayers());
        }

        if (actions.isEmpty()) { //done in case error in the actions
            actions.add(new NoCards());
        }

        if (player <= 0) { //increments the player counter which is needed to keep track of bets and checks
            pgs.turnNumber += 1;
        }

        return actions;
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState)gameState;
        System.out.println(Arrays.toString(playerPoints));

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 2; i++) {
            sb.append("Player " + i + " Hand: ");
            for (FrenchCard card : pgs.playerDecks.get(i).getComponents()) {
                sb.append(card.toString());
                sb.append(" ");
            }
            sb.append("\n");
            sb.append("Total Pot: " + pgs.getTotalPot());
            //sb.append("Player " + i + " Money: " + pgs.getPlayerMoney(i));
            sb.append("\n");
        }

        System.out.println(sb.toString());


        if (VERBOSE) {
            System.out.println("Game Results:");
            for (int playerID = 0; playerID < gameState.getNPlayers(); playerID++) {
                if (gameState.getPlayerResults()[playerID] == Utils.GameResult.WIN) {
                    System.out.println("The winner is the player : " + (playerID + 1));
                    //break;
                }
            }
        }
        //System.out.println("Game Results:");
        //System.out.println(gameState.getPlayerResults()[0]);
        //System.out.println(gameState.getPlayerResults()[1]);
        for (int playerID = 0; playerID < gameState.getNPlayers(); playerID++) {
            if (gameState.getPlayerResults()[playerID] == Utils.GameResult.WIN) {
                System.out.println("The winner is the player : " + (playerID +1));
                //break;
            }
        }


    }

    @Override
    protected AbstractForwardModel _copy() {
        return new PokerForwardModel();
    }
}

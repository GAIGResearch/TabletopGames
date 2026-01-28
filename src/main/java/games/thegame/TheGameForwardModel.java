package games.thegame;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.thegame.actions.PlayingCards;
import games.thegame.actions.SelectRows;
import games.thegame.components.TheGameCard;
import games.thegame.components.TheGameDeck;
import gametemplate.actions.GTAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TheGameForwardModel extends StandardForwardModel {

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
        TheGameGS gs = (TheGameGS) firstState;
        TheGameParameters params = (TheGameParameters) firstState.getGameParameters();

        // Create the rows and add the starting cards.
        gs.cardRows = new ArrayList<>();
        int cardRowIdx = 0;
        for(int i = 0; i < params.numAscendingRows; ++i) {
            gs.cardRows.add(new TheGameDeck<>("Row " + cardRowIdx, CoreConstants.VisibilityMode.VISIBLE_TO_ALL, true));
            gs.cardRows.get(cardRowIdx).add(new TheGameCard("" + params.minCardNumber, params.minCardNumber));
            cardRowIdx++;
        }

        for(int i = 0; i < params.numDescendingRows; ++i) {
            gs.cardRows.add(new TheGameDeck<>("Row " + cardRowIdx, CoreConstants.VisibilityMode.VISIBLE_TO_ALL, false));
            gs.cardRows.get(cardRowIdx).add(new TheGameCard("" + params.maxCardNumber, params.maxCardNumber));
            cardRowIdx++;
        }

        // Create fill and shuffle the row deck
        gs.drawDeck = new Deck<>("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        for(int i = params.minCardNumber+1; i < params.maxCardNumber; ++i)
            gs.drawDeck.add(new TheGameCard("" + i , i));
        gs.drawDeck.shuffle(gs.getRnd());

        // Create the player hands and deal cards to them. Also no selected rows at start.
        gs.playerHands = new ArrayList<>();
        gs.selectedRows = new HashMap<>();

        for(int i = 0; i < gs.getNPlayers(); ++i) {
            gs.playerHands.add(new Deck<>("Player " + i + " hand", i, params.playerHandVisibility));
            //Deal cards depending on player count.
            for(int k = 0; k < params.handSize[gs.getNPlayers()]; ++k)
                    gs.playerHands.get(i).add(gs.drawDeck.draw());
            gs.selectedRows.put(i, -1);
        }

//        gs.gamePhase = TheGameGS.TheGamePhase.SelectingRow;

    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        TheGameGS gs = (TheGameGS) gameState;
        actions.add(new PlayingCards(gs.getCurrentPlayer()));

//        switch (gs.gamePhase)
//        {
////            case SelectingRow -> {
////                actions.add(new SelectRows(gs.getCurrentPlayer()));
////            }
//
//            case PlayingCards -> {
//
//            }
//        }

        // TODO: create action classes for the current player in the given game state and add them to the list. Below just an example that does nothing, remove.
        actions.add(new GTAction());
        return actions;
    }


    /**
     * This is a method hook for any game-specific functionality that should run after an Action is executed
     * by the forward model
     *
     * @param currentState the current game state
     * @param actionTaken  the action taken by the current player, already applied to the game state
     */
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        // TODO: implement any game-specific functionality that should run after an Action is executed
        // TODO: Unlike _beforeAction, this is almost always implemented
        // TODO: This generally does things like checking for end of turn or round or game (and then doing the
        // TODO: appropriate actions).

        TheGameGS gs = (TheGameGS) currentState;

        if (gs.isActionInProgress())
            return;  // we always wait for any EAS to finish

        int nextPlayer = (gs.getCurrentPlayer() + 1) % gs.getNPlayers(); // we increment one more
        endPlayerTurn(gs, nextPlayer);

        boolean canPlay = new PlayingCards(gs.getCurrentPlayer()).canBePlayed(gs);
        if(!canPlay)
        {
            //Game over.
            TheGameParameters params = (TheGameParameters) gs.getGameParameters();
            int originalCardsInDrawDeck = params.maxCardNumber - params.minCardNumber - 1;
            int gameScore = (int) gs.getGameScore(0);
            CoreConstants.GameResult result = (originalCardsInDrawDeck == gameScore) ?  CoreConstants.GameResult.WIN_GAME :
                    CoreConstants.GameResult.LOSE_GAME;

            for(int i = 0; i < gs.getNPlayers(); ++i)
                gs.setPlayerResult(result, i);
            gs.setGameStatus(result);
            System.out.println("Game End (" + result + "). Score: " + gameScore + " / " + originalCardsInDrawDeck);
        }

    }


}

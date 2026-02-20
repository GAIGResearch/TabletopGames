package games.thegame;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import games.root.actions.choosers.ChooseNumber;
import games.thegame.actions.PlayCard;
import games.thegame.components.TheGameCard;
import games.thegame.components.TheGameDeck;

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

        gs.gamePhase = TheGameGS.TheGamePhase.SelectingRow;

    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        TheGameGS gs = (TheGameGS) gameState;
        int playerID = gs.getCurrentPlayer();


        switch (gs.gamePhase)
        {
            case TheGameGS.TheGamePhase.SelectingRow ->
            {
                actions = availableRows(gs, playerID);
                boolean mustKeepPlaying = gs.currentPlayerPlayedCards < gs.getCardsToPlay();
                if(!mustKeepPlaying)
                    actions.add(new DoNothing());

            }
            case TheGameGS.TheGamePhase.PlayingCards ->
            {
                Deck<TheGameCard> playerHand = gs.playerHands.get(playerID);
                int row = gs.selectedRows.get(playerID);
                TheGameDeck<TheGameCard> r = gs.cardRows.get(row);
                for (TheGameCard card : playerHand) {
                    if (gs.canPlayInRow(card, r)) {
                        actions.add(new PlayCard(card.number, card.getComponentID(), row));
                    }
                }
            }
        }
//        actions.add(new PlayingCards(gs.getCurrentPlayer()));
        return actions;
    }

    private List<AbstractAction> availableRows(TheGameGS gs, int playerID ){
        List<AbstractAction> actions = new ArrayList<>();
        Deck<TheGameCard> playerHand = gs.playerHands.get(playerID);
        int row = 0;
        for(TheGameDeck<TheGameCard> r : gs.cardRows) {
            for (TheGameCard card : playerHand) {
                if (gs.canPlayInRow(card, r)) {
                    actions.add(new ChooseNumber(playerID, row));
                    break;
                }
            }
            row++;
        }
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
        TheGameParameters params = (TheGameParameters) gs.getGameParameters();

        if (gs.isActionInProgress() || gs.getGameStatus() == CoreConstants.GameResult.GAME_END)
            return;  // we always wait for any EAS to finish

        switch (gs.gamePhase) {
            case TheGameGS.TheGamePhase.SelectingRow -> {
                if(actionTaken instanceof ChooseNumber cn)
                {
                    gs.selectedRows.put(gs.getCurrentPlayer(), cn.number);
                    gs.gamePhase = TheGameGS.TheGamePhase.PlayingCards;
                }else if(actionTaken instanceof DoNothing)
                {
                    advancePlayer(gs, params);
                }
            }
            case TheGameGS.TheGamePhase.PlayingCards ->
            {
                if(actionTaken instanceof PlayCard pc)
                {
                    gs.currentPlayerPlayedCards++;
                    boolean mustKeepPlaying = gs.currentPlayerPlayedCards < gs.getCardsToPlay();

                    if(mustKeepPlaying && availableRows(gs, gs.getCurrentPlayer()).isEmpty())
                        gs.gameOver();

                    else gs.gamePhase = TheGameGS.TheGamePhase.SelectingRow;
                    gs.selectedRows.clear();
                }
            }
        }

    }

    protected void advancePlayer(TheGameGS gs, TheGameParameters params)
    {
        while(gs.playerHands.get(gs.getCurrentPlayer()).getSize() < params.handSize[gs.getNPlayers()] && gs.drawDeck.getSize() > 0)
            gs.playerHands.get(gs.getCurrentPlayer()).add(gs.drawDeck.draw());

        gs.currentPlayerPlayedCards = 0;
        gs.selectedRows.clear();

        endPlayerTurn(gs);

        //Game over?
        if(availableRows(gs, gs.getCurrentPlayer()).isEmpty())
        {
            gs.gameOver();
        }

        gs.gamePhase = TheGameGS.TheGamePhase.SelectingRow;
    }

    protected void endGame(AbstractGameState abstractGameState)
    {
        TheGameGS gs = (TheGameGS) abstractGameState;
        TheGameParameters params = (TheGameParameters) gs.getGameParameters();
        System.out.println("Game End (" + gs.getPlayerResults()[gs.getCurrentPlayer()] + "). Score: " +
                gs.getGameScore(0) + " / " + params.maxScore);
    }


}

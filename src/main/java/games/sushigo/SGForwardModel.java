package games.sushigo;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sushigo.actions.DebugAction;
import games.sushigo.actions.PlayCardAction;
import games.sushigo.cards.SGCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.joining;

public class SGForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SGGameState SGGS = (SGGameState) firstState;

        //Setup player scores
        SGGS.playerScore = new int[firstState.getNPlayers()];
        SGGS.playerCardPicks = new int[firstState.getNPlayers()];

        //Setup draw & discard piles
        SetupDrawpile(SGGS);
        SGGS.discardPile = new Deck<SGCard>("Discard pile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        //Setup player hands and fields
        SGGS.playerHands = new ArrayList<>();
        SGGS.playerFields = new ArrayList<>();
        switch (firstState.getNPlayers())
        {
            case 2:
                SGGS.cardAmount = 10;
                break;
            case 3:
                SGGS.cardAmount = 9;
                break;
            case 4:
                SGGS.cardAmount = 8;
                break;
            case 5:
                SGGS.cardAmount = 7;
                break;

        }
        for (int i = 0; i < SGGS.getNPlayers(); i++){
            SGGS.playerHands.add(new Deck<SGCard>("Player" + i + " hand", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            SGGS.playerFields.add(new Deck<SGCard>("Player" + "Card field", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
        }
        DrawNewHands(SGGS);

        SGGS.getTurnOrder().setStartingPlayer(0);
    }

    public void DrawNewHands(SGGameState SGGS)
    {
        for (int i = 0; i < SGGS.getNPlayers(); i++){
            for (int j = 0; j < SGGS.cardAmount; j++)
            {
                SGGS.playerHands.get(i).add(SGGS.drawPile.draw());
            }
        }
    }

    private void SetupDrawpile(SGGameState SGGS)
    {
        SGParameters parameters = (SGParameters) SGGS.getGameParameters();
        SGGS.drawPile = new Deck<SGCard>("Draw pile", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        for (int i = 0; i < parameters.nMaki_3Cards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Maki_3, 0));
        }
        for (int i = 0; i < parameters.nMaki_2Cards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Maki_2, 0));
        }
        for (int i = 0; i < parameters.nMaki_1Cards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Maki_1, 0));
        }
        for (int i = 0; i < parameters.nChopstickCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Chopsticks, 0));
        }
        for (int i = 0; i < parameters.nTempuraCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Tempura, 0));
        }
        for (int i = 0; i < parameters.nSashimiCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Sashimi, 0));
        }
        for (int i = 0; i < parameters.nDumplingCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Dumpling, 0));
        }
        for (int i = 0; i < parameters.nSquidNigiriCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.SquidNigiri, 3));
        }
        for (int i = 0; i < parameters.nSalmonNigiriCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.SalmonNigiri, 2));
        }
        for (int i = 0; i < parameters.nEggNigiriCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.EggNigiri, 1));
        }
        for (int i = 0; i < parameters.nWasabiCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Wasabi, 0));
        }
        for (int i = 0; i < parameters.nPuddingCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Pudding, 0));
        }
        SGGS.drawPile.shuffle(new Random());
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        if(currentState.getGameStatus() == Utils.GameResult.GAME_END) return;

        //Perform action
        action.execute(currentState);

        //Rotate deck and reveal cards
        SGGameState SGGS = (SGGameState)currentState;
        int turn = SGGS.getTurnOrder().getTurnCounter();
        if((turn + 1) % 4 == 0)
        {
            RevealCards(SGGS);
            RotateDecks(SGGS);
        }

        //Calculate points
        CalculatePoints();

        //Check if game/round over
        if(IsRoundOver(SGGS))
        {
            if(SGGS.getTurnOrder().getRoundCounter() >= 2)
            {
                currentState.setGameStatus(Utils.GameResult.GAME_END);
                return;
            }
            SGGS.getTurnOrder().endRound(currentState);
            return;
        }

        //End turn
        if (currentState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            currentState.getTurnOrder().endPlayerTurn(currentState);
        }
    }

    boolean IsRoundOver(SGGameState SGGS)
    {
        for (int i = 0; i < SGGS.getPlayerDecks().size(); i++)
        {
            if(SGGS.getPlayerDecks().get(i).getSize() > 0) return false;
        }
        return true;
    }

    void RevealCards(SGGameState SGGS)
    {
        for(int i = 0; i < SGGS.getNPlayers(); i++)
        {
            if(SGGS.getPlayerDecks().get(i).getSize() <= SGGS.getPlayerCardPicks()[i]) continue;
            SGCard cardToReveal = SGGS.getPlayerDecks().get(i).get(SGGS.getPlayerCardPicks()[i]);
            SGGS.getPlayerDecks().get(i).remove(cardToReveal);
            SGGS.getPlayerFields().get(i).add(cardToReveal);
        }
    }

    void RotateDecks(SGGameState SGGS)
    {
        Deck<SGCard> tempDeck;
        tempDeck = SGGS.getPlayerDecks().get(0).copy();
        for(int i = 1; i < SGGS.getNPlayers(); i++)
        {
            SGGS.getPlayerDecks().set(i - 1, SGGS.getPlayerDecks().get(i).copy());
        }
        SGGS.getPlayerDecks().set(SGGS.getNPlayers() - 1, tempDeck.copy());
    }

    void CalculatePoints()
    {

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SGGameState SGGS = (SGGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();

        int deckFromId = SGGS.getPlayerDecks().get(gameState.getCurrentPlayer()).getComponentID();
        int deckToId = SGGS.getPlayerFields().get(gameState.getCurrentPlayer()).getComponentID();
        Deck<SGCard> currentPlayerHand = SGGS.getPlayerDecks().get(SGGS.getCurrentPlayer());
        for (int i = 0; i < currentPlayerHand.getSize(); i++){
            switch (currentPlayerHand.get(i).type) {
                case Maki_1:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Maki_1));
                    break;
                case Maki_2:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Maki_2));
                    break;
                case Maki_3:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Maki_3));
                    break;
                case Tempura:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Tempura));
                    break;
                case Sashimi:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Sashimi));
                    break;
                case Dumpling:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Dumpling));
                    break;
                case SquidNigiri:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.SquidNigiri));
                    break;
                case SalmonNigiri:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.SalmonNigiri));
                    break;
                case EggNigiri:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.EggNigiri));
                    break;
                case Wasabi:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Wasabi));
                    break;
                case Chopsticks:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Chopsticks));
                    break;
                case Pudding:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Pudding));
                    break;
            }
        }
        if(actions.size() <= 0) actions.add(new DebugAction());
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new SGForwardModel();
    }
}
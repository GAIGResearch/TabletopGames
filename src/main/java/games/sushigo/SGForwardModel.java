package games.sushigo;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sushigo.actions.DebugAction;
import games.sushigo.actions.PlayCardAction;
import games.sushigo.cards.SGCard;

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

        //Setup draw & discard piles
        SetupDrawpile(SGGS);
        SGGS.discardPile = new Deck<>("Discard pile");

        //Setup player hands and fields
        SGGS.playerHands = new ArrayList<>();
        SGGS.playerFields = new ArrayList<>();
        for (int i = 0; i < SGGS.getNPlayers(); i++){
            SGGS.playerHands.add(new Deck<>("Player" + i + " hand", i));
            SGGS.playerFields.add(new Deck<>("Player" + "Card field", i));
            int cardAmount = 0;
            switch (firstState.getNPlayers())
            {
                case 2:
                    cardAmount = 10;
                    break;
                case 3:
                    cardAmount = 9;
                    break;
                case 4:
                    cardAmount = 8;
                    break;
                case 5:
                    cardAmount = 7;
                    break;

            }
            for (int j = 0; j < cardAmount; j++)
            {
                SGGS.playerHands.get(i).add(SGGS.drawPile.draw());
            }
        }

        SGGS.getTurnOrder().setStartingPlayer(0);
    }

    private void SetupDrawpile(SGGameState SGGS)
    {
        SGParameters parameters = (SGParameters) SGGS.getGameParameters();
        SGGS.drawPile = new Deck<>("Draw pile");
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
        SGGameState SGGS = (SGGameState)currentState;
        SGTurnOrder turnOrder = (SGTurnOrder) SGGS.getTurnOrder();
        //Show cards after everyone has picked a card
        int turn = SGGS.getTurnOrder().getTurnCounter();
        if(turn % 4 == 0) System.out.println("Show cards!");


        //Perform action
        action.execute(currentState);


        //Round over
        if(IsRoundOver(SGGS))
        {
            turnOrder.endRound(currentState);
        }

        //End turn
        turnOrder.endPlayerTurn(currentState);

    }

    boolean IsRoundOver(SGGameState SGGS)
    {
        for (int i = 0; i < SGGS.getPlayerDecks().size(); i++)
        {
            if(SGGS.getPlayerDecks().get(i).getSize() > 0) return false;
        }
        return true;
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
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.Maki_1));
                    break;
                case Maki_2:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.Maki_2));
                    break;
                case Maki_3:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.Maki_3));
                    break;
                case Tempura:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.Tempura));
                    break;
                case Sashimi:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.Sashimi));
                    break;
                case Dumpling:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.Dumpling));
                    break;
                case SquidNigiri:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.SquidNigiri));
                    break;
                case SalmonNigiri:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.SalmonNigiri));
                    break;
                case EggNigiri:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.EggNigiri));
                    break;
                case Wasabi:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.Wasabi));
                    break;
                case Chopsticks:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.Chopsticks));
                    break;
                case Pudding:
                    actions.add(new PlayCardAction(deckFromId, deckToId, i, SGCard.SGCardType.Pudding));
                    break;
            }
        }
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new SGForwardModel();
    }
}

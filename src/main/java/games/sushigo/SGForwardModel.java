package games.sushigo;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sushigo.actions.DebugAction;
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

            SGGS.playerFields.get(i).add(new SGCard(SGCard.SGCardType.Pudding, 0));
            SGGS.playerFields.get(i).add(new SGCard(SGCard.SGCardType.Chopsticks, 0));
            SGGS.playerFields.get(i).add(new SGCard(SGCard.SGCardType.EggNigiri, 0));
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
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.SquidNigiri, 0));
        }
        for (int i = 0; i < parameters.nSalmonNigiriCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.SalmonNigiri, 0));
        }
        for (int i = 0; i < parameters.nEggNigiriCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.EggNigiri, 0));
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
        int turn = SGGS.getTurnOrder().getTurnCounter();
        if(turn % 4 == 0) System.out.println("Show cards!");
        currentState.getTurnOrder().endPlayerTurn(currentState);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        actions.add(new DebugAction());
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new SGForwardModel();
    }
}

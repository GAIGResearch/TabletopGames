package games.hanabi.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.hanabi.HanabiCard;
import games.hanabi.HanabiGameState;
import games.hanabi.HanabiParameters;
import games.uno.actions.NoCards;

import java.util.List;
import java.util.Random;

public class Play extends AbstractAction implements IPrintable {
    protected int deckFrom;
    protected int fromIndex;

    final String color;
    final int number;

    public Play (int deckFrom, int fromIndex, String color, int number) {
        this.deckFrom = deckFrom;
        this.fromIndex = fromIndex;

        this.color = color;
        this.number = number;
    }
    @Override
    public boolean execute(AbstractGameState gameState) {
        Deck<HanabiCard> from = (Deck<HanabiCard>) gameState.getComponentById(deckFrom);
        HanabiCard playCard = from.pick(fromIndex);
        HanabiParameters hbp = (HanabiParameters) gameState.getGameParameters();
        HanabiGameState hbgs = (HanabiGameState) gameState;
        Deck<HanabiCard> drawDeck = hbgs.getDrawDeck();
        List<HanabiCard> currentCard = hbgs.getCurrentCard();
        Deck<HanabiCard> discardDeck = hbgs.getDiscardDeck();
        PartialObservableDeck<HanabiCard> playerDeck = hbgs.getPlayerDecks().get(hbgs.getCurrentPlayer());
        Counter failCounter = hbgs.getFailCounter();
        int listIndex = 0;
        boolean color = false;
        boolean checkColor = false;
        boolean checkNumber = false;
        // make random color and number is not known;
        // set the card if correct;

        for(HanabiCard cd: currentCard){

            if(!(playCard.numberVisibility)){
                Random random = new Random(hbgs.getGameParameters().getRandomSeed());
                int nnumber = cd.possibleNumber.get(random.nextInt(cd.possibleNumber.size())) ;
                playCard.setNumber(nnumber);
            }
            if(!(playCard.colorVisibility)){
                Random random2 = new Random(hbgs.getGameParameters().getRandomSeed());
                String cnumber = cd.possibleColour.get(random2.nextInt(cd.possibleColour.size()));
                playCard.setColor(cnumber);
            }
            if(cd.color.equals(playCard.color)){

                if(cd.number + 1 == playCard.number){
                    currentCard.set(listIndex, playCard);
//                    System.out.println("Played successfully + color: " + playCard.color + " number " + playCard.number);
                }
                else {
                    discardDeck.add(playCard);
                    failCounter.decrement(1);
//                    System.out.println("Failed + color: " + playCard.color + " number " + playCard.number);
                }
                color = true;
            }
            listIndex += 1;
        }
        if (!color){
            if(playCard.number == 1){
                currentCard.add(playCard);
//                System.out.println("Played successfully + color: " + playCard.color + " number " + playCard.number);
            }
            else{
                discardDeck.add(playCard);
                failCounter.decrement(1);
//                System.out.println("Failed + color: " + playCard.color + " number " + playCard.number);
            }
        }
        if(drawDeck.getComponents().size() > 0) {
            HanabiCard card = drawDeck.draw();
            playerDeck.add(card);
        }
        return true;
    }




    @Override
    public AbstractAction copy() {
        return new Play(deckFrom, fromIndex, color, number);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Play;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Played card and draw a card.";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("Played card and draw a card.");
    }

    @Override
    public String toString() {
        return "Play " + color + " " + number;
    }
}

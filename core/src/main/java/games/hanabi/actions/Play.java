package games.hanabi.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.hanabi.CardType;
import games.hanabi.HanabiCard;
import games.hanabi.HanabiGameState;
import games.hanabi.HanabiParameters;

import java.util.List;
import java.util.Objects;

public class Play extends AbstractAction implements IPrintable {
    protected int playerId;
    protected int fromIndex;

    public Play (int playerId, int fromIndex) {
        this.playerId = playerId;
        this.fromIndex = fromIndex;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        HanabiParameters hbp = (HanabiParameters) gameState.getGameParameters();
        HanabiGameState hbgs = (HanabiGameState) gameState;
        Deck<HanabiCard> playerDeck = hbgs.getPlayerDecks().get(playerId);
        HanabiCard playCard = playerDeck.pick(fromIndex);

        Deck<HanabiCard> drawDeck = hbgs.getDrawDeck();
        List<HanabiCard> currentCard = hbgs.getCurrentCard();
        Deck<HanabiCard> discardDeck = hbgs.getDiscardDeck();
        Counter failCounter = hbgs.getFailCounter();
        int listIndex = 0;
        boolean color = false;
        boolean checkColor = false;
        boolean checkNumber = false;
        // make random color and number is not known;
        // set the card if correct;

        for(HanabiCard cd: currentCard){
            if(!(playCard.numberVisibility)){
                int nnumber = cd.possibleNumber.get(hbgs.getRnd().nextInt(cd.possibleNumber.size())) ;
                playCard.setNumber(nnumber);
            }
            if(!(playCard.colorVisibility)){
                CardType cnumber = cd.possibleColour.get(hbgs.getRnd().nextInt(cd.possibleColour.size()));
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
        return new Play(playerId, fromIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Play)) return false;
        Play play = (Play) o;
        return playerId == play.playerId && fromIndex == play.fromIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, fromIndex);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("Played card and draw a card.");
    }

    @Override
    public String toString() {
        return "Play card idx " + fromIndex;
    }
}

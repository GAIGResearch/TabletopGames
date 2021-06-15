package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import games.blackjack.BlackjackGameState;
import games.uno.UnoTurnOrder;
import games.uno.actions.PlayCard;
import games.uno.cards.UnoCard;
import games.uno.UnoGameState;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class playCard extends DrawCard implements IPrintable {
    //private String suite;

    public playCard(int deckFrom,  int deckTo, int cardToPlay) {
        super(deckFrom, deckTo,  cardToPlay);
    }

    @Override
    public boolean execute(AbstractGameState gameState){
        BlackjackGameState bjgs = (BlackjackGameState)gameState;
        super.execute(gameState);

        //Random r = new Random(bjgs.getGameParameters().getRandomSeed() + bjgs.getTurnOrder().getRoundCounter());

        FrenchCard cardToPlay = (FrenchCard) gameState.getComponentById(cardId);
        //bjgs.updateCurrentCard(cardToPlay);

        int nextPlayer = gameState.getTurnOrder().nextPlayer(gameState);
        Deck<FrenchCard> drawDeck = bjgs.DrawDeck();
        List<Deck<FrenchCard>> playerDecks = bjgs.PlayerDecks();
        Deck<FrenchCard> tableDeck = bjgs.getTableDeck();

/*        switch (cardToPlay.type){
            case Ace:
                if
        }*/
        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState){
        System.out.println(getString((gameState)));
    }

    @Override
    public String toString(){
        return "Play Card";
    }

    @Override
    public String getString(AbstractGameState gameState){
        return "Play  " + getCard(gameState).toString();
    }

    @Override
    public Card getCard(AbstractGameState gs){
        if (!executed){
            Deck<FrenchCard> deck = (Deck<FrenchCard>) gs.getComponentById(deckFrom);
            if (fromIndex == deck.getSize()) return deck.get(fromIndex-1);
            return deck.get(fromIndex);
        }
        return (FrenchCard) gs.getComponentById(cardId);
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof PlayCard)) return false;
        if (!super.equals(o)) return false;
        playCard play = (playCard) o;
        //return Objects.equals(suite, play.suite);
        return true;
    }

    @Override
    public int hashCode(){
        return Objects.hash(super.hashCode());
    }

    @Override
    public AbstractAction copy(){
        return new playCard(deckFrom,deckTo,fromIndex);
    }

}

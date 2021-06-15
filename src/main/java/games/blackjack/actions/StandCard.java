package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackTurnOrder;
import games.uno.UnoTurnOrder;
import games.uno.actions.PlayCard;
import games.uno.cards.UnoCard;
import games.uno.UnoGameState;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class StandCard extends DrawCard implements IPrintable {
    public StandCard(int deckFrom,  int deckTo, int cardToPlay) {
        super(deckFrom, deckTo, cardToPlay);
    }

    @Override
    public boolean execute(AbstractGameState gameState){
        BlackjackGameState bjgs = (BlackjackGameState)gameState;
        super.execute(gameState);
        ((BlackjackTurnOrder) gameState.getTurnOrder()).skip();
        return true;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof HitCard)) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode(){
        return Objects.hash(super.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState){
        return "Stand";
    }

    @Override
    public void printToConsole(){
        System.out.println("Stand");
    }

    @Override
    public AbstractAction copy(){
        return new HitCard(deckFrom,deckTo,fromIndex);
    }

}

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

public class HitCard extends DrawCard implements IPrintable {
    public HitCard(int deckFrom,  int deckTo, int cardToPlay) {
        super(deckFrom, deckTo,  cardToPlay);
    }

    @Override
    public boolean execute(AbstractGameState gameState){
        BlackjackGameState bjgs = (BlackjackGameState)gameState;
        super.execute(gameState);
        Deck<FrenchCard> drawDeck = bjgs.DrawDeck();
        List<Deck<FrenchCard>> playerDecks = bjgs.PlayerDecks();
        playerDecks.get(bjgs.getCurrentPlayer()).add(drawDeck.draw());
        //bjgs.PlayerDecks().get(bjgs.getCurrentPlayer()).add(bjgs.DrawDeck().draw());
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
        return "Hit";
    }

    @Override
    public void printToConsole(){
        System.out.println("Hit a card");
    }

    @Override
    public AbstractAction copy(){
        return new HitCard(deckFrom,deckTo,fromIndex);
    }

}

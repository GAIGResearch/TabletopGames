package games.poker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import games.poker.PokerGameState;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Call extends DrawCard implements IPrintable {

    private int playerHandId;
    private int playerMoney = 0;

    public Call(int deckFrom, int deckTo, int deckSize, int playerMoney) {
        //this.playerHandId = playerHandId;
        super(deckFrom, deckTo, deckSize);
        this.playerMoney = playerMoney;
        playerHandId = deckFrom;
    }


    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        super.execute(gameState);

        Random r = new Random(pgs.getGameParameters().getRandomSeed() + pgs.getTurnOrder().getRoundCounter());
        playerMoney = pgs.getPreviousBet();

        pgs.replacePlayerMoney(pgs.getCurrentPlayer(), playerMoney);
        Deck<FrenchCard> drawDeck = pgs.getDrawDeck();
        Deck<FrenchCard> discardDeck = pgs.getDiscardDeck();
        List<Deck<FrenchCard>> playerDecks = pgs.getPlayerDecks();
        //System.out.println("call from " + pgs.getCurrentPlayer());
        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(getString(gameState));
    }


    @Override
    public String getString(AbstractGameState gameState) {
        return "Call";
    }

    @Override
    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            Deck<FrenchCard> deck = (Deck<FrenchCard>) gs.getComponentById(deckFrom);
            if (fromIndex == deck.getSize()) return deck.get(fromIndex-1);
            return deck.get(fromIndex);
        }
        return (FrenchCard) gs.getComponentById(cardId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Call)) return false;
        if (!super.equals(o)) return false;
        Call that = (Call) o;
        return Objects.equals(playerMoney, that.playerMoney);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerMoney);
    }

    @Override
    public AbstractAction copy() {
        return new Call(deckFrom, deckTo, fromIndex, playerMoney);
    }

}

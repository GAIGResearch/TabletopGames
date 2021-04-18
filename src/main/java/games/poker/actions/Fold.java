package games.poker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import games.poker.PokerGameState;
import utilities.Utils;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Fold extends AbstractAction implements IPrintable {

    private int playerHandId;
    private int money = 0;

    public Fold(int deckFrom) {
        this.playerHandId = deckFrom;
        //super(deckFrom);
    }

    public Fold(int deckFrom, int money) {
        this.playerHandId = deckFrom;
        this.money = money;
        //super(deckFrom, money);
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        //super.execute(gameState);

        Random r = new Random(pgs.getGameParameters().getRandomSeed() + pgs.getTurnOrder().getRoundCounter());
        pgs.setPlayerResult(Utils.GameResult.LOSE, pgs.getCurrentPlayer());
        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("Fold");
    }


    @Override
    public String getString(AbstractGameState gameState) {
        return "Fold";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fold)) return false;
        Fold that = (Fold) o;
        return Objects.equals(money, that.money);
    }

    @Override
    public int hashCode() {
        return Objects.hash(money);
    }

    @Override
    public AbstractAction copy() {
        return new Fold(playerHandId, money);
    }

}

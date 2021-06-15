package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import games.blackjack.BlackjackForwardModel;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;

import java.util.Objects;

public class Hit extends AbstractAction implements IPrintable {
    private int playerHandID;

    public Hit(int playerHandID){
        this.playerHandID = playerHandID;
    }

    @Override
    public boolean execute(AbstractGameState gameState){
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        Deck<FrenchCard> playerHand = (Deck<FrenchCard>) bjgs.getComponentById(playerHandID);

        FrenchCard card  = bjgs.DrawDeck().draw();
        playerHand.add(card);
        String[] s = new String[5];
        s[0] = "Player " + bjgs.getCurrentPlayer() + " draws: " + card.toString();
        StringBuilder sBuilder = new StringBuilder();
        s[1] = "Player Hand: ";
        for (FrenchCard cards : playerHand.getComponents()){
            sBuilder.append(cards.toString());
            sBuilder.append(" ");
        }
        s[2] = sBuilder.toString();
        int point = bjgs.calcPoint(bjgs.getCurrentPlayer());
        String str = String.valueOf(point);
        s[3] = str;
        s[4] = "----------------------------------------------------";
        for (String x : s){
            System.out.println(x);
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new Hit(playerHandID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof  Hit)) return false;
        Hit that = (Hit) o;
        return playerHandID == that.playerHandID;
    }

    @Override
    public void printToConsole(){
        System.out.println("Hit");
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerHandID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Hit";
    }
}

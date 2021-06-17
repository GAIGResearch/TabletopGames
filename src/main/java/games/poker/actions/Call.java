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

public class Call extends AbstractAction implements IPrintable {

    private final int playerId;

    public Call(int id) {
        this.playerId = id;
    }


    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        int biggestBet = 0;
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (pgs.getBets()[i] > biggestBet) biggestBet = pgs.getBets()[i];
        }
        int diff = biggestBet - pgs.getBets()[playerId];

        pgs.getCurrentMoney()[playerId] -= diff;
        pgs.updateTotalPotMoney(diff);
        pgs.getBets()[playerId] = biggestBet;
        pgs.getPlayerNeedsToCall()[playerId] = false;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Call)) return false;
        Call call = (Call) o;
        return playerId == call.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public Call copy() {
        return new Call(playerId);
    }

}

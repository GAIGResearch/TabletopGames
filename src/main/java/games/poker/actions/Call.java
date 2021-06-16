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
        int previousPlayer = (gameState.getNPlayers() + playerId - 1) % gameState.getNPlayers();
        int diff = pgs.getBets()[previousPlayer] - pgs.getBets()[playerId];
        if (diff > 0) {
            pgs.getCurrentMoney()[playerId] -= diff;
            pgs.updateTotalPotMoney(diff);
            pgs.getBets()[playerId] = pgs.getBets()[previousPlayer];
        }

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

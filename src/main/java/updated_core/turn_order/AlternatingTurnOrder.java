package updated_core.turn_order;

import updated_core.players.AbstractPlayer;

import java.util.List;

public class AlternatingTurnOrder extends TurnOrder {
    List<AbstractPlayer> players;
    int currentPlayer = 0;
    int direction = 1;

    public AlternatingTurnOrder(List<AbstractPlayer> players){
        this.players = players;
    }

    @Override
    public void endPlayerTurn() {
        currentPlayer = (currentPlayer + direction) % players.size();
    }

    public AbstractPlayer getCurrentPlayer(){
        return players.get(currentPlayer);
    }

    public void reverse(){
        direction *= -1;
    }
}

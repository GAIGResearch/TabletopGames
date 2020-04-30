package updated_core.turn_order;

import updated_core.gamestates.AbstractGameState;
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
    public void endPlayerTurn(AbstractGameState gameState) {
        currentPlayer = (currentPlayer + direction) % players.size();
    }

    public AbstractPlayer getCurrentPlayer(AbstractGameState gameState){
        return players.get(currentPlayer);
    }

    public void reverse(){
        direction *= -1;
    }
}

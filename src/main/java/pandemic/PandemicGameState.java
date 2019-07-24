package pandemic;

import core.GameState;

public class PandemicGameState extends GameState {

    PandemicGameState() {}

    @Override
    public GameState copy() {
        //TODO: copy pandemic game state
        return this;
    }

    void setActivePlayer(int activePlayer) {
        this.activePlayer = activePlayer;
    }
}

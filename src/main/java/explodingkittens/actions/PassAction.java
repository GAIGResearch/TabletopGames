package explodingkittens.actions;


import core.GameState;

public class PassAction extends PlayCard implements IsNope {

    public PassAction(int playerID) {
        super(playerID, null);
    }

    @Override
    public boolean execute(GameState gs) {
        return false;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player %d passes", playerID);
    }

    @Override
    public boolean isNope() {
        return false;
    }
}

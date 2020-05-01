package games.carcassonne;

import core.GUI;
import core.Game;

import java.util.HashSet;

public class CarcassonneGame extends Game {
    @Override
    public void run(GUI gui) {

    }

    @Override
    public boolean isEnded() {
        return false;
    }

    @Override
    public HashSet<Integer> winners() {
        return null;
    }
}

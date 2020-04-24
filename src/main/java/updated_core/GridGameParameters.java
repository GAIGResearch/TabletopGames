package updated_core;


public class GridGameParameters extends GameParameters {
    public GridGameParameters(int nPlayers) {
        super(nPlayers);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    int width;
    int height;
}

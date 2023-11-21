package games.descent2e.actions;

import games.descent2e.components.Figure;
import utilities.Vector2D;

import java.util.List;

public class Approach extends Move{
    public Approach(Figure f, List<Vector2D> whereTo) {
        super(f, whereTo);
    }
}

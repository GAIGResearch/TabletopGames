package core.components;

import utilities.Vector2D;

import java.util.HashMap;
import java.util.HashSet;

public interface IBoardNode {

    boolean addNeighbour(IBoardNode neighbour);

    boolean removeNeighbour(IBoardNode neighbour);

    boolean addNeighbour(IBoardNode neighbour, int side);

    IBoardNode copy();

    int getId();

    HashSet getNeighbours();

    HashMap getNeighbourSideMapping();

    Vector2D getPosition();

    void setMaxNeighbours(int maxNeighbours);
}

package games.battleship;

import utilities.Vector2D;

import java.util.Arrays;

public class Ship {
    public int playerID = -1;
    public String name = "";
    public int size = 0;
    public int health = 0;
    public boolean defeated = false;
    public Vector2D position;
    public Vector2D[] coordinates;
    public boolean horizontal = false;

    public Ship(int playerID, String name, int size, boolean horizontal) {
        this.playerID = playerID;
        this.name = name;
        this.size = size;
        this.health = size;
        this.position = new Vector2D(-1, -1);
        this.coordinates = new Vector2D[size];
        this.horizontal = horizontal;
        this.defeated = false;
    }

    public void hit() {
        if (!defeated) {
            health--;
            if (health == 0) {
                defeated = true;
            }
        }
    }

    public void setPosition(Vector2D position) {
        this.position = position;
        for (int i = 0; i < size; i++) {
            if (horizontal)
            {
                coordinates[i] = new Vector2D(position.getX() + i, position.getY());
            }
            else
            {
                coordinates[i] = new Vector2D(position.getX(), position.getY() + i);
            }
        }
    }

    public Vector2D getPosition() {
        return position;
    }
    public Vector2D[] getCoordinates() {
        return coordinates;
    }

    public int getHealth() {
        return health;
    }
    public int getSize() {
        return size;
    }

    public boolean isDefeated() {
        return defeated;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public Ship copy() {
        Ship s = new Ship(playerID, name, size, horizontal);
        s.health = health;
        s.defeated = defeated;
        s.position = position.copy();
        for (int i = 0; i < size; i++) {
            s.coordinates[i] = coordinates[i].copy();
        }
        return s;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ship)) return false;
        Ship that = (Ship) o;
        return  playerID == that.playerID && name.equals(that.name) && size == that.size && health == that.health && position.equals(that.position) && Arrays.equals(coordinates, that.coordinates) && horizontal == that.horizontal;
    }

    public String toString() {
        return name + " - " + health + " - " + Arrays.toString(coordinates);
    }

    public int hashCode() {
        return name.hashCode() + playerID + size + health + position.hashCode() + Arrays.hashCode(coordinates) + (horizontal ? 1 : 0);
    }


}

package games.descent2e.abilities;

import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.List;

// This monster cannot spend surges on abilities unless it is within 3 spaces of any master monster or a lieutenant.
public class Cowardly {
    public static boolean isNearMasterOrLieutenant(DescentGameState dgs, Figure f) {
        boolean nearMasterOrLieutenant = false;
        int range = 3;
        Vector2D position = f.getPosition();

        // We need to check if this monster is near any master or lieutenant monster
        // We flatten the list for ease of processing, as we don't care what type of monster it is
        List<Monster> monsters = dgs.getMonsters().stream().flatMap(List::stream).toList();
        for (Monster m : monsters) {
            // We can ignore any minion monster, we only check for master monsters or lieutenants
            if (m.getName().contains("master") || m.isLieutenant()) {
                Vector2D other = m.getPosition();
                if (Math.abs(position.getX() - other.getX()) <= range && Math.abs(position.getY() - other.getY()) <= range) {
                    nearMasterOrLieutenant = true;
                    break;
                }
            }
        }
        /*if (!nearMasterOrLieutenant)
        {
            System.out.println("This monster is too cowardly to make a surge!");
        }*/
        return nearMasterOrLieutenant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }
    @Override
    public int hashCode() {
        return -23402;
    }
}

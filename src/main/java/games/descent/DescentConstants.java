package games.descent;

import utilities.Hash;

public class DescentConstants {

    public final static String[] archetypes = new String[] {"Warrior", "Mage", "Scout", "Healer"};
    public final static int connectionHash = Hash.GetInstance().hash("connections");
    public final static int terrainHash = Hash.GetInstance().hash("terrain");
    public final static int archetypeHash = Hash.GetInstance().hash("archetype");

}

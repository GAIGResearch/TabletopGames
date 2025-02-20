package games.descent2e;

import utilities.Hash;

public class DescentConstants {

    public final static int connectionHash = Hash.GetInstance().hash("connections");
    public final static int archetypeHash = Hash.GetInstance().hash("archetype");
    public final static int classHash = Hash.GetInstance().hash("class");
    public final static int xpHash = Hash.GetInstance().hash("XP");
    public final static int costHash = Hash.GetInstance().hash("cost");
    public final static int equipSlotHash = Hash.GetInstance().hash("equipSlots");
    public final static int defenceHash = Hash.GetInstance().hash("defence");
    public final static int attackHash = Hash.GetInstance().hash("attack");

    public final static int heroicFeatHash = Hash.GetInstance().hash("heroicFeat");
    public final static int abilityHash = Hash.GetInstance().hash("ability");
    public final static int setupHash = Hash.GetInstance().hash("setup");

}

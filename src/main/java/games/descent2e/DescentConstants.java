package games.descent2e;

import utilities.Hash;

import java.util.HashMap;
import java.util.Map;

public class DescentConstants {

    public final static String[] archetypes = new String[] {
//            "Warrior",
            "Mage",
//            "Scout",
            "Healer"
    };
    public final static Map<String, String[]> archetypeClassMap = new HashMap<String, String[]>() {{
       put("Mage", new String[] {
//                                "Necromancer",
                                "Runemaster",
//                                "Battlemage",
//                                "Conjurer",
//                                "Elementalist",
//                                "Geomancer",
//                                "Hexer",
//                                "Lorekeeper",
//                                "Truthseer"
                                });
       put("Healer", new String[] {
                                   "Disciple",
//                                   "Spiritspeaker",
//                                   "Apothecary",
//                                   "Bard",
//                                   "Crusader",
//                                   "Heretic",
//                                   "Prophet",
//                                   "Soul Reaper",
//                                   "Watchman"
                                    });
       put("Warrior", new String[] {
//                                   "Berserker",
//                                   "Knight",
//                                   "Avenger",
//                                   "Beastmaster",
//                                   "Champion",
//                                   "Marshal",
//                                   "Raider",
//                                   "Skirmisher",
//                                   "Steelcaster"
                                    });
       put("Scout", new String[] {
//                                   "Thief",
//                                   "Wildlander",
//                                   "Bounty Hunter",
//                                   "Monk",
//                                   "Ravager",
//                                   "Shadow Walker",
//                                   "Stalker",
//                                   "Treasure Hunter",
//                                   "Trickster"
       });
    }};
    
    public final static int connectionHash = Hash.GetInstance().hash("connections");
    public final static int archetypeHash = Hash.GetInstance().hash("archetype");
    public final static int classHash = Hash.GetInstance().hash("class");
    public final static int xpHash = Hash.GetInstance().hash("XP");
    public final static int costHash = Hash.GetInstance().hash("cost");
    public final static int equipSlotHash = Hash.GetInstance().hash("equipSlots");
    public final static int defenceHash = Hash.GetInstance().hash("defence");
    public final static int movementHash = Hash.GetInstance().hash("movement");
    public final static int fatigueHash = Hash.GetInstance().hash("fatigue");
    public final static int healthHash = Hash.GetInstance().hash("hp");

    public final static int mightHash = Hash.GetInstance().hash("might");
    public final static int knowledgeHash = Hash.GetInstance().hash("knowledge");
    public final static int willpowerHash = Hash.GetInstance().hash("willpower");
    public final static int awarenessHash = Hash.GetInstance().hash("awareness");
    public final static int heroicFeatHash = Hash.GetInstance().hash("heroicFeat");
    public final static int abilityHash = Hash.GetInstance().hash("ability");
    public final static int setupHash = Hash.GetInstance().hash("setup");

    public enum AttackType {
        NONE, MELEE, RANGED, BLAST;
    }
}

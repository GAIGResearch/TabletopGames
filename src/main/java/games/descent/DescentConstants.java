package games.descent;

import utilities.Hash;

import java.util.HashMap;

public class DescentConstants {

    public final static String[] archetypes = new String[] {
//            "Warrior",
            "Mage",
//            "Scout",
            "Healer"
    };
    public final static HashMap<String, String[]> archetypeClassMap = new HashMap<String, String[]>() {{
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
    public final static int terrainHash = Hash.GetInstance().hash("terrain");
    public final static int archetypeHash = Hash.GetInstance().hash("archetype");
    public final static int classHash = Hash.GetInstance().hash("class");
    public final static int xpHash = Hash.GetInstance().hash("XP");
    public final static int costHash = Hash.GetInstance().hash("cost");
    public final static int equipSlotHash = Hash.GetInstance().hash("equipSlots");

}

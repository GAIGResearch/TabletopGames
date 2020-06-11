package games.descent;

import utilities.Hash;

import java.util.HashMap;

public class DescentConstants {

    public final static String[] archetypes = new String[] {"Warrior", "Mage", "Scout", "Healer"};
    public final static HashMap<String, String[]> archetypeClassMap = new HashMap<String, String[]>() {{
       put("Mage", new String[] {"Battlemage",
                                "Conjurer",
                                "Elementalist",
                                "Geomancer",
                                "Hexer",
                                "Lorekeeper",
                                "Necromancer",
                                "Runemaster",
                                "Truthseer"});
       put("Healer", new String[] {"Apothecary",
                                   "Bard",
                                   "Crusader",
                                   "Disciple",
                                   "Heretic",
                                   "Prophet",
                                   "Soul Reaper",
                                   "Spiritspeaker",
                                   "Watchman"});
       put("Warrior", new String[] {"Avenger",
                                   "Beastmaster",
                                   "Berserker",
                                   "Champion",
                                   "Knight",
                                   "Marshal",
                                   "Raider",
                                   "Skirmisher",
                                   "Steelcaster"});
       put("Scout", new String[] { "Bounty Hunter",
                                   "Monk",
                                   "Ravager",
                                   "Shadow Walker",
                                   "Stalker",
                                   "Thief",
                                   "Treasure Hunter",
                                   "Trickster",
                                   "Wildlander"});
    }};
    
    public final static int connectionHash = Hash.GetInstance().hash("connections");
    public final static int terrainHash = Hash.GetInstance().hash("terrain");
    public final static int archetypeHash = Hash.GetInstance().hash("archetype");

}

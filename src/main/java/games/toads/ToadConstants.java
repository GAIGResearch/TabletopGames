package games.toads;

import core.interfaces.IGamePhase;
import games.toads.abilities.*;

public class ToadConstants {

    public static int ASSASSIN_KILLS = 7;

    public enum ToadGamePhase implements IGamePhase {
        DISCARD,
        PLAY,
        POST_BATTLE
    }

    public enum ToadCardType {
        BOMB(new Bomb()),
        ASSAULT_CANNON (new AssaultCannon()),
        ASSASSIN (new Assassin()),
        SCOUT (new Scout()),
        TRICKSTER (new Trickster()),
        SABOTEUR (new SaboteurII()),
        BERSERKER (new Berserker()),
        ICON_BEARER (new IconBearer()),
        GENERAL_ONE (new GeneralOne()),
        GENERAL_TWO (new GeneralTwo()),
        NONE_OF_THESE (new NoAbility());

        public final ToadAbility defaultAbility;

        ToadCardType(ToadAbility ability) {
            this.defaultAbility = ability;
        }

        public String prettyString() {
            // replace underscores with spaces and capitalize the first letter of each word only
            String[] words = this.name().split("_");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                sb.append(word.charAt(0)).append(word.substring(1).toLowerCase()).append(" ");
            }
            return sb.toString().trim();
        }

        public static ToadCardType fromString(String type) {
            // replace spaces with underscores and convert to uppercase
            return ToadCardType.valueOf(type.toUpperCase().replace(" ", "_"));
        }
    }
}

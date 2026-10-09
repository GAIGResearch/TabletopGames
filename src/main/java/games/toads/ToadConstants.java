package games.toads;

import core.interfaces.IGamePhase;
import games.toads.abilities.*;

public class ToadConstants {

    public static int ASSASSIN_KILLS = 7;

    public enum ToadGamePhase implements IGamePhase {
        OPENING_RETURN,
        DISCARD,
        PLAY,
        POST_BATTLE
    }

    public enum ToadCardType {
        BOMB(new Bomb()),
        ASSAULT_CANNON (new AssaultCannon()),
        ASSASSIN (new AssassinII()),
        SCOUT (new Scout()),
        TRICKSTER (new TricksterII()),
        SABOTEUR (new SaboteurIII()),
        BERSERKER (new BerserkerII()),
        ICON_BEARER (new IconBearer()),
        GENERAL_ONE (new GeneralHostages()),
        GENERAL_TWO (new GeneralFlags()),
        BODYGUARD (new Bodyguard()),
        SIEGE_CANNON (new SiegeCannon()),
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

        /**
         * The type guessed by a Siege Cannon for a card of this type. Both Generals are printed "General", so share one.
         */
        public ToadCardType guessGroup() {
            return this == GENERAL_TWO ? GENERAL_ONE : this;
        }

        /**
         * The printed name guessed by a Siege Cannon.
         */
        public String guessName() {
            return guessGroup() == GENERAL_ONE ? "General" : prettyString();
        }

        public static ToadCardType fromString(String type) {
            // replace spaces with underscores and convert to uppercase
            return ToadCardType.valueOf(type.toUpperCase().replace(" ", "_"));
        }
    }
}

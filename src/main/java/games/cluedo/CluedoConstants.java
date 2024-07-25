package games.cluedo;

public class CluedoConstants {

    public enum Character {
        MISS_SCARLETT,
        COL_MUSTARD,
        DR_ORCHID,
        REV_GREEN,
        MRS_PEACOCK,
        PROF_PLUM;

        public static boolean contains(String test) {
            for (Character character : Character.values()) {
                if (character.name().equals(test)) { return true; }
            }
            return false;
        }
    }

    public enum Weapon {
        ROPE,
        DAGGER,
        WRENCH,
        REVOLVER,
        CANDLESTICK,
        LEAD_PIPE;

        public static boolean contains(String test) {
            for (Weapon weapon : Weapon.values()) {
                if (weapon.name().equals(test)) { return true; }
            }
            return false;
        }
    }

    public enum Room {
        KITCHEN,
        DINING_ROOM,
        LOUNGE,
        BATHROOM,
        STUDY,
        LIBRARY,
        BILLIARD_ROOM,
        CONSERVATORY,
        BALLROOM;

        public static boolean contains(String test) {
            for (Room room : Room.values()) {
                if (room.name().equals(test)) { return true; }
            }
            return false;
        }
    }

}

package games.terraformingmars.rules;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import utilities.Utils;

import java.util.Objects;

public class Award {
    public final String name;
    public final String counterID;
    public int claimed;

    public Award(String name, String counterID) {
        this.name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        this.counterID = counterID;
        this.claimed = -1;
    }

    public int checkProgress(TMGameState gs, int player) {
        String[] split = counterID.split("-");
        int sum = 0;
        for (String s: split) {
            // Try tile
            TMTypes.Tile t = Utils.searchEnum(TMTypes.Tile.class, s);
            if (t != null) {
                sum += gs.getTilesPlaced()[player].get(t).getValue();
            } else {
                // Try resource
                TMTypes.Resource r = Utils.searchEnum(TMTypes.Resource.class, s.replace("prod", ""));
                if (r != null) {
                    if (r == TMTypes.Resource.Card) {
                        sum += gs.getPlayerHands()[player].getSize();
                    } else {
                        if (s.contains("prod")) {
                            sum += gs.getPlayerProduction()[player].get(r).getValue();
                        } else {
                            sum += gs.getPlayerResources()[player].get(r).getValue();
                        }
                    }
                } else {
                    // Try tag
                    TMTypes.Tag tag = Utils.searchEnum(TMTypes.Tag.class, s);
                    if (tag != null) {
                        sum += gs.getPlayerCardsPlayedTags()[player].get(tag).getValue();
                    }
                }
            }
        }
        return sum;
    }

    public boolean claim(TMGameState gs) {
        if (claimed == -1) {
            claimed = gs.getCurrentPlayer();
            return true;
        }
        return false;
    }

    public boolean isClaimed() {
        return claimed != -1;
    }

    public Award copy() {
        Award copy = new Award(name, counterID);
        copy.claimed = claimed;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Award)) return false;
        Award award = (Award) o;
        return claimed == award.claimed &&
                Objects.equals(name, award.name) &&
                Objects.equals(counterID, award.counterID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, counterID, claimed);
    }
}


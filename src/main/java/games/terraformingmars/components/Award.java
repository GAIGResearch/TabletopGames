package games.terraformingmars.components;

import core.components.Token;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Objects;

public class Award extends Token {
    public final String counterID;
    public int claimed;

    public Award(String name, String counterID) {
        super(name);
        String[] splitN = name.split("-");
        this.componentName = "";
        for (int i = 0; i < splitN.length; i++) {
            this.componentName += splitN[i].substring(0, 1).toUpperCase() + splitN[i].substring(1).toLowerCase();
            if (i != splitN.length-1) this.componentName += " ";
        }
        this.counterID = counterID;
        this.claimed = -1;
    }

    protected Award(String name, String counterID, int componentID) {
        super(name, componentID);
        String[] splitN = name.split("-");
        this.componentName = "";
        for (int i = 0; i < splitN.length; i++) {
            this.componentName += splitN[i].substring(0, 1).toUpperCase() + splitN[i].substring(1).toLowerCase();
            if (i != splitN.length-1) this.componentName += " ";
        }
        this.counterID = counterID;
        this.claimed = -1;
    }

    public int checkProgress(TMGameState gs, int player) {
        if (player == -1) player = gs.getCurrentPlayer();
        int sum = 0;
        String[] split = counterID.split("-");
        for (String s : split) {
            // Try tile
            TMTypes.Tile t = Utils.searchEnum(TMTypes.Tile.class, s);
            if (t != null) {
                sum += gs.getPlayerTilesPlaced()[player].get(t).getValue();
                // TODO  if (counterID.contains("adjacent")), then tiles adjacent to this type
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
                    } else {
                        // todo:
                        // - "x-prod" (X productions at minimum value given)
                        // - card type (automated, active, event)
                        // - "card-X" (cards played with minimum cost X)
                        // - "bottom-X" (tiles placed in the bottom X rows of the map)
                        // - "tag" (number of tag types with at least 1 played)
                        // - "req" (number of cards played with requirements)
                        // - "resource" (number of resources on cards)
                    }
                }
            }
        }
        return sum;
    }

    public boolean claim(TMGameState gs, int player) {
        if (canClaim(gs, player)) {
            claimed = player;
            return true;
        }
        return false;
    }

    public boolean isClaimed() {
        return claimed != -1;
    }

    public boolean canClaim(TMGameState gs, int player) {
        return !gs.getnAwardsFunded().isMaximum() && claimed == -1;
    }

    public Award copy() {
        Award copy = new Award(componentName, counterID, componentID);
        copy.claimed = claimed;
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Award)) return false;
        if (!super.equals(o)) return false;
        Award award = (Award) o;
        return claimed == award.claimed && Objects.equals(counterID, award.counterID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), counterID, claimed);
    }

    @Override
    public String toString() {
        return "Award{" + counterID + "}";
    }
}


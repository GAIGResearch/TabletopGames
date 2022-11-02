package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import utilities.ImageIO;

import java.awt.*;
import java.util.Objects;

public class TilePlacedRequirement implements Requirement<TMGameState> {

    public final TMTypes.Tile tile;
    public final int threshold;
    public final boolean max;  // if true, value of counter must be <= threshold, if false >=
    public final boolean any;  // tiles placed by any player, or by the player who checks this

    public TilePlacedRequirement(TMTypes.Tile tile, int threshold, boolean max, boolean any) {
        this.tile = tile;
        this.threshold = threshold;
        this.max = max;
        this.any = any;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        int nPlaced = nPlaced(gs);
        if (max && nPlaced <= threshold) return true;
        return !max && nPlaced >= threshold;
    }

    private int nPlaced(TMGameState gs) {
        int player = gs.getCurrentPlayer();
        int nPlaced = 0;
        if (!any) {
            nPlaced = gs.getPlayerTilesPlaced()[player].get(tile).getValue();
        } else {
            for (int i = 0; i < gs.getNPlayers(); i++) {
                nPlaced = gs.getPlayerTilesPlaced()[i].get(tile).getValue();
            }
            if (gs.getNPlayers() == 1) {
                if (tile == TMTypes.Tile.City || tile == TMTypes.Tile.Greenery) {
                    nPlaced += ((TMGameParameters) gs.getGameParameters()).getSoloCities();
                }
            }
        }
        return nPlaced;
    }

    @Override
    public boolean isMax() {
        return max;
    }

    @Override
    public boolean appliesWhenAnyPlayer() {
        return any;
    }

    @Override
    public String getDisplayText(TMGameState gs) {
        return null;
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        int nPlaced = nPlaced(gs);
        return nPlaced + "/" + threshold + " " + tile + " tiles placed" + (!any? " by you" : "");
    }

    @Override
    public Image[] getDisplayImages() {
        return new Image[] {ImageIO.GetInstance().getImage(tile.getImagePath())};
    }

    @Override
    public TilePlacedRequirement copy() {
        return this;
    }

    @Override
    public String toString() {
        return "Tile Placed";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TilePlacedRequirement)) return false;
        TilePlacedRequirement that = (TilePlacedRequirement) o;
        return threshold == that.threshold && max == that.max && any == that.any && tile == that.tile;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tile, threshold, max, any);
    }
}

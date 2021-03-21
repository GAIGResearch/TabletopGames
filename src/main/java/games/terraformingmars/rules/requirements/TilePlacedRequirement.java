package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import utilities.ImageIO;

import java.awt.*;

public class TilePlacedRequirement implements Requirement<TMGameState> {

    public TMTypes.Tile tile;
    int threshold;
    public boolean max;  // if true, value of counter must be <= threshold, if false >=
    boolean any;  // tiles placed by any player, or by the player who checks this

    public TilePlacedRequirement(TMTypes.Tile tile, int threshold, boolean max, boolean any) {
        this.tile = tile;
        this.threshold = threshold;
        this.max = max;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        // TODO
//        if (max && c.getValue() - discount <= threshold) return true;
//        return !max && c.getValue() + discount >= threshold;
        return true;
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
    public Image[] getDisplayImages() {
        return new Image[] {ImageIO.GetInstance().getImage(tile.getImagePath())};
    }
}

package games.terraformingmars;

import utilities.Utils;
import utilities.Vector2D;

import java.awt.*;

public class TMTypes {

    // Odd r: (odd rows offset to the right)
    public static Vector2D[][] neighbor_directions = new Vector2D[][] {{new Vector2D(1, 0), new Vector2D(0, -1),
            new Vector2D(-1, -1), new Vector2D(-1, 0),
            new Vector2D(-1, 1), new Vector2D(0, 1)},
            {new Vector2D(1, 0), new Vector2D(1, -1),
                    new Vector2D(0, -1), new Vector2D(-1, 0),
                    new Vector2D(0, 1), new Vector2D(1, 1)}};

    public enum MapTileType {
        Ground (Color.lightGray),
        Ocean (Color.blue),
        City (Utils.stringToColor("purple"));

        Color outline;
        MapTileType(Color outline) {
            this.outline = outline;
        }

        public Color getOutlineColor() {
            return outline;
        }
    }

    public enum Tile {
        Ocean ("data/terraformingmars/images/tiles/ocean.png"),
        Greenery ("data/terraformingmars/images/tiles/greenery_no_O2.png"),
        City ("data/terraformingmars/images/tiles/city.png"),
        CommercialBuilding ("data/terraformingmars/images/tiles/special.png"),
        NuclearExplosion ("data/terraformingmars/images/tiles/special.png"),
        IndustrialBuilding ("data/terraformingmars/images/tiles/special.png"),
        Mine ("data/terraformingmars/images/tiles/special.png"),
        Moonhole ("data/terraformingmars/images/tiles/special.png"),
        Nature ("data/terraformingmars/images/tiles/special.png"),
        Park ("data/terraformingmars/images/tiles/special.png"),
        Restricted ("data/terraformingmars/images/tiles/special.png"),
        Volcano ("data/terraformingmars/images/tiles/special.png");

        String imagePath;

        Tile(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getImagePath() {
            return imagePath;
        }
    }

    public enum Resource {
        MegaCredit("data/terraformingmars/images/megacredits/megacredit.png", true),
        Steel("data/terraformingmars/images/resources/steel.png", true),
        Titanium("data/terraformingmars/images/resources/titanium.png", true),
        Plant("data/terraformingmars/images/resources/plant.png", true),
        Energy("data/terraformingmars/images/resources/power.png", true),
        Heat("data/terraformingmars/images/resources/heat.png", true),
        Card("data/terraformingmars/images/resources/card.png", false),
        TR("data/terraformingmars/images/resources/TR.png", false);

        String imagePath;
        boolean playerBoardRes;
        static int nPlayerBoardRes = -1;

        Resource(String imagePath, boolean playerBoardRes) {
            this.imagePath = imagePath;
            this.playerBoardRes = playerBoardRes;
        }

        public String getImagePath() {
            return imagePath;
        }

        public boolean isPlayerBoardRes() {
            return playerBoardRes;
        }

        public static int nPlayerBoardRes() {
            if (nPlayerBoardRes == -1) {
                nPlayerBoardRes = 0;
                for (Resource res : values()) {
                    if (res.isPlayerBoardRes()) nPlayerBoardRes++;
                }
            }
            return nPlayerBoardRes;
        }
    }

    public enum Tag {
        Plant("data/terraformingmars/images/tags/plant.png"),
        Microbe("data/terraformingmars/images/tags/microbe.png"),
        Animal("data/terraformingmars/images/tags/animal.png"),
        Science("data/terraformingmars/images/tags/science.png"),
        Earth("data/terraformingmars/images/tags/earth.png"),
        Space("data/terraformingmars/images/tags/space.png"),
        Event("data/terraformingmars/images/tags/event.png"),
        Building("data/terraformingmars/images/tags/building.png"),
        Power("data/terraformingmars/images/tags/power.png"),
        Jovian("data/terraformingmars/images/tags/jovian.png"),
        City("data/terraformingmars/images/tags/city.png"),
        Venus("data/terraformingmars/images/tags/venus.png"),
        Wild("data/terraformingmars/images/tags/wild.png");

        String imagePath;

        Tag(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getImagePath() {
            return imagePath;
        }
    }

    public enum TokenType {
        Microbe("data/terraformingmars/images/tags/microbe.png"),
        Animal("data/terraformingmars/images/tags/animal.png"),
        Science("data/terraformingmars/images/tags/science.png");

        String imagePath;

        TokenType(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getImagePath() {
            return imagePath;
        }
    }

    public enum CardType {
        Automated("data/terraformingmars/images/cards/card-automated.png", true, Color.green),
        Active("data/terraformingmars/images/cards/card-active.png", true, Color.cyan),
        Event("data/terraformingmars/images/cards/card-event.png", true, Color.orange),
        Corporation("data/terraformingmars/images/cards/corp-card-bg.png", false, Color.gray),
        Prelude("data/terraformingmars/images/cards/proj-card-bg.png", false, Color.pink),
        Colony("data/terraformingmars/images/cards/proj-card-bg.png", false, Color.lightGray),
        GlobalEvent("data/terraformingmars/images/cards/proj-card-bg.png", false, Color.blue);

        String imagePath;
        Color color;
        boolean isPlayableStandard;

        CardType(String imagePath, boolean isPlayableStandard, Color color) {
            this.imagePath = imagePath;
            this.isPlayableStandard = isPlayableStandard;
            this.color = color;
        }

        public String getImagePath() {
            return imagePath;
        }

        public boolean isPlayableStandard() {
            return isPlayableStandard;
        }

        public Color getColor() {
            return color;
        }
    }

    public enum GlobalParameter {
        Oxygen ("data/terraformingmars/images/global-parameters/oxygen.png", Color.lightGray),
        Temperature ("data/terraformingmars/images/global-parameters/temperature.png", Color.white),
        OceanTiles ("data/terraformingmars/images/tiles/ocean.png", Color.yellow),
        Venus ("data/terraformingmars/images/global-parameters/venus.png", Color.white);

        String imagePath;
        Color color;

        GlobalParameter(String imagePath, Color color) {
            this.imagePath = imagePath;
            this.color = color;
        }

        public String getImagePath() {
            return imagePath;
        }

        public Color getColor() {
            return color;
        }
    }

}

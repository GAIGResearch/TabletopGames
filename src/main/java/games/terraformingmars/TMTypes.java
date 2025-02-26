package games.terraformingmars;

import core.components.BoardNode;
import core.components.Counter;
import core.components.Deck;
import core.components.GridBoard;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.*;
import games.terraformingmars.rules.effects.Bonus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils;
import utilities.Vector2D;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static games.terraformingmars.components.TMMapTile.parseMapTile;

public class TMTypes {

    // Odd r: (odd rows offset to the right)
    public static Vector2D[][] neighbor_directions = new Vector2D[][] {{new Vector2D(1, 0), new Vector2D(0, -1),
            new Vector2D(-1, -1), new Vector2D(-1, 0),
            new Vector2D(-1, 1), new Vector2D(0, 1)},
            {new Vector2D(1, 0), new Vector2D(1, -1),
                    new Vector2D(0, -1), new Vector2D(-1, 0),
                    new Vector2D(0, 1), new Vector2D(1, 1)}};

    // Enums

    public enum ActionType {
        PlayCard,
        StandardProject,
        ClaimMilestone,
        FundAward,
        ActiveAction,
        BasicResourceAction,
        BuyProject  // TODO ignore in GUI
    }

    public enum BasicResourceAction {
        HeatToTemp,
        PlantToGreenery
    }

    public enum StandardProject {
        SellPatents,
        PowerPlant,
        Asteroid,
        Aquifer,
        Greenery,
        City,
        AirScraping
    }

    public enum MapTileType {
        Ground (Color.lightGray),
        Ocean (Color.blue),
        City (Utils.stringToColor("purple")),
        Volcanic (Color.red);

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
        CommercialBuilding ("data/terraformingmars/images/tiles/CommercialBuilding.png"),
        NuclearExplosion ("data/terraformingmars/images/tiles/NuclearExplosion.png"),
        IndustrialBuilding ("data/terraformingmars/images/tiles/IndustrialBuilding.png"),
        Mine ("data/terraformingmars/images/tiles/mine.png"),
        Moonhole ("data/terraformingmars/images/tiles/Moonhole.png"),
        Nature ("data/terraformingmars/images/tiles/Nature.png"),
        Park ("data/terraformingmars/images/tiles/park.png"),
        Restricted ("data/terraformingmars/images/tiles/Restricted.png"),
        Volcano ("data/terraformingmars/images/tiles/Volcano.png");

        String imagePath;

        Tile(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getImagePath() {
            return imagePath;
        }

        public MapTileType getRegularLegalTileType() {
            if (this == Ocean) return MapTileType.Ocean;
            return MapTileType.Ground;
        }

        public boolean canBeOwned() {
            return this != Ocean;
        }

        public GlobalParameter getGlobalParameterToIncrease() {
            if (this == Ocean) return GlobalParameter.OceanTiles;
            if (this == Greenery) return GlobalParameter.Oxygen;
            return null;
        }
    }

    public enum Resource {
        MegaCredit("data/terraformingmars/images/megacredits/megacredit.png", true, false),
        Steel("data/terraformingmars/images/resources/steel.png", true, false),
        Titanium("data/terraformingmars/images/resources/titanium.png", true, false),
        Plant("data/terraformingmars/images/resources/plant.png", true, false),
        Energy("data/terraformingmars/images/resources/power.png", true, false),
        Heat("data/terraformingmars/images/resources/heat.png", true, false),
        Card("data/terraformingmars/images/resources/card.png", false, false),
        TR("data/terraformingmars/images/resources/TR.png", false, false),
        Microbe("data/terraformingmars/images/resources/microbe.png", false, true),
        Animal("data/terraformingmars/images/resources/animal.png", false, true),
        Science("data/terraformingmars/images/resources/science.png", false, true),
        Fighter("data/terraformingmars/images/resources/fighter.png", false, true),
        Floater("data/terraformingmars/images/resources/floater.png", false, true);

        String imagePath;
        boolean playerBoardRes;
        boolean canGoOnCard;
        static int nPlayerBoardRes = -1;

        Resource(String imagePath, boolean playerBoardRes, boolean canGoOnCard) {
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

        public boolean canGoOnCard() {
            return canGoOnCard;
        }

        public static Resource[] getPlayerBoardResources() {
            ArrayList<Resource> res = new ArrayList<>();
            for (Resource r: values()) {
                if (r.isPlayerBoardRes()) res.add(r);
            }
            return res.toArray(new Resource[0]);
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
        Oxygen ("data/terraformingmars/images/global-parameters/oxygen.png", Color.lightGray, true, "O2"),
        Temperature ("data/terraformingmars/images/global-parameters/temperature.png", Color.white, true, "°C"),
        OceanTiles ("data/terraformingmars/images/tiles/ocean.png", Color.yellow, true, "Ocean"),
        Venus("data/terraformingmars/images/global-parameters/venus.png", Color.blue, false, "Venus");

        String imagePath;
        String shortString;
        Color color;
        boolean countsForEndGame;

        GlobalParameter(String imagePath, Color color, boolean countsForEndGame, String shortString) {
            this.imagePath = imagePath;
            this.color = color;
            this.countsForEndGame = countsForEndGame;
            this.shortString = shortString;
        }

        public String getImagePath() {
            return imagePath;
        }

        public Color getColor() {
            return color;
        }

        public boolean countsForEndGame() {
            return countsForEndGame;
        }

        public String getShortString() {
            return shortString;
        }

        public static ArrayList<GlobalParameter> getDrawOrder(TMGameParameters params) {
            ArrayList<GlobalParameter> order = new ArrayList<>();
            if (params.expansions.contains(Expansion.Venus)) {
                order.add(Venus);
            }
            order.add(Temperature);
            order.add(Oxygen);
            order.add(OceanTiles);
            return order;
        }
    }

    public enum Expansion {
        Base,
        CorporateEra,
        Prelude,
        Venus,
        Turmoil,
        Colonies,
        Promo,
        Hellas,
        Elysium;

        public String getBoardPath() {
            return "data/terraformingmars/boards/" + this.name().toLowerCase() + ".json";
        }
        public String getCorpCardsPath() {
            return "data/terraformingmars/corporationCards/" + this.name().toLowerCase() + ".json";
        }
        public String getProjectCardsPath() {
            return "data/terraformingmars/projectCards/" + this.name().toLowerCase() + ".json";
        }
        public String getOtherCardsPath() {
            return "data/terraformingmars/otherCards/" + this.name().toLowerCase() + ".json";
        }

        /* custom loading info from json */

        public void loadBoard(GridBoard board, HashSet<TMMapTile> extraTiles, HashSet<Bonus> bonuses,
                              HashSet<Milestone> milestones, HashSet<Award> awards, HashMap<GlobalParameter, games.terraformingmars.components.GlobalParameter> globalParameters) {
            JSONParser jsonParser = new JSONParser();
            try (FileReader reader = new FileReader(getBoardPath())) {
                JSONObject data = (JSONObject) jsonParser.parse(reader);

                // Process main map
                if (data.get("board") != null) {
                    JSONArray b = (JSONArray) data.get("board");
                    int y = 0;
                    for (Object g : b) {
                        JSONArray row = (JSONArray) g;
                        int x = 0;
                        for (Object o1 : row) {
                            board.setElement(x, y, parseMapTile((String) o1, x, y));
                            x++;
                        }
                        y++;
                    }
                }

                // Process extra tiles not on regular board
                if (data.get("extra") != null) {
                    JSONArray extra = (JSONArray) data.get("extra");
                    for (Object o : extra) {
                        extraTiles.add(parseMapTile((String) o));
                    }
                }

                // Process milestones and awards
                if (data.get("milestones") != null) {
                    JSONArray milestonesStr = (JSONArray) data.get("milestones");
                    for (Object o : milestonesStr) {
                        String[] split = ((String) o).split(":");
                        milestones.add(new Milestone(split[0], Integer.parseInt(split[2]), split[1]));
                    }
                }
                if (data.get("awards") != null) {
                    JSONArray awardsStr = (JSONArray) data.get("awards");
                    for (Object o : awardsStr) {
                        String[] split = ((String) o).split(":");
                        awards.add(new Award(split[0], split[1]));
                    }
                }

                // Process global parameters enabled
                if (data.get("globalParameters") != null) {
                    JSONArray gps = (JSONArray) data.get("globalParameters");
                    for (Object o : gps) {
                        JSONObject gp = (JSONObject) o;
                        GlobalParameter p = GlobalParameter.valueOf((String) gp.get("name"));
                        JSONArray valuesJSON = (JSONArray) gp.get("range");
                        int[] values = new int[valuesJSON.size()];
                        for (int i = 0; i < valuesJSON.size(); i++) {
                            values[i] = (int)(long)valuesJSON.get(i);
                        }
                        globalParameters.put(p, new games.terraformingmars.components.GlobalParameter(values, p.name()));

                        // Process bonuses for this game when counters reach specific points
                        if (gp.get("bonus") != null) {
                            JSONArray bonus = (JSONArray) gp.get("bonus");
                            for (Object o2 : bonus) {
                                JSONObject b = (JSONObject) o2;
                                String effectString = (String) b.get("effect");
                                int threshold = (int)(long) b.get("threshold");
                                bonuses.add(new Bonus(p, threshold, TMAction.parseActionOnCard(effectString, null, true)));
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        public void loadProjectCards(Deck<TMCard> deck) {
            loadCards(deck, getProjectCardsPath());
        }

        public void loadCorpCards(Deck<TMCard> deck) {
            loadCards(deck, getCorpCardsPath());
        }

        private void loadCards(Deck<TMCard> deck, String path) {
            JSONParser jsonParser = new JSONParser();
            try (FileReader reader = new FileReader(path)) {
                JSONArray data = (JSONArray) jsonParser.parse(reader);
                for (Object o: data) {
                    TMCard card;
                    if (deck.getComponentName().equalsIgnoreCase("corporations")) {
                        card = TMCard.loadCorporation((JSONObject)o);
                    } else {
//                        card = TMCard.loadCardHTML((JSONObject) o);
                        card = TMCard.loadCardJSON((JSONObject) o);
                    }
                    deck.add(card);
                }
            } catch (IOException ignored) {
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

}

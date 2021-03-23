package games.terraformingmars.components;

import core.components.Card;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.*;
import games.terraformingmars.rules.effects.Effect;
import games.terraformingmars.rules.effects.PayForActionEffect;
import games.terraformingmars.rules.effects.PlaceTileEffect;
import games.terraformingmars.rules.effects.PlayCardEffect;
import games.terraformingmars.rules.requirements.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TMCard extends Card {
    public int number;
    public TMTypes.CardType cardType;
    public int cost;
    public HashSet<Requirement<TMGameState>> requirements;
    public TMTypes.Tag[] tags;

    public HashMap<Requirement, Integer> discountEffects;
    public HashSet<TMGameState.ResourceMapping> resourceMappings;

    public Effect[] persistingEffects;
    public TMAction firstAction;  // first action for the player is already decided to be this
    public TMAction[] actions;  // new actions available to the player
    public TMAction[] immediateEffects; // effect of this card, executed immediately

    public int mapTileIDTilePlaced;  // Location where tile was placed, ID of grid cell

    public double nPoints;
    public TMTypes.Resource pointsResource;  // Type of resource placed on this card earning points, number of points will be nPoints * nResources
    public Integer pointsThreshold;  // Points awarded if more than this resources on card, not multiplied
    public TMTypes.Tag pointsTag;  // Type of tags played earning points
    public TMTypes.Tile pointsTile;  // Type of tiles placed earning points
    public boolean pointsTileAdjacent;  // If true, only count tiles of type adjacent to tile placed by card

    public TMTypes.Resource resourceOnCard; // TODO
    public HashMap<TMTypes.Resource, Integer> resourcesOnCard;  // One count for each type of token

    public TMCard() {
        resourcesOnCard = new HashMap<>();
        for (TMTypes.Resource t: TMTypes.Resource.values()) {
            if (t.canGoOnCard()) {
                resourcesOnCard.put(t, 0);
            }
        }
        tags = new TMTypes.Tag[0];
        actions = new TMAction[0];
        immediateEffects = new TMAction[0];
        persistingEffects = new Effect[0];
        discountEffects = new HashMap<>();
        resourceMappings = new HashSet<>();
        requirements = new HashSet<>();
    }

    public boolean meetsRequirements(TMGameState gs) {
        for (Requirement r: requirements) {
            if (!r.testCondition(gs)) return false;
        }
        return true;
    }

    public boolean shouldSaveCard() {
        return pointsResource != null || pointsTag != null || pointsTile != null;
    }

    public static TMCard loadCorporation(JSONObject cardDef) {
        TMCard card = new TMCard();
        card.cardType = TMTypes.CardType.Corporation;
        card.number = (int)(long)cardDef.get("id");
        card.setComponentName((String)cardDef.get("name"));

        JSONArray start = (JSONArray) cardDef.get("start");
        String startResources = (String) start.get(0);
        String[] split = startResources.split(",");
        ArrayList<TMAction> immediateEffects = new ArrayList<>();
        for (String s: split) {
            s = s.trim();
            String[] split2 = s.split(" ");
            // First is amount
            int amount = Integer.parseInt(split2[0]);
            // Second is what resource
            String resString = split2[1].split("prod")[0];
            TMTypes.Resource res = Utils.searchEnum(TMTypes.Resource.class, resString);
            immediateEffects.add(new PlaceholderModifyCounter(-1, amount, res, split2[1].contains("prod"), true));
        }
        for (int i = 1; i < start.size(); i++) {
            JSONObject other = (JSONObject) start.get(i);
            String type = (String) other.get("type");
            if (type.equalsIgnoreCase("first")) {
                // First action in action phase for the player is decided, not free
                String action = (String) other.get("action");
                if (action.equalsIgnoreCase("resourcetransaction")) {
                    card.firstAction = new ResourceTransaction(-1, TMTypes.Resource.valueOf((String) other.get("resource")), (int)(long)other.get("amount"), false);
                } else if (action.equalsIgnoreCase("placetile")) {
                    TMTypes.Tile t = TMTypes.Tile.valueOf((String) other.get("tile"));
                    card.firstAction = new PlaceTile(-1, t, t.getRegularLegalTileType(), false);
                }
            }
        }

        if (cardDef.get("tags") != null) {
            JSONArray ts = (JSONArray) cardDef.get("tags");
            card.tags = new TMTypes.Tag[ts.size()];
            int i = 0;
            for (Object o: ts) {
                TMTypes.Tag t = Utils.searchEnum(TMTypes.Tag.class, (String)o);
                card.tags[i] = t;
                i++;
            }
        }

        JSONArray effects = (JSONArray) cardDef.get("effect");
        ArrayList<TMAction> actions = new ArrayList<>();
        ArrayList<Effect> persistingEffects = new ArrayList<>();
        for (Object o: effects) {
            JSONObject effect = (JSONObject) o;
            String type = (String) effect.get("type");
            if (type.equalsIgnoreCase("action")) {
                // Parse actions
                String[] action = ((String) effect.get("action")).split("-");
                String[] costStr = ((String) effect.get("cost")).split("/");
                TMTypes.Resource costResource = TMTypes.Resource.valueOf(costStr[0]);
                int cost = Integer.parseInt(costStr[1]);
                if (action[0].equalsIgnoreCase("placetile")) {
                    TMTypes.Tile t = TMTypes.Tile.valueOf(action[1]);
                    TMAction a = new PayForAction(TMTypes.ActionType.ActiveAction, -1, new PlaceTile(-1, t, t.getRegularLegalTileType(), false),
                            costResource, cost, -1);
                    actions.add(a);
                } else if (action[0].equalsIgnoreCase("resourcetransaction")) {
                    TMTypes.Resource r = TMTypes.Resource.valueOf(action[1]);
                    int amount = Integer.parseInt(action[2]);
                    Requirement req = null;
                    if (effect.get("if") != null) {
                        // parse requirement
                        String reqStr = (String) effect.get("if");
                        if (reqStr.contains("incgen")) {
                            req = new ResourceIncGenRequirement(TMTypes.Resource.valueOf(reqStr.split("-")[1]));
                        }
                    }
                    TMAction a = new PayForAction(TMTypes.ActionType.ActiveAction, -1, new ResourceTransaction(-1, r, amount, false), costResource, -cost, -1, req);
                    actions.add(a);
                }
            } else if (type.equalsIgnoreCase("discount")) {
                // Parse discounts
                int amount = (int)(long)effect.get("amount");
                Requirement r;
                if (effect.get("counter") != null) {
                    // A discount for CounterRequirement
                    for (Object o2: (JSONArray)effect.get("counter")) {
                        r = new CounterRequirement((String)o2, -1, true);
                        if (card.discountEffects.containsKey(r)) {
                            card.discountEffects.put(r, card.discountEffects.get(r) + amount);
                        } else {
                            card.discountEffects.put(r, amount);
                        }
                    }
                } else if (effect.get("tag") != null) {
                    // A discount for tag requirements
                    TMTypes.Tag t = TMTypes.Tag.valueOf((String) effect.get("tag"));
                    r = new TagsPlayedRequirement(new TMTypes.Tag[]{t}, null);
                    if (card.discountEffects.containsKey(r)) {
                        card.discountEffects.put(r, card.discountEffects.get(r) + amount);
                    } else {
                        card.discountEffects.put(r, amount);
                    }
                } else if (effect.get("standardproject") != null) {
                    // A discount for buying standard projects
                    TMTypes.StandardProject sp = TMTypes.StandardProject.valueOf((String) effect.get("standardproject"));
                    r = new ActionTypeRequirement(TMTypes.ActionType.StandardProject, sp);
                    if (card.discountEffects.containsKey(r)) {
                        card.discountEffects.put(r, card.discountEffects.get(r) + amount);
                    } else {
                        card.discountEffects.put(r, amount);
                    }
                }
            } else if (type.equalsIgnoreCase("resourcemapping")) {
                TMTypes.Resource from = TMTypes.Resource.valueOf((String)effect.get("from"));
                TMTypes.Resource to = TMTypes.Resource.valueOf((String)effect.get("to"));
                double rate = (double) effect.get("rate");
                card.resourceMappings.add(new TMGameState.ResourceMapping(from, to, rate,null));
            } else if (type.equalsIgnoreCase("effect")) {
                String condition = (String)effect.get("if");
                String actionTypeCondition = condition.split("\\(")[0];
                String result = (String)effect.get("then");
                if (actionTypeCondition.equalsIgnoreCase("placetile")) {
                    // Place tile effect
                    String cond = condition.split("\\(")[1].replace(")", "");
                    String[] split2 = cond.split(",");
                    TMTypes.Tile tile = null;
                    TMTypes.Resource[] resourcesGained = null;
                    if (split2.length > 1) {
                        tile = TMTypes.Tile.valueOf(split2[0]);
                    } else {
                        if (split2[0].contains("gain")) {
                            String[] split3 = split2[0].replace("gain ", "").split("/");
                            resourcesGained = new TMTypes.Resource[split3.length];
                            for (int i = 0; i < split3.length; i++) {
                                resourcesGained[i] = TMTypes.Resource.valueOf(split3[i]);
                            }
                        }
                    }
                    persistingEffects.add(new PlaceTileEffect(!condition.contains("any"), result, condition.contains("onMars"), tile, resourcesGained));
                } else if (actionTypeCondition.equalsIgnoreCase("playcard")) {
                    // Play card effect
                    String[] cond = condition.split("\\(")[1].replace(")", "").split("-");
                    persistingEffects.add(new PlayCardEffect(!condition.contains("any"), result, TMTypes.Tag.valueOf(cond[1])));
                } else if (actionTypeCondition.equalsIgnoreCase("payforaction")) {
                    // pay for action effect
                    int minCost = Integer.parseInt(condition.split("\\(")[1].replace(")", ""));
                    persistingEffects.add(new PayForActionEffect(!condition.contains("any"), result, minCost));
                }
            }
        }

        card.immediateEffects = immediateEffects.toArray(new TMAction[0]);
        card.actions = actions.toArray(new TMAction[0]);
        card.persistingEffects = persistingEffects.toArray(new Effect[0]);

        return card;
    }

    public static TMCard loadCardHTML(JSONObject cardDef) {
        TMCard card = new TMCard();
        String classDef = (String)cardDef.get("@class");
        card.cardType = Utils.searchEnum(TMTypes.CardType.class, classDef.split(" ")[1].trim());
        JSONArray div1 = (JSONArray) cardDef.get("div");
        ArrayList<TMTypes.Tag> tempTags = new ArrayList<>();

        ArrayList<TMAction> immediateEffects = new ArrayList<>();
        for (Object o: div1) {
            JSONObject ob = (JSONObject)o;
            String info = (String)ob.get("@class");
            if (info.contains("title")) {
                // Card type
                String[] split = info.split("-");
                card.cardType = Utils.searchEnum(TMTypes.CardType.class, split[split.length-1].trim());
                // Name of card
                card.setComponentName((String)ob.get("#text"));
            } else if (info.contains("price")) {
                // Cost of card
                card.cost = Integer.parseInt((String)ob.get("#text"));
            } else if (info.contains("tag")) {
                // A tag
                TMTypes.Tag tag = Utils.searchEnum(TMTypes.Tag.class, info.split("-")[1].trim());
                if (tag != null) {
                    tempTags.add(tag);
                }
            } else if (info.contains("number")) {
                // Card number
                card.number = Integer.parseInt(((String)ob.get("#text")).replaceAll("[^\\d.]",""));
            } else if (info.contains("content")) {
                if (ob.get("div") instanceof JSONArray) {
                    JSONArray div2 = (JSONArray) ob.get("div");
                    for (Object o2 : div2) {
                        JSONObject ob2 = (JSONObject) o2;
                        String info2 = (String) ob2.get("@class");
                        if (info2 != null && info2.contains("points")) {
                            // Points for this card
                            String ps = (String) ob2.get("#text");
                            card.nPoints = Double.parseDouble(ps.split("/")[0]);
                        } else if (info2 != null && info2.contains("requirements")) {
                            String[] reqStr = ((String) ob2.get("#text")).split("\\.");
                            for (String s: reqStr) {
                                s = s.trim();
                                if (s.contains("tags")) {
                                    // Tag requirement
                                    s = s.replace("tags","").trim();
                                    String[] split = s.split(" ");
                                    HashMap<TMTypes.Tag, Integer> tagCount = new HashMap<>();
                                    for (String s2: split) {
                                        TMTypes.Tag t = TMTypes.Tag.valueOf(s2);
                                        if (tagCount.containsKey(t)) {
                                            tagCount.put(t, tagCount.get(t) + 1);
                                        } else {
                                            tagCount.put(t, 1);
                                        }
                                    }
                                    TMTypes.Tag[] tags = new TMTypes.Tag[tagCount.size()];
                                    int[] min = new int[tagCount.size()];
                                    int i = 0;
                                    for (TMTypes.Tag t: tagCount.keySet()) {
                                        tags[i] = t;
                                        min[i] = tagCount.get(t);
                                        i++;
                                    }
                                    Requirement r = new TagsPlayedRequirement(tags, min);
                                    card.requirements.add(r);
                                } else if (s.contains("tile")) {
                                    // Tile count placed requirement TODO
//                                    Requirement r = new TilePlacedRequirement()
                                } else {
                                    // Counter requirement
                                    boolean max = s.contains("max");
                                    s = s.replace("max", "").trim();
                                    String[] split = s.split(" ");
                                    // first is threshold, second is counter code
                                    Requirement r = new CounterRequirement(split[1], Integer.parseInt(split[0]), max);
                                }
                            }
                        } else if (info2 != null && info2.contains("description")) {
                            String ps = (String) ob2.get("#text");
                            String[] split = ps.split("\\.");  // Dot separates multiple effects
                            for (String s: split) {
                                if (s.contains("Requires") || s.contains("must"))
                                    continue;  // Already parsed requirements
                                if (s.contains("Action")) continue;  // TODO if add resource action, mark card.resourceOnCard property
                                if (s.contains("Effect")) continue;  // TODO
                                s = s.trim();
                                if (s.contains("VP")) {
                                    // This card has special rules for awarding victory points
                                    String[] pointCondition = s.split(" ");
                                    int nVP = Integer.parseInt(pointCondition[0]);
                                    int nOther = Integer.parseInt(pointCondition[3]);
                                    card.nPoints = 1.*nVP / nOther;
                                    String other = pointCondition[4];
                                    if (s.contains("tag")) {
                                        // Find tag that earns points on this card
                                        card.pointsTag = TMTypes.Tag.valueOf(other);
                                    } else {
                                        // Maybe a resource?
                                        TMTypes.Resource r = Utils.searchEnum(TMTypes.Resource.class, other);
                                        if (r != null) {
                                            card.pointsResource = r;
                                            card.resourceOnCard = r;
                                        } else {
                                            // A tile
                                            card.pointsTile = TMTypes.Tile.valueOf(other);
                                            if (pointCondition.length > 5 && pointCondition[5].equalsIgnoreCase("adjacent")) {
                                                // Only count tiles adjacent to tile placed by card
                                                card.pointsTileAdjacent = true;
                                            }
                                        }
                                    }
                                    if (pointCondition[2].equalsIgnoreCase("if")) {
                                        card.pointsThreshold = nOther;
                                    }
                                } else {
                                    String[] orSplit = s.split(" or ");
                                    if (orSplit.length == 1) {
                                        TMAction a = TMAction.parseAction(s, true, card.getComponentID()).a;
                                        if (a != null) {
                                            immediateEffects.add(a);
                                            if (a instanceof PlaceTile) {
                                                ((PlaceTile) a).cardID = card.getComponentID();
                                            }
                                        } else {
                                            int b = 0;  // action didn't parse, put a breakpoint here to see it
                                        }
                                    } else {
                                        // Action choice
                                        TMAction[] actionChoice = new TMAction[orSplit.length];
                                        int i = 0;
                                        for (String s2 : orSplit) {
                                            s2 = s2.trim();
                                            String[] andSplit = s2.split(" and ");
                                            if (andSplit.length == 1) {
                                                TMAction a = TMAction.parseAction(s2, true, card.getComponentID()).a;
                                                if (a != null) {
                                                    actionChoice[i] = a;
                                                }
                                            } else {
                                                // Compound action
                                                TMAction[] compound = new TMAction[andSplit.length];
                                                int j = 0;
                                                for (String s3 : andSplit) {
                                                    s3 = s3.trim();
                                                    TMAction a = TMAction.parseAction(s3, true, card.getComponentID()).a;
                                                    if (a != null) {
                                                        compound[j] = a;
                                                    }
                                                    j++;
                                                }
                                                actionChoice[i] = new CompoundAction(-1, compound, true);
                                            }
                                            i++;
                                        }
                                        for (int k = 0; k < actionChoice.length; k++) {
                                            if (actionChoice[k] == null) {
                                                int p = 0;
                                            }
                                        }
                                        immediateEffects.add(new ActionChoice(-1, actionChoice, true));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        card.immediateEffects = immediateEffects.toArray(new TMAction[0]);
        card.tags = tempTags.toArray(new TMTypes.Tag[0]);

        return card;
    }
}

package games.terraformingmars.components;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.components.Card;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.*;
import games.terraformingmars.rules.Discount;
import games.terraformingmars.rules.effects.*;
import games.terraformingmars.rules.requirements.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.SimpleDeserializer;
import utilities.Utils;

import java.util.*;

import static games.terraformingmars.actions.TMAction.parseActionOnCard;

public class TMCard extends Card {
    public int number;
    public String annotation;
    public TMTypes.CardType cardType;
    public int cost;
    public HashSet<Requirement<TMGameState>> requirements;
    public TMTypes.Tag[] tags;

    public LinkedList<Discount> discountEffects;
    public HashSet<TMGameState.ResourceMapping> resourceMappings;

    public Effect[] persistingEffects;
    public TMAction firstAction;  // first action for the player is already decided to be this
    transient public boolean firstActionExecuted;
    transient public boolean actionPlayed;
    public TMAction[] actions;  // new actions available to the player
    public TMAction[] immediateEffects; // effect of this card, executed immediately

    transient public int mapTileIDTilePlaced = -1;  // Location where tile was placed, ID of grid cell

    public double nPoints;
    public TMTypes.Resource pointsResource;  // Type of resource placed on this card earning points, number of points will be nPoints * nResources
    public Integer pointsThreshold;  // Points awarded if more than this resources on card, not multiplied
    public TMTypes.Tag pointsTag;  // Type of tags played earning points
    public TMTypes.Tile pointsTile;  // Type of tiles placed earning points
    public boolean pointsTileAdjacent;  // If true, only count tiles of type adjacent to tile placed by card

    public TMTypes.Resource resourceOnCard;
    transient public int nResourcesOnCard;  // One count for each type of token
    public boolean canResourcesBeRemoved = true;

    public TMCard() {
        nResourcesOnCard = 0;
        tags = new TMTypes.Tag[0];
        actions = new TMAction[0];
        immediateEffects = new TMAction[0];
        persistingEffects = new Effect[0];
        discountEffects = new LinkedList<>();
        resourceMappings = new HashSet<>();
        requirements = new HashSet<>();
    }

    private TMCard(String name, int componentID) {
        super(name, componentID);
    }

    public boolean meetsRequirements(TMGameState gs) {
        for (Requirement r: requirements) {
            if (!r.testCondition(gs)) return false;
        }
        return true;
    }

    public boolean shouldSaveCard() {
        return pointsResource != null || pointsTag != null || pointsTile != null || resourceOnCard != null || persistingEffects.length > 0 || actions.length > 0 || discountEffects.size() > 0 || resourceMappings.size() > 0;
    }

    public static TMCard loadCorporation(JSONObject cardDef) {
        TMCard card = new TMCard();
        card.cardType = TMTypes.CardType.Corporation;
        card.number = (int)(long)cardDef.get("id");
        card.annotation = (String) cardDef.get("annotation");
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
            immediateEffects.add(new ModifyPlayerResource(-1, amount, res, split2[1].contains("prod")));
        }
        for (int i = 1; i < start.size(); i++) {
            JSONObject other = (JSONObject) start.get(i);
            String type = (String) other.get("type");
            if (type.equalsIgnoreCase("first")) {
                // First action in action phase for the player is decided, not free
                String action = (String) other.get("action");
                card.firstAction = TMAction.parseActionOnCard(action, card, false);
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
                String action = (String) effect.get("action");
                String[] costStr;
                TMTypes.Resource costResource = null;
                int cost = 0;
                if (effect.get("cost") != null)  {
                    costStr = ((String) effect.get("cost")).split("/");
                    costResource = TMTypes.Resource.valueOf(costStr[0]);
                    cost = Integer.parseInt(costStr[1]);
                }
                TMAction a = TMAction.parseActionOnCard(action, card, false);
                if (a != null) {
                    actions.add(a);
                    a.actionType = TMTypes.ActionType.ActiveAction;
                    if (cost != 0) {
                        a.setActionCost(costResource, cost, -1);
                    }
                    a.setCardID(card.getComponentID());
                    if (effect.get("if") != null) {
                        // Requirement
                        String reqStr = (String) effect.get("if");
                        if (reqStr.contains("incgen")) {
                            TMTypes.Resource res = Utils.searchEnum(TMTypes.Resource.class, reqStr.split("-")[1]);
                            if (res != null) {
                                a.requirements.add(new ResourceIncGenRequirement(res));
                            }
                        }
                    }
                }
            } else if (type.equalsIgnoreCase("discount")) {
                // Parse discounts
                int amount = (int)(long)effect.get("amount");
                Requirement r;
                if (effect.get("counter") != null) {
                    // A discount for CounterRequirement
                    for (Object o2: (JSONArray)effect.get("counter")) {
                        r = new CounterRequirement((String)o2, -1, true);
                        if (card.discountEffects.contains(r)) {
                            int disc = card.discountEffects.get(card.discountEffects.indexOf(r)).b;
                            card.discountEffects.add(new Discount(r, disc + amount));
                        } else {
                            card.discountEffects.add(new Discount(r, amount));
                        }
                    }
                } else if (effect.get("tag") != null) {
                    // A discount for tag requirements
                    TMTypes.Tag t = TMTypes.Tag.valueOf((String) effect.get("tag"));
                    r = new TagsPlayedRequirement(new TMTypes.Tag[]{t}, new int[1]);
                    if (card.discountEffects.contains(r)) {
                        int disc = card.discountEffects.get(card.discountEffects.indexOf(r)).b;
                        card.discountEffects.add(new Discount(r, disc + amount));
                    } else {
                        card.discountEffects.add(new Discount(r, amount));
                    }
                } else if (effect.get("standardproject") != null) {
                    // A discount for buying standard projects
                    TMTypes.StandardProject sp = TMTypes.StandardProject.valueOf((String) effect.get("standardproject"));
                    r = new ActionTypeRequirement(TMTypes.ActionType.StandardProject, sp);
                    if (card.discountEffects.contains(r)) {
                        int disc = card.discountEffects.get(card.discountEffects.indexOf(r)).b;
                        card.discountEffects.add(new Discount(r, disc + amount));
                    } else {
                        card.discountEffects.add(new Discount(r, amount));
                    }
                }
            } else if (type.equalsIgnoreCase("resourcemapping")) {
                TMTypes.Resource from = TMTypes.Resource.valueOf((String)effect.get("from"));
                TMTypes.Resource to = TMTypes.Resource.valueOf((String)effect.get("to"));
                double rate = (double) effect.get("rate");
                card.resourceMappings.add(new TMGameState.ResourceMapping(from, to, rate,null));
            } else if (type.equalsIgnoreCase("effect")) {
                String condition = (String)effect.get("if");
                String result = (String)effect.get("then");
                persistingEffects.add(parseEffect(condition, TMAction.parseActionOnCard(result, card, true)));
            }
        }

        card.immediateEffects = immediateEffects.toArray(new TMAction[0]);
        card.actions = actions.toArray(new TMAction[0]);
        card.persistingEffects = persistingEffects.toArray(new Effect[0]);

        return card;
    }

    public static TMCard loadCardJSON(JSONObject cardDef)
    {
        TMCard card = null;
        try{
            GsonBuilder gsonBuilder = new GsonBuilder()
                    .registerTypeAdapter(Requirement.class, new SimpleDeserializer<Requirement>())
                    .registerTypeAdapter(Effect.class, new SimpleDeserializer<Effect>())
                    .registerTypeAdapter(Discount.class, new Discount())
                    .registerTypeAdapter(TMAction.class, new SimpleDeserializer<TMAction>())
                    ;
            Gson gson = gsonBuilder.setPrettyPrinting().create();
            card = gson.fromJson(cardDef.toJSONString(), TMCard.class);
        }catch(Exception e)
        {
            e.printStackTrace();
        }

        return card;
    }

    public static TMCard loadCardHTML(JSONObject cardDef) {
        TMCard card = new TMCard();
        String classDef = (String)cardDef.get("@class");
        card.cardType = Utils.searchEnum(TMTypes.CardType.class, classDef.split(" ")[1].trim());
        card.annotation = (String) cardDef.get("annotation");
        JSONArray div1 = (JSONArray) cardDef.get("div");
        ArrayList<TMTypes.Tag> tempTags = new ArrayList<>();

        ArrayList<TMAction> immediateEffects = new ArrayList<>();
        ArrayList<TMAction> actions = new ArrayList<>();
        ArrayList<Effect> persistingEffects = new ArrayList<>();
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
                            String[] reqStr = ((String) ob2.get("#text")).split("\\*");
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
                                } else if (s.contains("tile") && !s.contains("tiles")) {
                                    // Tile count placed requirement
                                    String[] split = s.split(" ");
                                    int nTiles = 0;
                                    boolean max = false;
                                    boolean any = false;
                                    TMTypes.Tile t = TMTypes.Tile.valueOf(split[0].trim());
                                    for (int i = 0; i < split.length; i++) {
                                        if (split[i].equalsIgnoreCase("tile")) {
                                            nTiles = i;
                                        } else if (split[i].equalsIgnoreCase("max")) {
                                            max = true;
                                        } else if (split[i].equalsIgnoreCase("any")) {
                                            any = true;
                                        }
                                    }
                                    Requirement r = new TilePlacedRequirement(t, nTiles, max, any);
                                    card.requirements.add(r);
                                } else if (s.contains("resources")) {
                                    // Resources on cards requirement
                                    s = s.replace("resources","").trim();
                                    String[] split = s.split(" ");
                                    HashMap<TMTypes.Resource, Integer> resourceCount = new HashMap<>();
                                    for (String s2: split) {
                                        TMTypes.Resource t = TMTypes.Resource.valueOf(s2);
                                        if (resourceCount.containsKey(t)) {
                                            resourceCount.put(t, resourceCount.get(t) + 1);
                                        } else {
                                            resourceCount.put(t, 1);
                                        }
                                    }
                                    TMTypes.Resource[] resources = new TMTypes.Resource[resourceCount.size()];
                                    int[] min = new int[resourceCount.size()];
                                    int i = 0;
                                    for (TMTypes.Resource t: resourceCount.keySet()) {
                                        resources[i] = t;
                                        min[i] = resourceCount.get(t);
                                        i++;
                                    }
                                    Requirement r = new ResourcesOnCardsRequirement(resources, min);
                                    card.requirements.add(r);
                                } else {
                                    // Counter requirement
                                    boolean max = s.contains("max");
                                    s = s.replace("max", "").trim();
                                    String[] split = s.split(" ");
                                    // first is threshold, second is counter code
                                    Requirement r = new CounterRequirement(split[1], Integer.parseInt(split[0]), max);
                                    card.requirements.add(r);
                                }
                            }
                        } else if (info2 != null && info2.contains("description")) {
                            String ps = (String) ob2.get("#text");
                            String[] split = ps.split("\\*");  // * separates multiple effects
                            for (String s: split) {
                                if (s.contains("Requires") || s.contains("must"))
                                    continue;  // Already parsed requirements
                                s = s.trim();
                                if (s.contains("Action:")) {
                                    TMAction a = parseActionOnCard(s.split(":")[1], card, false);
                                    if (a != null) {
                                        a.actionType = TMTypes.ActionType.ActiveAction;
                                        actions.add(a);
                                    }
                                } else if (s.contains("stays on this card")) {
                                    card.canResourcesBeRemoved = false;
                                } else if (s.contains("next card")) {
                                    // TODO temporary effects, removed after player plays a card (optionally, this generation)
                                    boolean thisGeneration = s.contains("this generation");
                                    HashSet<Requirement> reqs = parseDiscount(s.split(" is ")[1].split("-"));
                                }
                                else if (s.contains("Effect:")) {
                                    if (s.contains("discount")) {
                                        // Discount effects
                                        String[] split2 = s.split("-");
                                        int amount = Integer.parseInt(split2[1]);
                                        HashSet<Requirement> reqs = parseDiscount(split2);
                                        for (Requirement r: reqs) {
                                            if (card.discountEffects.contains(r)) {
                                                int disc = card.discountEffects.get(card.discountEffects.indexOf(r)).b;
                                                card.discountEffects.add(new Discount(r, disc + amount));
                                            } else {
                                                card.discountEffects.add(new Discount(r, amount));
                                            }
                                        }
                                    } else if (s.contains("resourcemapping")) {
                                        // Resource mappings
                                        String[] split2 = s.split("-");
                                        TMTypes.Resource from = TMTypes.Resource.valueOf(split2[1]);
                                        TMTypes.Resource to = TMTypes.Resource.valueOf(split2[2]);
                                        double rate = Double.parseDouble(split2[3]);
                                        card.resourceMappings.add(new TMGameState.ResourceMapping(from, to, rate,null));
                                    } else {
                                        // Persisting effects
                                        // Format:
//                                    payforaction(StandardProject) / effect
//                                    playcard(tag-Tag,Tag,Tag-any) / effect
//                                    placetile(City,onMars,any) / effect

                                        String s2 = s.split(":")[1].trim();
                                        String when = s2.split(" / ")[0].trim();
                                        String then = s2.split(" / ")[1].trim();
                                        Effect e = parseEffect(when, TMAction.parseActionOnCard(then, card, true));
                                        persistingEffects.add(e);
                                    }
                                }
                                else if (s.contains("VP")) {
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
                                    // parse immediate effects
                                    TMAction a = parseActionOnCard(s, card, true);
                                    if (a != null) {
                                        immediateEffects.add(a);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        card.actions = actions.toArray(new TMAction[0]);
        card.persistingEffects = persistingEffects.toArray(new Effect[0]);
        card.immediateEffects = immediateEffects.toArray(new TMAction[0]);
        card.tags = tempTags.toArray(new TMTypes.Tag[0]);

        return card;
    }

    private static HashSet<Requirement> parseDiscount(String[] split2) {
        HashSet<Requirement> reqs = new HashSet<>();
        if (split2.length > 2) {
            if (split2[2].equalsIgnoreCase("global")) {
                // global parameter effect
                for (TMTypes.GlobalParameter gp: TMTypes.GlobalParameter.values()) {
                    reqs.add(new CounterRequirement(gp.name(), -1, true));
                }
            } else {
                // A tag discount?
                String[] tagDef = split2[2].split(",");
                TMTypes.Tag[] tags = new TMTypes.Tag[tagDef.length];
                for (int i = 0; i < tagDef.length; i++) {
                    TMTypes.Tag t = Utils.searchEnum(TMTypes.Tag.class, tagDef[i]);
                    if (t != null) {
                        tags[i] = t;
                    } else {
                        tags = null;
                        break;
                    }
                }
                if (tags != null) {
                    reqs.add(new TagOnCardRequirement(tags));
                }
            }
        } else {
            reqs.add(new TagOnCardRequirement(null));
        }
        return reqs;
    }

    private static Effect parseEffect(String when, TMAction then) {
        String[] ss = when.split("\\(");
        String actionTypeCondition = ss[0].trim();
        String content = ss[1].replace(")", "");
        boolean mustBeCurrentPlayer = !when.contains("any");

        if (actionTypeCondition.equalsIgnoreCase("placetile")) {
            // Place tile effect
            String[] split2 = content.split(",");
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
            return new PlaceTileEffect(mustBeCurrentPlayer, then, when.contains("onMars"), tile, resourcesGained);
        } else if (actionTypeCondition.equalsIgnoreCase("playcard")) {
            // Play card effect
            String[] tagDef = content.split("-")[1].split(",");
            HashSet<TMTypes.Tag> tags = new HashSet<>();
            for (String s: tagDef) {
                tags.add(TMTypes.Tag.valueOf(s));
            }
            return new PlayCardEffect(mustBeCurrentPlayer, then, tags);
        } else if (actionTypeCondition.equalsIgnoreCase("payforaction")) {
            // pay for action effect
            TMTypes.ActionType at = Utils.searchEnum(TMTypes.ActionType.class, content);
            if (at != null) {
                return new PayForActionEffect(mustBeCurrentPlayer, then, at);
            } else {
                int minCost = Integer.parseInt(content);
                return new PayForActionEffect(mustBeCurrentPlayer, then, minCost);
            }
        } else if (actionTypeCondition.equals("globalparameter")) {
            // Increase parameter effect
            TMTypes.GlobalParameter param = Utils.searchEnum(TMTypes.GlobalParameter.class, content.split("-")[0]);
            if (param != null) {
                return new GlobalParameterEffect(mustBeCurrentPlayer, then, param);
            }
        }
        // todo inc(prod-1)

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMCard)) return false;
        if (!super.equals(o)) return false;
        TMCard tmCard = (TMCard) o;
        return number == tmCard.number && cost == tmCard.cost && firstActionExecuted == tmCard.firstActionExecuted && actionPlayed == tmCard.actionPlayed && mapTileIDTilePlaced == tmCard.mapTileIDTilePlaced && Double.compare(tmCard.nPoints, nPoints) == 0 && pointsTileAdjacent == tmCard.pointsTileAdjacent && nResourcesOnCard == tmCard.nResourcesOnCard && canResourcesBeRemoved == tmCard.canResourcesBeRemoved && Objects.equals(annotation, tmCard.annotation) && cardType == tmCard.cardType && Objects.equals(requirements, tmCard.requirements) && Arrays.equals(tags, tmCard.tags) && Objects.equals(discountEffects, tmCard.discountEffects) && Objects.equals(resourceMappings, tmCard.resourceMappings) && Arrays.equals(persistingEffects, tmCard.persistingEffects) && Objects.equals(firstAction, tmCard.firstAction) && Arrays.equals(actions, tmCard.actions) && Arrays.equals(immediateEffects, tmCard.immediateEffects) && pointsResource == tmCard.pointsResource && Objects.equals(pointsThreshold, tmCard.pointsThreshold) && pointsTag == tmCard.pointsTag && pointsTile == tmCard.pointsTile && resourceOnCard == tmCard.resourceOnCard;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), number, annotation, cardType, cost, requirements, discountEffects, resourceMappings, firstAction, firstActionExecuted, actionPlayed, mapTileIDTilePlaced, nPoints, pointsResource, pointsThreshold, pointsTag, pointsTile, pointsTileAdjacent, resourceOnCard, nResourcesOnCard, canResourcesBeRemoved);
        result = 31 * result + Arrays.hashCode(tags);
        result = 31 * result + Arrays.hashCode(persistingEffects);
        result = 31 * result + Arrays.hashCode(actions);
        result = 31 * result + Arrays.hashCode(immediateEffects);
        return result;
    }

    @Override
    public TMCard copy() {
        TMCard copy = new TMCard(componentName, componentID);
        copy.number = number;
        copy.cardType = cardType;
        copy.firstActionExecuted = firstActionExecuted;
        copy.actionPlayed = actionPlayed;
        copy.annotation = annotation;
        copy.cost = cost;
        if (requirements != null) {
            copy.requirements = new HashSet<>();
            for (Requirement r: requirements) {
                copy.requirements.add(r.copy());
            }
        }
        copy.tags = tags.clone();
        if (discountEffects != null) {
            copy.discountEffects = new LinkedList<>();
            for (Discount discountEffect : discountEffects) {
                copy.discountEffects.add(new Discount(discountEffect.a.copy(), discountEffect.b));
            }
        }
        if (resourceMappings != null) {
            copy.resourceMappings = new HashSet<>();
            for (TMGameState.ResourceMapping rm: resourceMappings) {
                copy.resourceMappings.add(rm.copy());
            }
        }
        if (persistingEffects != null) {
            copy.persistingEffects =  new Effect[persistingEffects.length];
            for (int i = 0; i < persistingEffects.length; i++) {
                if (persistingEffects[i] != null) {
                    copy.persistingEffects[i] = persistingEffects[i].copy();
                }
            }
        }
        if (firstAction != null) {
            copy.firstAction = firstAction.copy();
        }
        if (actions != null) {
            copy.actions = new TMAction[actions.length];
            for (int i = 0; i < actions.length; i++) {
                copy.actions[i] = actions[i].copy();
            }
        }
        if (immediateEffects != null) {
            copy.immediateEffects = new TMAction[immediateEffects.length];
            for (int i = 0; i < immediateEffects.length; i++) {
                copy.immediateEffects[i] = immediateEffects[i].copy();
            }
        }
        copy.mapTileIDTilePlaced = mapTileIDTilePlaced;
        copy.nPoints = nPoints;
        copy.pointsResource = pointsResource;
        copy.pointsThreshold = pointsThreshold;
        copy.pointsTag = pointsTag;
        copy.pointsTile = pointsTile;
        copy.pointsTileAdjacent = pointsTileAdjacent;
        copy.resourceOnCard = resourceOnCard;
        copy.nResourcesOnCard = nResourcesOnCard;
        copy.canResourcesBeRemoved = canResourcesBeRemoved;
        copyComponentTo(copy);
        return copy;
    }

    public TMCard copySerializable() {
        TMCard copy = new TMCard(componentName, componentID);
        copy.number = number;
        copy.cardType = cardType;
        copy.annotation = annotation;
        copy.cost = cost;
        if (requirements != null && requirements.size() > 0) {
            copy.requirements = new HashSet<>();
            for (Requirement r: requirements) {
                copy.requirements.add(r.copySerializable());
            }
        } else copy.requirements = null;
        if (tags != null && tags.length > 0) {
            copy.tags = tags.clone();
        } else copy.tags = null;
        if (discountEffects != null && discountEffects.size() > 0) {
            copy.discountEffects = new LinkedList<>();
            for (Discount discount :discountEffects) {
                copy.discountEffects.add(new Discount(discount.a.copySerializable(), discount.b));
            }
        } else copy.discountEffects = null;
        if (resourceMappings != null && resourceMappings.size() > 0) {
            copy.resourceMappings = new HashSet<>();
            for (TMGameState.ResourceMapping rm: resourceMappings) {
                copy.resourceMappings.add(rm.copy());
            }
        } else copy.resourceMappings = null;
        if (persistingEffects != null && persistingEffects.length > 0) {
            copy.persistingEffects =  new Effect[persistingEffects.length];
            for (int i = 0; i < persistingEffects.length; i++) {
                if (persistingEffects[i] != null) {
                    copy.persistingEffects[i] = persistingEffects[i].copySerializable();
                }
            }
        } else copy.persistingEffects = null;
        if (firstAction != null) {
            copy.firstAction = firstAction.copySerializable();
        } else copy.firstAction = null;
        if (actions != null && actions.length > 0) {
            copy.actions = new TMAction[actions.length];
            for (int i = 0; i < actions.length; i++) {
                copy.actions[i] = actions[i].copySerializable();
            }
        } else copy.actions = null;
        if (immediateEffects != null && immediateEffects.length > 0) {
            copy.immediateEffects = new TMAction[immediateEffects.length];
            for (int i = 0; i < immediateEffects.length; i++) {
                copy.immediateEffects[i] = immediateEffects[i].copySerializable();
            }
        } else copy.immediateEffects = null;
        copy.mapTileIDTilePlaced = mapTileIDTilePlaced;
        copy.nPoints = nPoints;
        copy.pointsResource = pointsResource;
        copy.pointsThreshold = pointsThreshold;
        copy.pointsTag = pointsTag;
        copy.pointsTile = pointsTile;
        copy.pointsTileAdjacent = pointsTileAdjacent;
        copy.resourceOnCard = resourceOnCard;
        copy.nResourcesOnCard = nResourcesOnCard;
        copy.canResourcesBeRemoved = canResourcesBeRemoved;
        copyComponentTo(copy);
        if (properties.size() == 0) copy.properties = null;
        return copy;
    }
}

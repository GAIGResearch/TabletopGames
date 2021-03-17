package games.terraformingmars.components;

import core.actions.AbstractAction;
import core.components.Card;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.*;
import games.terraformingmars.rules.CounterRequirement;
import games.terraformingmars.rules.Requirement;
import games.terraformingmars.rules.ResourceIncGenRequirement;
import games.terraformingmars.rules.TagRequirement;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class TMCard extends Card {
    public int number;
    public TMTypes.CardType cardType;
    public int cost;
    public Requirement<TMGameState> requirement;
    public TMTypes.Tag[] tags;

    public HashMap<Requirement, Integer> discountEffects;
    public TMAction firstAction;  // first action for the player is already decided to be this
    public TMAction[] rules;  // long-lasting effects
    public TMAction[] actions;  // new actions available to the player
    public TMAction[] effects; // effect of this card, executed immediately
    public double nPoints;  // if tokens, number of points will be nPoints * tokens

    public TMTypes.TokenType pointsTokenType;  // Type of token placed on this card
    public TMTypes.TokenType[] tokensOnCard;  // One count for each type of token

    public TMCard() {
        tokensOnCard = new TMTypes.TokenType[TMTypes.TokenType.values().length];
        tags = new TMTypes.Tag[0];
        rules = new TMAction[0];
        actions = new TMAction[0];
        effects = new TMAction[0];
        discountEffects = new HashMap<>();
    }

    public static TMCard loadCard(JSONObject cardDef) {
        TMCard card = new TMCard();
        card.setComponentName((String)cardDef.get("name"));
        card.cardType = TMTypes.CardType.valueOf((String)cardDef.get("type"));
        if (cardDef.get("cost") != null) {
            card.cost = ((Long) cardDef.get("cost")).intValue();
        }
        if (cardDef.get("requirement") != null) {
            card.requirement = Requirement.stringToRequirement((String) cardDef.get("requirement"));
        }

        JSONArray tagDef = (JSONArray) cardDef.get("tags");
        card.tags = new TMTypes.Tag[tagDef.size()];
        for (int i = 0; i < tagDef.size(); i++) {
            card.tags[i] = TMTypes.Tag.valueOf((String)tagDef.get(i));
        }

        // TODO: rules, actions, effects

        card.nPoints = 0;
        if (cardDef.get("points") != null) {
            card.nPoints = (Double)cardDef.get("points");
        }
        card.tokensOnCard = new TMTypes.TokenType[TMTypes.TokenType.values().length];
        if (cardDef.get("token-type") != null) {
            card.pointsTokenType = TMTypes.TokenType.valueOf((String)cardDef.get("token-type"));
        }

        return card;
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
            immediateEffects.add(new PlaceholderModifyCounter(amount, res, split2[1].contains("prod"), true));
        }
        for (int i = 1; i < start.size(); i++) {
            JSONObject other = (JSONObject) start.get(i);
            String type = (String) other.get("type");
            if (type.equalsIgnoreCase("first")) {
                // First action in action phase for the player is decided, not free
                String action = (String) other.get("action");
                if (action.equalsIgnoreCase("resourcetransaction")) {
                    card.firstAction = new ResourceTransaction(TMTypes.Resource.valueOf((String) other.get("resource")), (int)(long)other.get("amount"), false);
                } else if (action.equalsIgnoreCase("placetile")) {
                    card.firstAction = new PlaceTile(TMTypes.Tile.valueOf((String) other.get("tile")), null, false);
                }
                // TODO: other actions?
            }
            // TODO: other options?
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

        JSONArray effects = (JSONArray) cardDef.get("effect");  // TODO
        ArrayList<TMAction> actions = new ArrayList<>();
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
                    TMAction a = new PayForAction(new PlaceTile(TMTypes.Tile.valueOf(action[1]), null, false),
                            costResource, -cost, -1);
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
                    TMAction a = new PayForAction(new ResourceTransaction(r, amount, false, req), costResource, -cost, -1);
                    actions.add(a);
                }
            } else if (type.equalsIgnoreCase("discount")) {
                // Parse discounts
                int amount = (int)(long)effect.get("amount");
                Requirement r = null;
                if (effect.get("counter") != null) {
                    // A discount for CounterRequirement
                    for (Object o2: (JSONArray)effect.get("counter")) {
                        r = new CounterRequirement((String)o2, -1, true);
                    }
                } else if (effect.get("tag") != null) {
                    // A discount for tag requirements
                    TMTypes.Tag t = TMTypes.Tag.valueOf((String) effect.get("tag"));
                    r = new TagRequirement(new TMTypes.Tag[]{t}, null);
                }
                if (r != null) {
                    if (card.discountEffects.containsKey(r)) {
                        card.discountEffects.put(r, card.discountEffects.get(r) + amount);
                    } else {
                        card.discountEffects.put(r, amount);
                    }
                }
            }
        }

        card.effects = immediateEffects.toArray(new TMAction[0]);
        card.actions = actions.toArray(new TMAction[0]);

        return card;
    }

    public static TMCard loadCardHTML(JSONObject cardDef) {
        TMCard card = new TMCard();
        String classDef = (String)cardDef.get("@class");
        card.cardType = Utils.searchEnum(TMTypes.CardType.class, classDef.split(" ")[1].trim());
        JSONArray div1 = (JSONArray) cardDef.get("div");
        ArrayList<TMTypes.Tag> tempTags = new ArrayList<>();

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
                            card.nPoints = Double.parseDouble(ps.replace("/", ""));
                            if (ob2.get("div") != null) {
                                String[] pointCondition = ((String) ((JSONObject) ob2.get("div")).get("@class")).split(" ");
                                if (pointCondition[0].equalsIgnoreCase("resource")) {
                                    // Find resource that earns points on this card
                                    card.pointsTokenType = Utils.searchEnum(TMTypes.TokenType.class, pointCondition[1]);
                                }
                                // TODO more cases...
                            }
                        } else if (info2 != null && info2.contains("requirements")) {
                            String reqs = (String) ob2.get("#text");
                            // TODO parse...
                        }

                        // "red arrow" is action, ":" is effect
                    }
                }
            }
        }

        card.tags = tempTags.toArray(new TMTypes.Tag[0]);

        return card;
    }
}

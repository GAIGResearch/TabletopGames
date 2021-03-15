package games.terraformingmars.components;

import core.actions.AbstractAction;
import core.components.Card;
import games.terraformingmars.TMTypes;
import games.terraformingmars.rules.Requirement;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.Utils;

import java.util.ArrayList;

public class TMCard extends Card {
    public int number;
    public TMTypes.CardType cardType;
    public int cost;
    public Requirement requirement;
    public TMTypes.Tag[] tags;

    public AbstractAction[] rules;  // long-lasting effects
    public AbstractAction[] actions;  // new actions available to the player
    public AbstractAction[] effects; // effect of this card, executed immediately
    public double nPoints;  // if tokens, number of points will be nPoints * tokens

    public TMTypes.TokenType pointsTokenType;  // Type of token placed on this card
    public TMTypes.TokenType[] tokensOnCard;  // One count for each type of token

    public TMCard() {
        tokensOnCard = new TMTypes.TokenType[TMTypes.TokenType.values().length];
        tags = new TMTypes.Tag[0];
        rules = new AbstractAction[0];
        actions = new AbstractAction[0];
        effects = new AbstractAction[0];
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

package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTurnOrder;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.effects.Effect;
import games.terraformingmars.rules.requirements.AdjacencyRequirement;
import games.terraformingmars.rules.requirements.Requirement;
import games.terraformingmars.rules.requirements.ResourceRequirement;
import utilities.Pair;
import utilities.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class TMAction extends AbstractAction {
    public boolean freeActionPoint;
    public int player;
    public boolean pass;

    public Requirement<TMGameState> costRequirement;
    public HashSet<Requirement<TMGameState>> requirements;

    public TMTypes.ActionType actionType;
    public TMTypes.StandardProject standardProject;
    public TMTypes.BasicResourceAction basicResourceAction;

    private int cost = 0;
    private TMTypes.Resource costResource;
    transient private int playCardID = -1;  // Card used to play this action (factors into the cost of the action)
    transient private int cardID = -1;  // Card related to the action (does not factor into the cost)

    public TMAction()  {  } // This is needed for JSON Deserializer

    public TMAction(TMTypes.ActionType actionType, int player, boolean free) {
        this.player = player;
        this.freeActionPoint = free;
        this.pass = false;
        this.actionType = actionType;
        this.requirements = new HashSet<>();
    }

    public TMAction(TMTypes.StandardProject project, int player, boolean free) {
        this.player = player;
        this.freeActionPoint = free;
        this.pass = false;
        this.actionType = TMTypes.ActionType.StandardProject;
        this.standardProject = project;
        this.requirements = new HashSet<>();
    }

    public TMAction(TMTypes.BasicResourceAction basicResourceAction, int player, boolean free) {
        this.player = player;
        this.freeActionPoint = free;
        this.pass = false;
        this.actionType = TMTypes.ActionType.BasicResourceAction;
        this.basicResourceAction = basicResourceAction;
        this.requirements = new HashSet<>();
    }

    public TMAction(int player) {
        this.player = player;
        this.freeActionPoint = false;
        this.pass = true;
        this.requirements = new HashSet<>();
    }

    public TMAction(int player, boolean free) {
        this.player = player;
        this.freeActionPoint = free;
        this.pass = false;
        this.requirements = new HashSet<>();
    }

    public TMAction(int player, boolean free, HashSet<Requirement<TMGameState>> requirement) {
        this.player = player;
        this.freeActionPoint = free;
        this.pass = false;
        this.requirements = new HashSet<>(requirement);
    }

    public TMAction(TMTypes.ActionType actionType, int player, boolean free, HashSet<Requirement<TMGameState>> requirement) {
        this.player = player;
        this.freeActionPoint = free;
        this.pass = false;
        this.requirements = new HashSet<>(requirement);
        this.actionType = actionType;
    }

    public TMAction(TMTypes.StandardProject project, int player, boolean free, HashSet<Requirement<TMGameState>> requirement) {
        this.player = player;
        this.freeActionPoint = free;
        this.pass = false;
        this.requirements = new HashSet<>(requirement);
        this.actionType = TMTypes.ActionType.StandardProject;
        this.standardProject = project;
    }

    public TMAction(TMTypes.BasicResourceAction basicResourceAction, int player, boolean free, HashSet<Requirement<TMGameState>> requirement) {
        this.player = player;
        this.freeActionPoint = free;
        this.pass = false;
        this.requirements = new HashSet<>(requirement);
        this.actionType = TMTypes.ActionType.BasicResourceAction;
        this.basicResourceAction = basicResourceAction;
    }

    public void setActionCost(TMTypes.Resource resource, int cost, int cardID) {
        this.costResource = resource;
        this.cost = cost;
        this.playCardID = cardID;
        this.costRequirement = new ResourceRequirement(resource, Math.abs(cost), false, player, cardID);
        this.requirements.add(costRequirement);
    }

    public boolean canBePlayed(TMGameState gs) {
        boolean played = false;
        if (getCardID() != -1) {
            TMCard c = (TMCard) gs.getComponentById(getCardID());
            if (c != null && c.actionPlayed) played = true;
        }
        if (played && standardProject == null && basicResourceAction == null) return false;
        if (requirements != null && requirements.size() > 0) {
            for (Requirement r: requirements) {
                if (!r.testCondition(gs)) return false;
            }
        }
        return true;
    }

    public boolean _execute(TMGameState gameState) { return true; }

    @Override
    public final boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        gs.getAllComponents();  // Force recalculate components
        if (player == -1) player = gameState.getCurrentPlayer();
        if (!canBePlayed(gs)) {
            throw new AssertionError("Card cannot be played " + this);
        }
        boolean s = _execute(gs);
        postExecute(gs);
        return s;
    }

    public void postExecute(TMGameState gs) {
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();
        if (player < 0 || player >= gs.getNPlayers()) return;
        if (!freeActionPoint) {
            ((TMTurnOrder)gs.getTurnOrder()).registerActionTaken(gs, this, player);
        }
        if (getCardID() != -1 && !(this instanceof BuyCard) && !(this instanceof PlayCard) && !(this instanceof DiscardCard)) {
            TMCard c = (TMCard) gs.getComponentById(getCardID());
            if (c != null) {
                if (!c.firstActionExecuted && c.firstAction != null) {
                    c.firstActionExecuted = true;
                } else if (c.cardType == TMTypes.CardType.Active || c.cardType == TMTypes.CardType.Corporation) {
                    c.actionPlayed = true;
                }
            }
        }

        // Check persisting effects for all players
        for (int i = 0; i < gs.getNPlayers(); i++) {
            for (Effect e: gs.getPlayerPersistingEffects()[i]) {
                if (e == null) continue;
                e.execute(gs, this, i);
            }
        }

        // Check if player has Card resources, transform those to cards into hand
        Counter c = gs.getPlayerResources()[player].get(TMTypes.Resource.Card);
        int nCards = c.getValue();
        if (nCards > 0) {
            for (int i = 0; i < nCards; i++) {
                TMCard card = gs.drawCard();
                if (card != null) {
                    gs.getPlayerHands()[player].add(card);
                } else {
                    break;
                }
            }
            c.setValue(0);
        } else if (nCards < 0) {
            // Player needs to discard nCards from hand
            for (int i = 0; i < Math.abs(nCards); i++) {
                new DiscardCard(player, false).execute(gs);
            }
        }
    }

    public TMAction _copy() { return new TMAction(player, freeActionPoint); }

    @Override
    public TMAction copy() {
        TMAction action = _copy();
        action.freeActionPoint = freeActionPoint;
        action.player = player;
        action.pass = pass;
        if (costRequirement != null) {
            action.costRequirement = costRequirement.copy();
        }
        if (requirements != null) {
            action.requirements = new HashSet<>();
            for (Requirement r : requirements) {
                action.requirements.add(r.copy());
            }
        }
        action.actionType = actionType;
        action.standardProject = standardProject;
        action.basicResourceAction = basicResourceAction;
        action.cost = cost;
        action.costResource = costResource;
        action.playCardID = playCardID;
        action.cardID = cardID;
        return action;
    }

    public TMAction copySerializable() {
        TMAction action = _copy();
        action.freeActionPoint = freeActionPoint;
        action.player = player;
        action.pass = pass;
        if (costRequirement != null) {
            action.costRequirement = costRequirement.copy();
        }
        if (requirements != null){ // && requirements.size() > 0) {
            action.requirements = new HashSet<>();
            for (Requirement r : requirements) {
                action.requirements.add(r.copy());
            }
        } else action.requirements = null;
        action.actionType = actionType;
        action.standardProject = standardProject;
        action.basicResourceAction = basicResourceAction;
        action.cost = cost;
        action.costResource = costResource;
        action.playCardID = playCardID;
        action.cardID = cardID;
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMAction)) return false;
        TMAction tmAction = (TMAction) o;
        return freeActionPoint == tmAction.freeActionPoint && player == tmAction.player && pass == tmAction.pass &&
                cost == tmAction.cost && playCardID == tmAction.playCardID && cardID == tmAction.cardID &&
                Objects.equals(costRequirement, tmAction.costRequirement) &&
                Objects.equals(requirements, tmAction.requirements) && actionType == tmAction.actionType &&
                standardProject == tmAction.standardProject && basicResourceAction == tmAction.basicResourceAction &&
                costResource == tmAction.costResource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(freeActionPoint, player, pass, costRequirement, requirements, actionType, standardProject, basicResourceAction, cost, costResource, playCardID, cardID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return actionType != null? actionType.name() : "Pass";
    }

    @Override
    public String toString() {
        return actionType != null? actionType.name() : "Pass";
    }

    public int getCost() {
        return cost;  // 0 by default, the cost in resources this action takes to perform
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public TMTypes.Resource getCostResource() {
        return costResource;  // none by default, the resource to be paid for this action
    }

    public int getPlayCardID() {
        return playCardID;  // none by default, the component ID of card used for this action
    }

    public void setCardID(int cardID) {
        this.cardID = cardID;
    }

    public int getCardID() {
        return cardID;
    }



    /* parsing input **/


    public static TMAction parseActionOnCard(String s, TMCard card, boolean free) {
        String[] orSplit = s.split(" or ");
        String[] andSplit = s.split(" and ");
        int cardID = -1;
        if (card != null) cardID = card.getComponentID();
        if (orSplit.length == 1) {
            if (andSplit.length == 1) {
                TMAction a = TMAction.parseAction(s, free, cardID).a;
                if (a != null) {
                    if (a instanceof PlaceTile) {
                        a.setCardID(cardID);
                    } else if (card != null && a instanceof AddResourceOnCard && (!((AddResourceOnCard) a).chooseAny || a.cardID == -1)) {
                        // if add resource on card (not other), set resource on this card
                        card.resourceOnCard = ((AddResourceOnCard) a).resource;
                    }
                    return a;
                }
            } else {
                // Compound action
                return processCompoundAction(free, andSplit, cardID);
            }
        } else {
            // Action choice
            TMAction[] actionChoice = new TMAction[orSplit.length];
            int i = 0;
            for (String s2 : orSplit) {
                s2 = s2.trim();
                andSplit = s2.split(" and ");
                if (andSplit.length == 1) {
                    TMAction a = TMAction.parseAction(s2, free, cardID).a;
                    if (a != null) {
                        actionChoice[i] = a;
                        if (a instanceof PlaceTile) {
                            a.setCardID(cardID);
                        }
                    }
                } else {
                    // Compound action
                    actionChoice[i] = processCompoundAction(free, andSplit, cardID);
                }
                i++;
            }
            for (TMAction tmAction : actionChoice) {
                if (tmAction == null) {
                    int p = 0;
                }
            }
            return new ChoiceAction(-1, actionChoice);
        }
        return null;
    }

    private static CompoundAction processCompoundAction(boolean free, String[] andSplit, int cardID) {
        TMAction[] compound = new TMAction[andSplit.length];
        int j = 0;
        for (String s3 : andSplit) {
            s3 = s3.trim();
            TMAction a = TMAction.parseAction(s3, free, cardID).a;
            if (a != null) {
                compound[j] = a;
                if (a instanceof PlaceTile) {
                    a.setCardID(cardID);
                }
            }
            j++;
        }
        return new CompoundAction(-1, compound);
    }

    private static Pair<TMAction, String> parseAction(String encoding, boolean free, int cardID) {

        TMAction effect = null;
        String effectString = "";
        int player = -1;

        if (encoding.contains("inc") || encoding.contains("dec")) {
            // Increase/Decrease counter action, format: "inc-Res-Amount(-Any)" or "inc-Res-Amount-tag:Tag(-Any)" or "inc-Res-Amount-tile:Tile(-Any)" or "dec-Res-Amount..."
            String[] split2 = encoding.split("-");
            try {
                // Find how much
                Double increment = null;
                if (!split2[2].equalsIgnoreCase("x")) {
                    increment = Double.parseDouble(split2[2]);
                    if (encoding.contains("dec")) increment *= -1;
                }
                effectString = split2[1];

                if (increment == null) {
                    // - dec-Resource1-X-Resource2 : X = chosen by player from 0 to N first resource, decrease first resource that, increase second resource that
                    increment = 1.0;
                    String resString = split2[1].split("prod")[0];
                    TMTypes.Resource res1 = Utils.searchEnum(TMTypes.Resource.class, resString);
                    TMTypes.Resource res2 = Utils.searchEnum(TMTypes.Resource.class, split2[3].replace("prod", ""));
                    effect = new ModifyPlayerResource(player, player, increment, res1, split2[1].contains("prod"), free);
                    ((ModifyPlayerResource) effect).counterResource = res2;
                    ((ModifyPlayerResource) effect).counterResourceProduction = split2[3].contains("prod");
                } else {
                    // Find which counter
                    TMTypes.GlobalParameter which = Utils.searchEnum(TMTypes.GlobalParameter.class, split2[1]);
                    if (which == null) {
                        // A resource or production instead
                        String resString = split2[1].split("prod")[0];
                        TMTypes.Resource res = Utils.searchEnum(TMTypes.Resource.class, resString);
                        int targetPlayer = player;
                        if (split2.length > 3) {
                            if (split2[3].equalsIgnoreCase("any")) {
                                targetPlayer = -2;
                            }
                        }
                        effect = new ModifyPlayerResource(player, targetPlayer, increment, res, split2[1].contains("prod"), free);
                        if (split2.length > 3) {
                            if (!split2[3].equalsIgnoreCase("any")) {
                                if (split2[3].contains("tag")) {
                                    TMTypes.Tag tag = Utils.searchEnum(TMTypes.Tag.class, split2[3].split("\\+")[1]);
                                    if (tag != null) {
                                        ((ModifyPlayerResource) effect).tagToCount = tag;
                                        if (split2.length > 4) {
                                            if (split2[4].equalsIgnoreCase("any")) {
                                                ((ModifyPlayerResource) effect).any = true;
                                            } else if (split2[4].equalsIgnoreCase("opp")) {
                                                ((ModifyPlayerResource) effect).opponents = true;
                                            }
                                        }
                                    }
                                } else if (split2[3].contains("tile")) {
                                    // A tile
                                    TMTypes.Tile t = Utils.searchEnum(TMTypes.Tile.class, split2[3].split("\\+")[1]);
                                    if (t != null) {
                                        ((ModifyPlayerResource) effect).tileToCount = t;
                                        ((ModifyPlayerResource) effect).onMars = Boolean.parseBoolean(split2[4]);
                                        if (split2.length > 5) {
                                            if (split2[5].equalsIgnoreCase("any")) {
                                                ((ModifyPlayerResource) effect).any = true;
                                            } else if (split2[5].equalsIgnoreCase("opp")) {
                                                ((ModifyPlayerResource) effect).opponents = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // A global counter (temp, oxygen, oceantiles)
                        effect = new ModifyGlobalParameter(which, increment, free);
                    }
                }
            } catch (Exception ignored) {}
        } else if (encoding.contains("placetile")) {
            // PlaceTile action, format: "placetile/Tile/{Where = map type or name or Volcanic or resources sep by -}/{true or false for OnMars}/{Adjacency: Owned or None or tile names sep by -}"
            String[] split2 = encoding.split("/");
            // split2[1] is type of tile to place
            TMTypes.Tile toPlace;
            if (split2[1].equalsIgnoreCase("capital")) toPlace = TMTypes.Tile.City;
            else toPlace = Utils.searchEnum(TMTypes.Tile.class, split2[1]);
            if (toPlace != null) {
                // split2[2] is where to place it. can be a map tile, or a city name, or volcanic or resources gained.
                if (split2[2].equalsIgnoreCase("volcanic")) {
                    // Volcanic restriction
                    effect = new PlaceTile(player, toPlace, true, free);
                } else if (split2[2].contains("-")) {
                    String[] split3 = split2[2].split("-");
                    TMTypes.Resource[] resources = new TMTypes.Resource[split3.length];
                    for (int i = 0; i < split3.length; i++) {
                        resources[i] = TMTypes.Resource.valueOf(split3[i]);
                    }
                    // Resource gained restriction
                    effect = new PlaceTile(player, toPlace, resources, free);
                } else {
                    // Map tile restriction
                    TMTypes.MapTileType where = Utils.searchEnum(TMTypes.MapTileType.class, split2[2]);
                    boolean onMars = Boolean.parseBoolean(split2[3]);
                    if (where == null) {
                        // A named tile
                        effect = new PlaceTile(player, toPlace, split2[2], onMars, free);
                        ((PlaceTile) effect).respectingAdjacency = false;
                    } else {
                        boolean respectAdjacency = where == toPlace.getRegularLegalTileType();
                        effect = new PlaceTile(player, toPlace, where, free);  // Extended sequence, will ask player where to put it
                        ((PlaceTile) effect).respectingAdjacency = respectAdjacency;
                    }
                }
            }
            effectString = split2[1];
            if (effect != null && split2.length > 4) {
                // split2[4] = adjacency rule: X tile types separated by -, Owned, None (not placed)
                // Adjacency rules
                AdjacencyRequirement req = null;
                if (split2[4].equalsIgnoreCase("Owned")) {
                    req = new AdjacencyRequirement();
                    req.owned = true;
                } else if (split2[4].equalsIgnoreCase("None")) {
                    req = new AdjacencyRequirement();
                    req.noneAdjacent = true;
                } else if (split2[4].contains("any")) {
                    // If adjacent to any tiles, can remove resources from owner of those tiles: any-Amount-Resource
                    String[] split3 = split2[4].split("-");
                    int amount = Integer.parseInt(split3[1]);
                    TMTypes.Resource res = Utils.searchEnum(TMTypes.Resource.class, split3[2].replace("prod", ""));
                    ((PlaceTile) effect).removeResourcesAdjacentOwner = true;
                    ((PlaceTile) effect).removeResourcesRes = res;
                    ((PlaceTile) effect).removeResourcesAmount = amount;
                    ((PlaceTile) effect).removeResourcesProd = split3[2].contains("prod");
                } else {
                    // Adjacent to some types, make hashmap
                    HashMap<TMTypes.Tile, Integer> types = new HashMap<>();
                    String[] split3 = split2[4].split("-");
                    for (String s: split3) {
                        TMTypes.Tile t = TMTypes.Tile.valueOf(s);
                        if (types.containsKey(t)) {
                            types.put(t, types.get(t)+1);
                        } else {
                            types.put(t, 1);
                        }
                    }
                    req = new AdjacencyRequirement(types);
                }
                ((PlaceTile) effect).adjacencyRequirement = req;
            }
        } else if (encoding.contains("reserve")) {
            // Reserve tile action, places Reserve token and gets resources, only that player can place a tile there
            String[] split2 = encoding.split("/");
            // split2[1] is map type where this can be placed
            TMTypes.MapTileType toPlace = TMTypes.MapTileType.valueOf(split2[1]);
            effect = new ReserveTile(-1, toPlace, free);
        } else if (encoding.contains("add") || encoding.contains("rem")) {
            // Add resource to card: add/rem-amount-Resource-another/any-minAmount/Tag-Tag(on card top of draw deck, to be discarded)
            int sign = encoding.contains("rem") ? -1 : 1;
            String[] split2 = encoding.split("-");
            try {
                int amount = Integer.parseInt(split2[1]);
                TMTypes.Resource res = Utils.searchEnum(TMTypes.Resource.class, split2[2]);
                if (encoding.contains("that")) cardID = -1;
                effect = new AddResourceOnCard(-1, cardID, res, amount * sign, free);

                if (split2.length > 3) {
                    if (split2[3].equalsIgnoreCase("another")) {
                        effect.cardID = -1;
                    } else if (split2[3].equalsIgnoreCase("any")) {
                        effect.cardID = -1;
                        ((AddResourceOnCard) effect).chooseAny = true;
                    }
                    if (split2.length > 4) {
                        if (split2[4].contains("min")) {
                            // min resources on target card required
                            ((AddResourceOnCard) effect).minResRequirement = Integer.parseInt(split2[4].replace("min", ""));
                        } else {
                            // maybe a tag required;
                            ((AddResourceOnCard) effect).tagRequirement = Utils.searchEnum(TMTypes.Tag.class, split2[4]);
                        }
                    }
                    if (split2.length > 5) {
                        // Tag that top card of draw deck should have to execute this action (card is discarded either way)
                        ((AddResourceOnCard) effect).tagTopCardDrawDeck = Utils.searchEnum(TMTypes.Tag.class, split2[5]);
                    }
                }
            } catch (Exception ignored) {}
        } else if (encoding.contains("duplicate")) {
            // Duplicate action, format: duplicate-Building-ModifyPlayerResource-true
            String[] split = encoding.split("-");
            TMTypes.Tag t = Utils.searchEnum(TMTypes.Tag.class, split[1]);
            effect = new DuplicateImmediateEffect(t, split[2], split[3].equalsIgnoreCase("true"));
        } else if (encoding.contains("look")) {
            // Look at top X cards, keep/buy N cards, discard the rest: look-nLook-nKeep-buy
            String[] split = encoding.split("-");
            int nCardsLook = Integer.parseInt(split[1]);
            int nCardsKeep = Integer.parseInt(split[2]);
            boolean buy = Boolean.parseBoolean(split[3]);
            effect = new TopCardDecision(nCardsLook, nCardsKeep, buy);
        }
        return new Pair<>(effect, effectString);
    }
}

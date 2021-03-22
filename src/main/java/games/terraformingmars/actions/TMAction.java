package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTurnOrder;
import games.terraformingmars.TMTypes;
import games.terraformingmars.rules.effects.Effect;
import games.terraformingmars.rules.requirements.AdjacencyRequirement;
import games.terraformingmars.rules.requirements.Requirement;
import utilities.Pair;
import utilities.Utils;

import java.util.HashMap;
import java.util.Objects;

public class TMAction extends AbstractAction {
    public final boolean free;
    public int player;
    public final boolean pass;

    public Requirement<TMGameState> requirement;
    public boolean played;

    public TMTypes.ActionType actionType;
    public TMTypes.StandardProject standardProject;

    public TMAction(TMTypes.ActionType actionType, int player, boolean free) {
        this.player = player;
        this.free = free;
        this.pass = false;
        this.actionType = actionType;
    }

    public TMAction(TMTypes.StandardProject project, int player, boolean free) {
        this.player = player;
        this.free = free;
        this.pass = false;
        this.actionType = TMTypes.ActionType.StandardProject;
        this.standardProject = project;
    }

    public TMAction(int player) {
        this.player = player;
        this.free = false;
        this.pass = true;
    }

    public TMAction(int player, boolean free) {
        this.player = player;
        this.free = free;
        this.pass = false;
    }

    public TMAction(int player, boolean free, Requirement requirement) {
        this.player = player;
        this.free= free;
        this.pass = false;
        this.requirement = requirement;
    }

    public TMAction(TMTypes.ActionType actionType, int player, boolean free, Requirement requirement) {
        this.player = player;
        this.free= free;
        this.pass = false;
        this.requirement = requirement;
        this.actionType = actionType;
    }

    public TMAction(TMTypes.StandardProject project, int player, boolean free, Requirement requirement) {
        this.player = player;
        this.free= free;
        this.pass = false;
        this.requirement = requirement;
        this.actionType = TMTypes.ActionType.StandardProject;
        this.standardProject = project;
    }

    public boolean canBePlayed(TMGameState gs) {  // TODO: overwrite in subclasses and simplify code
        return !played && (requirement == null || requirement.testCondition(gs));
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();
        if (!free) {
            ((TMTurnOrder)gs.getTurnOrder()).registerActionTaken(gs, this, player);
        }
        played = true;

        // Check persisting effects for all players
        for (int i = 0; i < gs.getNPlayers(); i++) {
            for (Effect e: gs.getPlayerPersistingEffects()[i]) {
                e.execute(gs, this, i);
            }
        }

        // Check if player has Card resources, transform those to cards into hand
        Counter c = gs.getPlayerResources()[player].get(TMTypes.Resource.Card);
        int nCards = c.getValue();
        if (nCards > 0) {
            for (int i = 0; i < nCards; i++) {
                gs.getPlayerHands()[player].add(gs.getProjectCards().pick(0));
            }
            c.setValue(0);
        } else if (nCards < 0) {
            // TODO player needs to discard nCards
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // TODO
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMAction)) return false;
        TMAction tmAction = (TMAction) o;
        return free == tmAction.free && player == tmAction.player && pass == tmAction.pass && played == tmAction.played && Objects.equals(requirement, tmAction.requirement) && actionType == tmAction.actionType && standardProject == tmAction.standardProject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(free, player, pass, requirement, played, actionType, standardProject);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return actionType != null? actionType.name() : "Pass";
    }

    @Override
    public String toString() {
        return actionType != null? actionType.name() : "Pass";
    }

    public static Pair<TMAction, String> parseAction(String encoding, boolean free) {
        return parseAction(encoding, free, -1);
    }

    public static Pair<TMAction, String> parseAction(String encoding, boolean free, int cardID) {

        // Third element is effect
        TMAction effect = null;
        String effectString = "";
        int player = -1;

        if (encoding.contains("inc") || encoding.contains("dec")) {
            // Increase/Decrease counter action
            String[] split2 = encoding.split("-");
            try {
                // Find how much
                Integer increment = null;
                if (!split2[2].equalsIgnoreCase("x")) {
                    increment = Integer.parseInt(split2[2]);
                }
                if (encoding.contains("dec")) increment *= -1;

                effectString = split2[1];

                // Find which counter
                TMTypes.GlobalParameter which = Utils.searchEnum(TMTypes.GlobalParameter.class, split2[1]);

                if (increment == null) {
                    // TODO: handle "dec X to increase X", sub action to be done only when first is done and X decided
                } else {
                    if (which == null) {
                        // A resource or production instead
                        String resString = split2[1].split("prod")[0];
                        TMTypes.Resource res = Utils.searchEnum(TMTypes.Resource.class, resString);
                        int targetPlayer = player;
                        if (split2.length > 3 && split2[3].equalsIgnoreCase("any")) targetPlayer = -2;
                        effect = new PlaceholderModifyCounter(player, targetPlayer, increment, res, split2[1].contains("prod"), free);
                    } else {
                        // A global counter (temp, oxygen, oceantiles)
                        effect = new ModifyGlobalParameter(which, increment, free);
                    }
                }
            } catch (Exception ignored) {}
        } else if (encoding.contains("placetile")) {
            // PlaceTile action
            String[] split2 = encoding.split("/");
            // split2[1] is type of tile to place
            TMTypes.Tile toPlace = Utils.searchEnum(TMTypes.Tile.class, split2[1]);
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
                    } else {
                        boolean respectAdjacency = where == toPlace.getRegularLegalTileType();
                        effect = new PlaceTile(player, toPlace, where, free);  // Extended sequence, will ask player where to put it
                        ((PlaceTile) effect).respectingAdjacency = respectAdjacency;
                    }
                }
            } else {
                // TODO handle "Capital" tile type as City, but special rules
                int a = 0; // TODO this shouldn't happen
            }
            effectString = split2[1];
            if (effect != null && split2.length > 4) {
                // split2[4] = adjacency rule: X tile types separated by -, Owned, None (not placed)
                // Adjacency rules
                AdjacencyRequirement req;
                if (split2[4].equalsIgnoreCase("Owned")) {
                    req = new AdjacencyRequirement();
                    req.owned = true;
                } else if (split2[4].equalsIgnoreCase("None")) {
                    req = new AdjacencyRequirement();
                    req.noneAdjacent = true;
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
            // Add resource to card
            int sign = encoding.contains("rem") ? -1 : 1;
            String[] split2 = encoding.split("-");
            int amount = Integer.parseInt(split2[1]);
            TMTypes.Resource res = Utils.searchEnum(TMTypes.Resource.class, split2[2]);
            effect = new AddResourceOnCard(-1, cardID, res, amount*sign, free);

            if (split2.length > 3) {
                if (split2[3].equalsIgnoreCase("another")) {
                    ((AddResourceOnCard) effect).cardID = -1;
                } else if (split2[3].equalsIgnoreCase("any")) {
                    ((AddResourceOnCard) effect).cardID = -1;
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
            }
        }
        return new Pair<>(effect, effectString);
    }
}

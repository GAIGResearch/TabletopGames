package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import evaluation.metrics.Event;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.*;
import games.root.actions.choosers.ChooseCardForSupporters;
import games.root.actions.choosers.ChooseNumber;
import games.root.components.cards.RootCard;
import games.root.components.Item;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VagabondStrike extends AbstractAction implements IExtendedSequence {

    public final int playerID;

    public VagabondStrike(int playerID) {
        this.playerID = playerID;
    }

    public enum Stage {
        chooseTargetPlayer,
        removePiecesDefender,
        Outrage,
        OutrageWoodland,
    }
    Stage stage = Stage.chooseTargetPlayer;
    boolean done = false;
    int locationID;
    int targetPlayerID;
    int defenderDamage = 1;

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (gs.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond) {
            currentState.increaseActionsPlayed();
            //exhaust crossbow
            for (Item item: currentState.getSatchel()){
                if (item.itemType == Item.ItemType.crossbow && item.refreshed && !item.damaged){
                    item.refreshed = false;
                    break;
                }
            }
            locationID = currentState.getGameMap().getVagabondClearing().getComponentID();
            currentState.setActionInProgress(this);
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState currentState = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        switch (stage) {
            case chooseTargetPlayer -> actions.addAll(getChooseTargetPlayerActions(currentState));
            case removePiecesDefender -> actions.addAll(getTakeDamageAction(currentState));
            case Outrage -> actions.addAll(getOutrageAction(currentState));
            case OutrageWoodland -> actions.addAll(getOutrageWoodlandAction(currentState));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        return switch (stage) {
            case chooseTargetPlayer, Outrage -> playerID;
            case removePiecesDefender -> targetPlayerID;
            case OutrageWoodland -> gs.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance);
        };
    }


    private List<AbstractAction> getChooseTargetPlayerActions(RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        RootBoardNodeWithRootEdges clearing = gs.getGameMap().getNodeByID(locationID);
        for (int i = 0; i < gs.getNPlayers(); i++){
            if (i != playerID && clearing.isAttackable(gs.getPlayerFaction(i))){
                actions.add(new ChooseNumber(playerID,i));
            }
        }
        //can never be empty
        return actions;
    }


    private List<AbstractAction> getTakeDamageAction(RootGameState gs){
        List<AbstractAction> actions = new ArrayList<>();
        RootBoardNodeWithRootEdges clearing = gs.getGameMap().getNodeByID(locationID);
        if (defenderDamage > 0){
            switch (gs.getPlayerFaction(gs.getCurrentPlayer())){
                case MarquiseDeCat:
                    if (clearing.getWarrior(RootParameters.Factions.MarquiseDeCat) > 0){
                        actions.add(new RemoveWarrior(gs.getCurrentPlayer(), locationID));
                    }else {
                        if (clearing.hasBuilding(RootParameters.BuildingType.Recruiter)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, RootParameters.BuildingType.Recruiter, null));
                        }
                        if (clearing.hasBuilding(RootParameters.BuildingType.Workshop)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, RootParameters.BuildingType.Workshop, null));
                        }
                        if (clearing.hasBuilding(RootParameters.BuildingType.Sawmill)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, RootParameters.BuildingType.Sawmill, null));
                        }
                        if (clearing.hasToken(RootParameters.TokenType.Wood)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, null, RootParameters.TokenType.Wood));
                        }
                        if (clearing.hasToken(RootParameters.TokenType.Keep)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, null, RootParameters.TokenType.Keep));
                        }
                    }
                    break;
                case EyrieDynasties:
                    if (clearing.getWarrior(RootParameters.Factions.EyrieDynasties) > 0){
                        actions.add(new RemoveWarrior(gs.getCurrentPlayer(), locationID));
                    }else {
                        if (clearing.hasBuilding(RootParameters.BuildingType.Roost)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, RootParameters.BuildingType.Roost, null));
                        }
                    }
                    break;
                case WoodlandAlliance:
                    if (clearing.getWarrior(RootParameters.Factions.WoodlandAlliance) > 0){
                        actions.add(new RemoveWarrior(gs.getCurrentPlayer(), locationID));
                    }else {
                        if (clearing.hasToken(RootParameters.TokenType.Sympathy)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, null, RootParameters.TokenType.Sympathy));
                        }
                        if (clearing.hasBuilding(RootParameters.BuildingType.FoxBase)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, RootParameters.BuildingType.FoxBase, null));
                        }
                        if (clearing.hasBuilding(RootParameters.BuildingType.MouseBase)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, RootParameters.BuildingType.MouseBase, null));
                        }
                        if (clearing.hasBuilding(RootParameters.BuildingType.RabbitBase)) {
                            actions.add(new TakeHit(gs.getCurrentPlayer(), locationID, RootParameters.BuildingType.RabbitBase, null));
                        }
                    }
                    break;
                case Vagabond:
                    for (Item item: gs.getSatchel()){
                        if(!item.damaged){
                            DamageItem damageAction = new DamageItem(gs.getCurrentPlayer(), item.itemType);
                            if (!actions.contains(damageAction)) actions.add(damageAction);
                        }
                    }
                    for (Item item: gs.getCoins()){
                        if(!item.damaged){
                            DamageItem damageAction = new DamageItem(gs.getCurrentPlayer(), item.itemType);
                            if (!actions.contains(damageAction)) actions.add(damageAction);
                        }
                    }
                    for (Item item: gs.getTeas()){
                        if(!item.damaged){
                            DamageItem damageAction = new DamageItem(gs.getCurrentPlayer(), item.itemType);
                            if (!actions.contains(damageAction)) actions.add(damageAction);
                        }
                    }
                    for (Item item: gs.getBags()){
                        if(!item.damaged){
                            DamageItem damageAction = new DamageItem(gs.getCurrentPlayer(), item.itemType);
                            if (!actions.contains(damageAction)) actions.add(damageAction);
                        }
                    }
                    break;
            }
        }
        if (actions.isEmpty()){
            actions.add(new Pass(gs.getCurrentPlayer(), "cannot remove any more pieces"));
        }
        return actions;
    }

    private List<AbstractAction> getOutrageAction(RootGameState gs){
        List<AbstractAction> actions = new ArrayList<>();
        PartialObservableDeck<RootCard> hand = gs.getPlayerHand(playerID);
        for (int i = 0 ; i < hand.getSize(); i++){
            if (hand.get(i).suit == gs.getGameMap().getNodeByID(locationID).getClearingType() || hand.get(i).suit == RootParameters.ClearingTypes.Bird){
                AddCardToSupporters action = new AddCardToSupporters(playerID, i, hand.get(i).getComponentID());
                if (!actions.contains(action)) actions.add(action);
            }
        }
        if (actions.isEmpty()){
            actions.add(new Pass(playerID, "cannot add card to supporters"));
        }
        return actions;
    }

    private List<AbstractAction> getOutrageWoodlandAction(RootGameState gs){
        List<AbstractAction> actions = new ArrayList<>();
        PartialObservableDeck<RootCard> playerHand = gs.getPlayerHand(playerID);
        for (int i = 0; i < playerHand.getSize(); i++){
            ChooseCardForSupporters action = new ChooseCardForSupporters(gs.getCurrentPlayer(), playerID, i);
            if (!actions.contains(action)) actions.add(action);
        }
        if (actions.isEmpty()){
            actions.add(new Pass(gs.getCurrentPlayer(), "hand is empty"));
        }
        return actions;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        RootGameState gs = (RootGameState) state;
        switch (stage) {
            case chooseTargetPlayer:
                if (action instanceof ChooseNumber ct) {
                    targetPlayerID = ct.number;
                    stage = Stage.removePiecesDefender;
                }
                break;
            case removePiecesDefender:
                if (action instanceof RemoveWarrior){
                    if (gs.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
                        if (gs.getRelationship(gs.getPlayerFaction(targetPlayerID)) == RootParameters.Relationship.Hostile){
                            gs.addGameScorePlayer(playerID, 2);
                        } else{
                            gs.setHostile(gs.getPlayerFaction(targetPlayerID));
                            gs.logEvent(Event.GameEvent.GAME_EVENT, gs.getPlayerFaction(targetPlayerID).toString() + " is now Hostile");
                        }
                    }
                    defenderDamage--;
                } else if (action instanceof TakeHit td) {
                    defenderDamage--;
                    gs.addGameScorePlayer(playerID,1);
                    if (td.tokenType == RootParameters.TokenType.Sympathy){
                        stage = Stage.Outrage;
                        break;
                    }
                } else if (action instanceof DamageItem) {
                    defenderDamage--;
                } else if (action instanceof Pass) {
                    done = true;
                }
                if (defenderDamage==0) {
                    done = true;
                }
            case Outrage:
                if (action instanceof Pass){
                    stage = Stage.OutrageWoodland;
                    ArrayList<boolean[]> handVisibility = new ArrayList<>();
                    for (int i = 0; i <  gs.getPlayerHand(playerID).getSize(); i++) {
                        boolean[] cardVisibility = new boolean[gs.getNPlayers()];
                        cardVisibility[playerID] = true;
                        cardVisibility[gs.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance)] = true;
                        handVisibility.add(cardVisibility);
                    }
                    gs.getPlayerHand(playerID).setVisibility(handVisibility);
                }else{
                    done = true;
                }
                break;
            case OutrageWoodland:
                done = true;
                break;


        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public VagabondStrike copy() {
        VagabondStrike copy = new VagabondStrike(playerID);
        copy.stage = stage;
        copy.targetPlayerID = targetPlayerID;
        copy.done = done;
        copy.defenderDamage = defenderDamage;
        copy.locationID = locationID;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VagabondStrike that)) return false;
        return playerID == that.playerID && done == that.done && locationID == that.locationID && targetPlayerID == that.targetPlayerID && defenderDamage == that.defenderDamage && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, stage, done, locationID, targetPlayerID, defenderDamage);
    }

    @Override
    public String toString() {
        return "p" + playerID + " wants to strike";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to strike";
    }
}

package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import evaluation.metrics.Event;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.*;
import games.root.actions.choosers.ChooseCard;
import games.root.actions.choosers.ChooseCardForSupporters;
import games.root.actions.choosers.ChooseNode;
import games.root.actions.choosers.ChooseNumber;
import games.root.components.cards.RootCard;
import games.root.components.Item;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BattleAction extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    public enum Stage {
        chooseLocation,
        chooseTargetPlayer,
        targetAmbush,
        playerAmbush,
        ambushPlayed,
        Battle,
        optionalModifiersAttacker,
        optionalModifiersDefender,
        removePiecesAttacker,
        removePiecesDefender,
        Outrage,
        OutrageWoodland,
    }

    Stage lastStage;
    Stage stage = Stage.chooseLocation;
    boolean done = false;
    int playerDestroyedSympathyID;
    int locationID;
    int targetPlayerID;
    int attackerDamage = 0;
    int defenderDamage = 0;

    public BattleAction(int playerID) {
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (gs.getCurrentPlayer() == playerID) {
            currentState.increaseActionsPlayed();
            if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
                for (Item item: currentState.getSatchel()){
                    if (item.itemType == Item.ItemType.sword && item.refreshed && !item.damaged){
                        item.refreshed = false;
                        break;
                    }
                }
            }
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
            case chooseLocation -> actions.addAll(getChooseLocationActions(currentState));
            case chooseTargetPlayer -> actions.addAll(getChooseTargetPlayerActions(currentState));
            case targetAmbush -> actions.addAll(getTargetAmbushActions(currentState));
            case playerAmbush -> actions.addAll(getPlayerAmbushActions(currentState));
            case ambushPlayed, removePiecesAttacker -> actions.addAll(getTakeDamageAction(currentState, true));
            case Battle -> actions.add(new BattleRoll(playerID, targetPlayerID, locationID));
            case optionalModifiersAttacker -> actions.addAll(getOptionalModifiersAttacker(playerID, currentState));
            case optionalModifiersDefender -> actions.addAll(getOptionalModifiersDefender(targetPlayerID, currentState));
            case removePiecesDefender -> actions.addAll(getTakeDamageAction(currentState, false));
            case Outrage -> actions.addAll(getOutrageAction(currentState));
            case OutrageWoodland -> actions.addAll(getOutrageWoodlandAction(currentState));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        return switch (stage) {
            case chooseLocation, chooseTargetPlayer, playerAmbush, ambushPlayed, Battle, optionalModifiersAttacker, removePiecesAttacker ->
                    playerID;
            case targetAmbush, optionalModifiersDefender, removePiecesDefender -> targetPlayerID;
            case Outrage -> playerDestroyedSympathyID;
            case OutrageWoodland -> gs.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance);
        };
    }

    private List<AbstractAction> getChooseLocationActions(RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        if (gs.getPlayerFaction(gs.getCurrentPlayer())!= RootParameters.Factions.EyrieDynasties) {
            for (RootBoardNodeWithRootEdges clearing : gs.getGameMap().getNonForrestBoardNodes()) {
                boolean canAttack = false;
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    if (i != playerID && clearing.isAttackable(gs.getPlayerFaction(i))) {
                        canAttack = true;
                    }
                }
                if (clearing.getWarrior(gs.getPlayerFaction(playerID)) > 0 && canAttack) {
                    ChooseNode action = new ChooseNode(playerID, clearing.getComponentID());
                    if (!actions.contains(action)) actions.add(action);
                }
            }
        } else {
            List<RootParameters.ClearingTypes> available = gs.getDecreeSuits(2);
            for (RootParameters.ClearingTypes clearingType : available) {
                for (RootBoardNodeWithRootEdges node : gs.getGameMap().getNodesOfType(clearingType)) {
                    boolean canAttack = false;
                    for (int i = 0; i < gs.getNPlayers(); i++) {
                        if (i != playerID && node.isAttackable(gs.getPlayerFaction(i))) {
                            canAttack = true;
                        }
                    }
                    if (node.getWarrior(gs.getPlayerFaction(playerID)) > 0 && canAttack) {
                        ChooseNode action = new ChooseNode(playerID, node.getComponentID(), clearingType== RootParameters.ClearingTypes.Bird);
                        if (!actions.contains(action)) actions.add(action);
                    }
                }
            }
        }
        //should never be empty
        return actions;
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

    private List<AbstractAction> getTargetAmbushActions(RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        Deck<RootCard> defenderHand = gs.getPlayerHand(targetPlayerID);
        Deck<RootCard> attackerCraftedCards = gs.getPlayerCraftedCards(playerID);
        boolean isAmbushable = true;
        for (int e = 0; e < attackerCraftedCards.getSize(); e++ ){
            if (attackerCraftedCards.get(e).cardType == RootCard.CardType.ScoutingParty){
                isAmbushable = false;
            }
        }
        RootBoardNodeWithRootEdges clearing = gs.getGameMap().getNodeByID(locationID);
        if (isAmbushable) {
            for (int i = 0; i < defenderHand.getSize(); i++) {
                if (defenderHand.get(i).cardType == RootCard.CardType.Ambush && (defenderHand.get(i).suit == clearing.getClearingType() || defenderHand.get(i).suit == RootParameters.ClearingTypes.Bird)) {
                    PlayAmbush action = new PlayAmbush(targetPlayerID, i, defenderHand.get(i).getComponentID());
                    actions.add(action);
                }
            }
        }
        //can always pass
        Pass pass = new Pass(targetPlayerID);
        actions.add(pass);
        return actions;
    }

    private List<AbstractAction> getPlayerAmbushActions(RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        Deck<RootCard> attackerHand = gs.getPlayerHand(playerID);
        RootBoardNodeWithRootEdges clearing = gs.getGameMap().getNodeByID(locationID);
        for (int i = 0 ; i < attackerHand.getSize(); i++){
            if(attackerHand.get(i).cardType == RootCard.CardType.Ambush && (attackerHand.get(i).suit == clearing.getClearingType() || attackerHand.get(i).suit == RootParameters.ClearingTypes.Bird)){
                PlayAmbush action = new PlayAmbush(playerID, i, attackerHand.get(i).getComponentID());
                actions.add(action);
            }
        }
        Pass reactionPass = new Pass(playerID);
        actions.add(reactionPass);
        return actions;
    }

    private List<AbstractAction> getTakeDamageAction(RootGameState gs, boolean attacker){
        List<AbstractAction> actions = new ArrayList<>();
        RootBoardNodeWithRootEdges clearing = gs.getGameMap().getNodeByID(locationID);
        int damage = attacker ? attackerDamage : defenderDamage;
        if (damage > 0){
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
            actions.add(new Pass(gs.getCurrentPlayer()));
        }
        return actions;
    }

    private List<AbstractAction> getOptionalModifiersAttacker(int player, RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        Deck<RootCard> craftedCardsAttacker = gs.getPlayerCraftedCards(playerID);
        for (int i = 0; i < craftedCardsAttacker.getSize(); i++){
            if (craftedCardsAttacker.get(i).cardType == RootCard.CardType.Armorers){
                //ignore all rolled hits
                actions.add(new DiscardCraftedCard(playerID, i, craftedCardsAttacker.get(i).getComponentID()));
            } else if (craftedCardsAttacker.get(i).cardType == RootCard.CardType.BrutalTactics){
                actions.add(new ChooseCard(playerID, i, craftedCardsAttacker.get(i).getComponentID()));
            }
        }
        actions.add(new Pass(player));
        return actions;
    }

    private List<AbstractAction> getOptionalModifiersDefender(int player, RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        Deck<RootCard> craftedCardsDefender = gs.getPlayerCraftedCards(targetPlayerID);
        for (int i = 0; i < craftedCardsDefender.getSize(); i++){
            if (craftedCardsDefender.get(i).cardType == RootCard.CardType.Armorers){
                //ignore all rolled hits
                actions.add(new DiscardCraftedCard(playerID, i, craftedCardsDefender.get(i).getComponentID()));
            } else if (craftedCardsDefender.get(i).cardType == RootCard.CardType.Sappers) {
                actions.add(new DiscardCraftedCard(playerID, i, craftedCardsDefender.get(i).getComponentID()));
            }
        }
        actions.add(new Pass(player));
        return actions;
    }

    private List<AbstractAction> getOutrageAction(RootGameState gs){
        List<AbstractAction> actions = new ArrayList<>();
        PartialObservableDeck<RootCard> hand = gs.getPlayerHand(playerDestroyedSympathyID);
        for (int i = 0 ; i < hand.getSize(); i++){
            if (hand.get(i).suit == gs.getGameMap().getNodeByID(locationID).getClearingType() || hand.get(i).suit == RootParameters.ClearingTypes.Bird){
                AddCardToSupporters action = new AddCardToSupporters(playerID, i, hand.get(i).getComponentID());
                if (!actions.contains(action)) actions.add(action);
            }
        }
        if (actions.isEmpty()){
            actions.add(new Pass(playerID));
        }
        return actions;
    }

    private List<AbstractAction> getOutrageWoodlandAction(RootGameState gs){
        List<AbstractAction> actions = new ArrayList<>();
        PartialObservableDeck<RootCard> playerHand = gs.getPlayerHand(playerDestroyedSympathyID);
        for (int i = 0; i < playerHand.getSize(); i++){
            ChooseCardForSupporters action = new ChooseCardForSupporters(gs.getCurrentPlayer(), playerID, i);
            if(!actions.contains(action)) actions.add(action);
        }
        if (actions.isEmpty()){
            actions.add(new Pass(gs.getCurrentPlayer()));
        }
        return actions;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        RootGameState gs = (RootGameState) state;
        switch (stage) {
            case chooseLocation:
                if (action instanceof ChooseNode cn) {
                    locationID = cn.nodeID;
                    lastStage = Stage.chooseLocation;
                    stage = Stage.chooseTargetPlayer;
                    if (gs.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties){
                        if (cn.birdPlayed){
                            gs.addPlayedSuit(RootParameters.ClearingTypes.Bird);
                        }else {
                            gs.addPlayedSuit(gs.getGameMap().getNodeByID(locationID).getClearingType());
                        }
                    }
                }
                break;
            case chooseTargetPlayer:
                if (action instanceof ChooseNumber ct) {
                    targetPlayerID = ct.number;
                    lastStage = Stage.chooseTargetPlayer;
                    stage = Stage.targetAmbush;
                }
                break;
            case targetAmbush:
                if (action instanceof PlayAmbush ignored1) {
                    attackerDamage = 2;
                    lastStage = Stage.targetAmbush;
                    stage = Stage.playerAmbush;
                } else {
                    lastStage = Stage.targetAmbush;
                    stage = Stage.Battle;
                }
                break;
            case playerAmbush:
                if (action instanceof PlayAmbush ignored) {
                    attackerDamage = 0;
                    lastStage = Stage.playerAmbush;
                    stage = Stage.Battle;
                } else {
                    lastStage = Stage.playerAmbush;
                    stage = Stage.ambushPlayed;
                }
                break;
            case ambushPlayed:
                if (action instanceof RemoveWarrior) {
                    if (gs.getPlayerFaction(targetPlayerID) == RootParameters.Factions.Vagabond){
                        if (gs.getRelationship(gs.getPlayerFaction(playerID)) == RootParameters.Relationship.Hostile){
                            gs.addGameScorePlayer(targetPlayerID, 2);
                        } else{
                            gs.setHostile(gs.getPlayerFaction(playerID));
                            gs.logEvent(Event.GameEvent.GAME_EVENT, gs.getPlayerFaction(playerID).toString() + " is now Hostile");
                        }
                    }
                    attackerDamage--;
                } else if (action instanceof TakeHit th) {
                    gs.addGameScorePlayer(targetPlayerID,1);
                    attackerDamage--;
                    if (th.tokenType == RootParameters.TokenType.Sympathy) {
                        lastStage = Stage.ambushPlayed;
                        playerDestroyedSympathyID = targetPlayerID;
                        stage = Stage.Outrage;
                    }
                } else if (action instanceof Pass) {
                    done = true; // happens when player has no more pieces to remove
                } else if (action instanceof DamageItem) {
                    attackerDamage--;
                }
                if (attackerDamage == 0 && gs.getGameMap().getNodeByID(locationID).getWarrior(gs.getPlayerFaction(playerID))>0){
                    lastStage = Stage.ambushPlayed;
                    stage = Stage.Battle;
                }
                break;
            case Battle:
                if (action instanceof BattleRoll br){
                    attackerDamage = br.attackerDamage;
                    defenderDamage = br.defenderDamage;
                    lastStage = Stage.Battle;
                    stage = Stage.optionalModifiersAttacker;
                }
                break;
            case optionalModifiersAttacker:
                if (action instanceof Pass) {
                    lastStage = Stage.optionalModifiersAttacker;
                    stage = Stage.optionalModifiersDefender;
                } else if (action instanceof DiscardCraftedCard dc){
                    RootCard card = (RootCard) gs.getComponentById(dc.cardId);
                    if (card.cardType == RootCard.CardType.Armorers){
                        attackerDamage = 0;
                        lastStage = Stage.optionalModifiersAttacker;
                        stage = Stage.optionalModifiersDefender;
                    }
                } else if (action instanceof ChooseCard) {
                    defenderDamage++;
                    gs.addGameScorePlayer(targetPlayerID,1);
                    lastStage = Stage.optionalModifiersAttacker;
                    stage = Stage.optionalModifiersDefender;
                }
                break;
            case optionalModifiersDefender:
                if (action instanceof Pass) {
                    lastStage = Stage.optionalModifiersDefender;
                    stage = Stage.removePiecesAttacker;
                } else if (action instanceof DiscardCraftedCard dc){
                    RootCard card = (RootCard) gs.getComponentById(dc.cardId);
                    if (card.cardType == RootCard.CardType.Armorers){
                        defenderDamage = 0;
                        lastStage = Stage.optionalModifiersAttacker;
                        stage = Stage.optionalModifiersDefender;
                    } else if (card.cardType == RootCard.CardType.Sappers) {
                        attackerDamage ++;
                        lastStage = Stage.optionalModifiersAttacker;
                        stage = Stage.optionalModifiersDefender;
                    }
                }
                break;
            case removePiecesAttacker:
                if (action instanceof RemoveWarrior){
                    attackerDamage--;
                    if (gs.getPlayerFaction(targetPlayerID) == RootParameters.Factions.Vagabond){
                        if (gs.getRelationship(gs.getPlayerFaction(playerID)) == RootParameters.Relationship.Hostile){
                            gs.addGameScorePlayer(targetPlayerID, 2);
                        } else{
                            gs.setHostile(gs.getPlayerFaction(playerID));
                            gs.logEvent(Event.GameEvent.GAME_EVENT, gs.getPlayerFaction(playerID).toString() + " is now Hostile");
                        }
                    }
                }else if (action instanceof TakeHit ta){
                    attackerDamage--;
                    gs.addGameScorePlayer(targetPlayerID,1);
                    if (ta.tokenType == RootParameters.TokenType.Sympathy){
                        playerDestroyedSympathyID = targetPlayerID;
                        lastStage = Stage.removePiecesAttacker;
                        stage = Stage.Outrage;
                        break;
                    }
                } else if (action instanceof DamageItem) {
                    attackerDamage--;
                } else if (action instanceof Pass) {
                    //attacker cannot remove any more pieces
                    lastStage = Stage.removePiecesAttacker;
                    stage = Stage.removePiecesDefender;
                }
                if (attackerDamage == 0){
                    stage = Stage.removePiecesDefender;
                    lastStage = Stage.removePiecesDefender;
                }
                break;
            case removePiecesDefender:
                if (action instanceof RemoveWarrior){
                    defenderDamage--;
                    if (gs.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
                        if (gs.getRelationship(gs.getPlayerFaction(targetPlayerID)) == RootParameters.Relationship.Hostile){
                            gs.addGameScorePlayer(playerID, 2);
                        } else{
                            gs.setHostile(gs.getPlayerFaction(targetPlayerID));
                            gs.logEvent(Event.GameEvent.GAME_EVENT, gs.getPlayerFaction(targetPlayerID).toString() + " is now Hostile");
                        }
                    }
                } else if (action instanceof TakeHit td) {
                    defenderDamage--;
                    gs.addGameScorePlayer(playerID,1);
                    if (td.tokenType == RootParameters.TokenType.Sympathy){
                        playerDestroyedSympathyID = playerID;
                        lastStage = Stage.removePiecesDefender;
                        stage = Stage.Outrage;
                        break;
                    }
                } else if (action instanceof DamageItem) {
                    defenderDamage--;
                } else if (action instanceof Pass) {
                    done = true;
                    lastStage = Stage.removePiecesDefender;
                }
                if (defenderDamage==0) {
                    done = true;
                }
            case Outrage:
                if (action instanceof Pass){
                    stage = Stage.OutrageWoodland;
                    ArrayList<boolean[]> handVisibility = new ArrayList<>();
                    for (int i = 0; i <  gs.getPlayerHand(playerDestroyedSympathyID).getSize(); i++) {
                        boolean[] cardVisibility = new boolean[gs.getNPlayers()];
                        cardVisibility[playerDestroyedSympathyID] = true;
                        cardVisibility[gs.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance)] = true;
                        handVisibility.add(cardVisibility);
                    }
                    gs.getPlayerHand(playerDestroyedSympathyID).setVisibility(handVisibility);
                }else{
                    stage = lastStage;
                }
                break;
            case OutrageWoodland:
                stage = lastStage;
                break;


        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public BattleAction copy() {
        BattleAction copy = new BattleAction(playerID);
        copy.stage = stage;
        copy.playerDestroyedSympathyID = playerDestroyedSympathyID;
        copy.targetPlayerID = targetPlayerID;
        copy.done = done;
        copy.attackerDamage = attackerDamage;
        copy.defenderDamage = defenderDamage;
        copy.locationID = locationID;
        copy.lastStage = lastStage;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BattleAction that)) return false;
        return playerID == that.playerID && done == that.done && playerDestroyedSympathyID == that.playerDestroyedSympathyID && locationID == that.locationID && targetPlayerID == that.targetPlayerID && attackerDamage == that.attackerDamage && defenderDamage == that.defenderDamage && lastStage == that.lastStage && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, lastStage, stage, done, playerDestroyedSympathyID, locationID, targetPlayerID, attackerDamage, defenderDamage);
    }

    @Override
    public String toString() {
        return "p" + playerID + " wants to battle";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to battle";
    }
}

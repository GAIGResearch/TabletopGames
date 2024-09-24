package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VagabondCrafting extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public List<RootParameters.ClearingTypes> available;
    public List<RootParameters.ClearingTypes> craftingCost = new ArrayList<>();

    public enum Stage{
        chooseCard,
        exhaust,
        craft,
    }

    public RootCard card;

    public Stage stage = Stage.chooseCard;
    public boolean done = false;

    public VagabondCrafting(int playerID, List<RootParameters.ClearingTypes> available){
        this.playerID = playerID;
        this.available = new ArrayList<>();
        this.available.addAll(available);
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        if (gs.getCurrentPlayer() == playerID){
            gs.setActionInProgress(this);
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        if (stage == Stage.chooseCard){
            PartialObservableDeck<RootCard> hand = gs.getPlayerHand(playerID);
            for (int i = 0 ; i < hand.getSize(); i++){
                if (hand.get(i).craftingType!= RootCard.CraftingType.unCraftable && gs.canCraftCard(available, hand.get(i))){
                    actions.add(new ChooseCard(playerID, hand.get(i)));
                }
            }
            actions.add(new Pass(playerID, "does not craft anymore"));
            return actions;
        } else if (stage == Stage.exhaust) {
            for (RootParameters.ClearingTypes needed: craftingCost){
                if (needed != RootParameters.ClearingTypes.Bird){
                    ExhaustHammerForCrafting action = new ExhaustHammerForCrafting(playerID, needed, needed);
                    if (!actions.contains(action)) {
                        actions.add(action);
                    }
                }
            }
            if (actions.isEmpty() && !craftingCost.isEmpty()){
                for (RootParameters.ClearingTypes needed: craftingCost){
                    if (needed == RootParameters.ClearingTypes.Bird){
                        for (RootParameters.ClearingTypes ready: available) {
                            ExhaustHammerForCrafting action = new ExhaustHammerForCrafting(playerID, ready, needed);
                            if (!actions.contains(action)) {
                                actions.add(action);
                            }
                        }
                    }
                }
            }
            return actions;
        } else if (stage == Stage.craft) {
            //Discard cards play action
            switch (card.craftingType){
                case itemCard -> actions.add(new VagabondCraftItem(playerID, card.getCraftableItem(), card));
                case craftedCard -> actions.add(new CraftCard(playerID, card));
                case immediateDiscard -> actions.add(new Discard(playerID, card, false));
            }
            return actions;
            //Item cards retrieve item
            //Craftable cards get added to players craftedCards deck
        }
        return null;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        RootGameState gs = (RootGameState) state;
        if (action instanceof ChooseCard cc){
            card = cc.card;
            stage = Stage.exhaust;
            craftingCost.addAll(cc.card.craftingCost);
        } else if (action instanceof ExhaustHammerForCrafting c){
            available.remove(c.activate);
            craftingCost.remove(c.actual);
            if (craftingCost.isEmpty()){
                stage = Stage.craft;
            }
        } else if (stage == Stage.craft){
            if (action instanceof Discard d){
                //resolve discard effect
                if (d.card.cardtype == RootCard.CardType.FavorOfTheFoxes){
                    resolveFavorCard(gs, RootParameters.ClearingTypes.Fox);
                } else if (d.card.cardtype == RootCard.CardType.FavorOfTheMice) {
                    resolveFavorCard(gs, RootParameters.ClearingTypes.Mouse);
                } else if (d.card.cardtype == RootCard.CardType.FavorOfTheRabbits){
                    resolveFavorCard(gs, RootParameters.ClearingTypes.Rabbit);
                }
            }
            if (gs.canCraft(playerID, available)){
                stage = Stage.chooseCard;
            } else {
                done = true;
            }
        } else if (action instanceof Pass){
            done = true;
        }
    }

    private void resolveFavorCard(RootGameState gs, RootParameters.ClearingTypes clearingType){
        for (RootBoardNodeWithRootEdges clearing: gs.getGameMap().getNodesOfType(RootParameters.ClearingTypes.Fox)){
            if (gs.getPlayerFaction(playerID) != RootParameters.Factions.MarquiseDeCat){
                for (int warriorRemoval = 0; warriorRemoval < clearing.getWarrior(RootParameters.Factions.MarquiseDeCat); warriorRemoval++){
                    clearing.removeWarrior(RootParameters.Factions.MarquiseDeCat);
                    gs.addWarrior(RootParameters.Factions.MarquiseDeCat);
                }
                for (int sawmillCount = 0; sawmillCount < clearing.getSawmill(); sawmillCount++){
                    clearing.removeBuilding(RootParameters.BuildingType.Sawmill);
                    gs.addBuilding(RootParameters.BuildingType.Sawmill);
                    gs.addGameScorePLayer(playerID,1);
                }
                for (int workshopCount = 0 ; workshopCount < clearing.getWorkshops(); workshopCount++){
                    clearing.removeBuilding(RootParameters.BuildingType.Workshop);
                    gs.addBuilding(RootParameters.BuildingType.Workshop);
                    gs.addGameScorePLayer(playerID,1);
                }
                for (int recruiterCount = 0; recruiterCount < clearing.getRecruiters(); recruiterCount++){
                    clearing.removeBuilding(RootParameters.BuildingType.Recruiter);
                    gs.addBuilding(RootParameters.BuildingType.Recruiter);
                    gs.addGameScorePLayer(playerID,1);
                }
                for (int woodCount = 0; woodCount < clearing.getWood(); woodCount++){
                    clearing.removeToken(RootParameters.TokenType.Wood);
                    gs.addToken(RootParameters.TokenType.Wood);
                    gs.addGameScorePLayer(playerID, 1);
                }
                if (clearing.hasToken(RootParameters.TokenType.Keep)){
                    clearing.removeToken(RootParameters.TokenType.Keep);
                    gs.addToken(RootParameters.TokenType.Keep);
                    gs.addGameScorePLayer(playerID,1);
                }
            }
            if (gs.getPlayerFaction(playerID) != RootParameters.Factions.EyrieDynasties){
                for (int warriors= 0; warriors < clearing.getWarrior(RootParameters.Factions.EyrieDynasties); warriors++){
                    clearing.removeWarrior(RootParameters.Factions.EyrieDynasties);
                    gs.addWarrior(RootParameters.Factions.EyrieDynasties);
                }
                if (clearing.hasBuilding(RootParameters.BuildingType.Roost)){
                    clearing.removeBuilding(RootParameters.BuildingType.Roost);
                    gs.addBuilding(RootParameters.BuildingType.Roost);
                    gs.addGameScorePLayer(playerID,1);
                }
            }
            if (gs.getPlayerFaction(playerID) != RootParameters.Factions.WoodlandAlliance){
                for (int warriors = 0; warriors < clearing.getWarrior(RootParameters.Factions.WoodlandAlliance); warriors++){
                    clearing.removeWarrior(RootParameters.Factions.WoodlandAlliance);
                    gs.addWarrior(RootParameters.Factions.WoodlandAlliance);
                }
                if (clearing.hasToken(RootParameters.TokenType.Sympathy)){
                    clearing.removeToken(RootParameters.TokenType.Sympathy);
                    gs.addToken(RootParameters.TokenType.Sympathy);
                    gs.addGameScorePLayer(playerID, 1);
                }
                if (clearing.hasBuilding(RootParameters.BuildingType.MouseBase)){
                    clearing.removeBuilding(RootParameters.BuildingType.MouseBase);
                    gs.addBuilding(RootParameters.BuildingType.MouseBase);
                    gs.addGameScorePLayer(playerID, 1);
                }
                if (clearing.hasBuilding(RootParameters.BuildingType.FoxBase)){
                    clearing.removeBuilding(RootParameters.BuildingType.FoxBase);
                    gs.addBuilding(RootParameters.BuildingType.FoxBase);
                    gs.addGameScorePLayer(playerID, 1);
                }
                if (clearing.hasBuilding(RootParameters.BuildingType.RabbitBase)){
                    clearing.removeBuilding(RootParameters.BuildingType.RabbitBase);
                    gs.addBuilding(RootParameters.BuildingType.RabbitBase);
                    gs.addGameScorePLayer(playerID, 1);
                }
            }
            if (gs.getPlayerFaction(playerID) != RootParameters.Factions.Vagabond){
                //nothing
            }
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public VagabondCrafting copy() {
        VagabondCrafting copy = new VagabondCrafting(playerID, available);
        copy.card = card;
        copy.stage = stage;
        copy.done = done;
        copy.craftingCost.addAll(craftingCost);
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof VagabondCrafting cs){
            return playerID == cs.playerID && available.equals(cs.available) && stage == cs.stage && done == cs.done && card == cs.card && craftingCost.equals(cs.craftingCost);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("VagabondCrafting", playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " want to craft";
    }
}

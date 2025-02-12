package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import evaluation.metrics.Event;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.CraftCard;
import games.root.actions.CraftItem;
import games.root.actions.Discard;
import games.root.actions.Pass;
import games.root.actions.choosers.ChooseCard;
import games.root.actions.choosers.ChooseCraftersToActivate;
import games.root.components.cards.RootCard;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CraftSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    List<RootParameters.ClearingTypes> available;
    List<RootParameters.ClearingTypes> craftingCost = new ArrayList<>();

    public enum Stage{
        chooseCard,
        activateCrafters,
        craft,
    }

    int craftedCards = 0;
    Stage stage = Stage.chooseCard;
    boolean done = false;
    int cardId = -1, cardIdx = -1;

    public CraftSequence(int playerID, List<RootParameters.ClearingTypes> available){
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
                    actions.add(new ChooseCard(playerID, i, hand.get(i).getComponentID()));
                }
            }
            if (craftedCards > 0) {
                actions.add(new Pass(playerID, "does not craft anymore"));
            }
            return actions;
        } else if (stage == Stage.activateCrafters) {
            for (RootParameters.ClearingTypes needed: craftingCost){
                if (needed != RootParameters.ClearingTypes.Bird){
                    ChooseCraftersToActivate action = new ChooseCraftersToActivate(playerID, needed, needed);
                    if (!actions.contains(action)) {
                        actions.add(action);
                    }
                }
            }
            if (actions.isEmpty() && !craftingCost.isEmpty()){
                for (RootParameters.ClearingTypes needed: craftingCost){
                    if (needed == RootParameters.ClearingTypes.Bird){
                        for (RootParameters.ClearingTypes ready: available) {
                            ChooseCraftersToActivate action = new ChooseCraftersToActivate(playerID, ready, needed);
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
            RootCard card = (RootCard) gs.getComponentById(cardId);
            switch (card.craftingType){
                case itemCard -> actions.add(new CraftItem(playerID, card.getCraftableItem(), cardIdx, cardId));
                case craftedCard -> actions.add(new CraftCard(playerID, cardIdx, cardId));
                case immediateDiscard -> actions.add(new Discard(playerID, cardIdx, cardId, false));
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
            cardId = cc.cardId;
            cardIdx = cc.cardIdx;
            RootCard card = (RootCard) gs.getComponentById(cardId);
            stage = Stage.activateCrafters;
            craftingCost.addAll(card.craftingCost);
            craftedCards++;
        } else if (action instanceof ChooseCraftersToActivate c){
            available.remove(c.activate);
            craftingCost.remove(c.actual);
            if (craftingCost.isEmpty()){
                stage = Stage.craft;
            }
        } else if (stage == Stage.craft){
            if (action instanceof Discard d){
                RootCard card = (RootCard) gs.getComponentById(d.cardId);
                //resolve discard effect
                if (card.cardType == RootCard.CardType.FavorOfTheFoxes){
                    gs.logEvent(Event.GameEvent.GAME_EVENT, "favor of the foxes used");
                    resolveFavorCard(gs, RootParameters.ClearingTypes.Fox);
                } else if (card.cardType == RootCard.CardType.FavorOfTheMice) {
                    gs.logEvent(Event.GameEvent.GAME_EVENT, "favor of the mice used");
                    resolveFavorCard(gs, RootParameters.ClearingTypes.Mouse);
                } else if (card.cardType == RootCard.CardType.FavorOfTheRabbits){
                    gs.logEvent(Event.GameEvent.GAME_EVENT, "favor of the rabbits used");
                    resolveFavorCard(gs, RootParameters.ClearingTypes.Rabbit);
                }
            }
            if (gs.canCraft(playerID, available)){
                stage = Stage.chooseCard;
            } else {
                gs.increaseSubGamePhase();
                done = true;
            }
        } else if (action instanceof Pass){
            done = true;
        }
    }

    private void resolveFavorCard(RootGameState gs, RootParameters.ClearingTypes clearingType){
        for (RootBoardNodeWithRootEdges clearing: gs.getGameMap().getNodesOfType(clearingType)){
            if (gs.getPlayerFaction(playerID) != RootParameters.Factions.MarquiseDeCat){
                for (int warriorRemoval = 0; warriorRemoval < clearing.getWarrior(RootParameters.Factions.MarquiseDeCat); warriorRemoval++){
                    clearing.removeWarrior(RootParameters.Factions.MarquiseDeCat);
                    gs.addWarrior(RootParameters.Factions.MarquiseDeCat);
                }
                for (int sawmillCount = 0; sawmillCount < clearing.getSawmill(); sawmillCount++){
                    clearing.removeBuilding(RootParameters.BuildingType.Sawmill);
                    gs.addBuilding(RootParameters.BuildingType.Sawmill);
                    gs.addGameScorePlayer(playerID,1);
                }
                for (int workshopCount = 0 ; workshopCount < clearing.getWorkshops(); workshopCount++){
                    clearing.removeBuilding(RootParameters.BuildingType.Workshop);
                    gs.addBuilding(RootParameters.BuildingType.Workshop);
                    gs.addGameScorePlayer(playerID,1);
                }
                for (int recruiterCount = 0; recruiterCount < clearing.getRecruiters(); recruiterCount++){
                    clearing.removeBuilding(RootParameters.BuildingType.Recruiter);
                    gs.addBuilding(RootParameters.BuildingType.Recruiter);
                    gs.addGameScorePlayer(playerID,1);
                }
                for (int woodCount = 0; woodCount < clearing.getWood(); woodCount++){
                    clearing.removeToken(RootParameters.TokenType.Wood);
                    gs.addToken(RootParameters.TokenType.Wood);
                    gs.addGameScorePlayer(playerID, 1);
                }
                if (clearing.hasToken(RootParameters.TokenType.Keep)){
                    clearing.removeToken(RootParameters.TokenType.Keep);
                    gs.addToken(RootParameters.TokenType.Keep);
                    gs.addGameScorePlayer(playerID,1);
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
                    gs.addGameScorePlayer(playerID,1);
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
                    gs.addGameScorePlayer(playerID, 1);
                }
                if (clearing.hasBuilding(RootParameters.BuildingType.MouseBase)){
                    clearing.removeBuilding(RootParameters.BuildingType.MouseBase);
                    gs.addBuilding(RootParameters.BuildingType.MouseBase);
                    gs.addGameScorePlayer(playerID, 1);
                }
                if (clearing.hasBuilding(RootParameters.BuildingType.FoxBase)){
                    clearing.removeBuilding(RootParameters.BuildingType.FoxBase);
                    gs.addBuilding(RootParameters.BuildingType.FoxBase);
                    gs.addGameScorePlayer(playerID, 1);
                }
                if (clearing.hasBuilding(RootParameters.BuildingType.RabbitBase)){
                    clearing.removeBuilding(RootParameters.BuildingType.RabbitBase);
                    gs.addBuilding(RootParameters.BuildingType.RabbitBase);
                    gs.addGameScorePlayer(playerID, 1);
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
    public CraftSequence copy() {
        CraftSequence copy = new CraftSequence(playerID, available);
        copy.cardId = cardId;
        copy.cardIdx = cardIdx;
        copy.stage = stage;
        copy.done = done;
        copy.craftingCost.addAll(craftingCost);
        copy.craftedCards = craftedCards;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CraftSequence that)) return false;
        return playerID == that.playerID && craftedCards == that.craftedCards && done == that.done && cardId == that.cardId && cardIdx == that.cardIdx && Objects.equals(available, that.available) && Objects.equals(craftingCost, that.craftingCost) && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, available, craftingCost, craftedCards, stage, done, cardId, cardIdx);
    }

    @Override
    public String toString() {
        return "p" + playerID + " wants to craft";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " want to craft";
    }
}

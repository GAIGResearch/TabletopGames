package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import evaluation.metrics.Event;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.EyrieRulers;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class BattleRoll extends AbstractAction {
    public final int attackerID;
    public final int defenderID;
    public final int locationID;

    public int attackerDamage = 0;
    public int defenderDamage = 0;
    public int attackerRoll = 0;
    public int defenderRoll = 0;

    public BattleRoll(int attackerID, int defenderID, int locationID){
        this.attackerID = attackerID;
        this.defenderID = defenderID;
        this.locationID = locationID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        RootBoardNodeWithRootEdges clearing = currentState.getGameMap().getNodeByID(locationID);
        if (currentState.getCurrentPlayer() == attackerID && clearing.getWarrior(currentState.getPlayerFaction(attackerID)) > 0){
            int firstRoll = currentState.getRnd().nextInt(4);
            int secondRoll = currentState.getRnd().nextInt(4);
            if (currentState.getPlayerFaction(defenderID) != RootParameters.Factions.WoodlandAlliance){
                attackerRoll = Math.max(firstRoll, secondRoll);
            }else {
                attackerRoll = Math.min(firstRoll, secondRoll);
            }
            defenderRoll = Math.min(firstRoll, secondRoll);
            if (currentState.getPlayerFaction(attackerID) == RootParameters.Factions.Vagabond){
                defenderDamage = Math.min(attackerRoll, currentState.getVagabondUndamagedSwords());
            } else {
                defenderDamage = Math.min(attackerRoll, clearing.getWarrior(currentState.getPlayerFaction(attackerID)));
            }
            if (currentState.getPlayerFaction(defenderID) == RootParameters.Factions.Vagabond){
                attackerDamage = Math.min(defenderRoll, currentState.getVagabondUndamagedSwords());
            } else {
                attackerDamage = Math.min(defenderRoll, clearing.getWarrior(currentState.getPlayerFaction(defenderID)));
            }
            if (clearing.getWarrior(currentState.getPlayerFaction(defenderID)) == 0){
                defenderDamage++;
            }
            if (currentState.getPlayerFaction(attackerID) == RootParameters.Factions.EyrieDynasties && currentState.getRuler().ruler == EyrieRulers.CardType.Commander){
                defenderDamage++;
            }
            currentState.logEvent(Event.GameEvent.GAME_EVENT, currentState.getPlayerFaction(attackerID).toString() + " rolled " + attackerRoll + " and is able to deal " + defenderDamage + " hits, " + currentState.getPlayerFaction(defenderID) + " rolled " + defenderRoll + " and is able to deal " + attackerDamage + "hits");
            return true;
        }
        return false;
    }

    @Override
    public BattleRoll copy() {
        BattleRoll copy = new BattleRoll(attackerID, defenderID, locationID);
        copy.attackerRoll = attackerRoll;
        copy.defenderRoll = defenderRoll;
        copy.attackerDamage = attackerDamage;
        copy.defenderDamage =defenderDamage;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof BattleRoll br){
            return attackerID == br.attackerID && defenderID == br.defenderID && locationID == br.locationID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("BattleRoll", attackerID, defenderID, locationID);
    }

    @Override
    public String toString() {
        return "p" + attackerID + " rolls for battle";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(attackerID).toString() + " rolls for battle";
    }
}

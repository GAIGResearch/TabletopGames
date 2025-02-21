package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class Revolt extends AbstractAction {
    public final int playerID;
    public final int locationID;
    public final int cardToDiscard1Idx, cardToDiscard1Id;
    public final int cardToDiscard2Idx, cardToDiscard2Id;
    public final boolean passSubGamePhase;

    public Revolt(int playerID, int location, int cardToDiscard1Idx, int cardToDiscard1Id, int cardToDiscard2Idx, int cardToDiscard2Id, boolean passSubGamePhase) {
        this.playerID = playerID;
        this.locationID = location;
        this.cardToDiscard1Idx = cardToDiscard1Idx;
        this.cardToDiscard1Id = cardToDiscard1Id;
        this.cardToDiscard2Idx = cardToDiscard2Idx;
        this.cardToDiscard2Id = cardToDiscard2Id;
        this.passSubGamePhase = passSubGamePhase;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if (!canExecute(state)) {
            System.out.println("Action cannot be executed because the current player does not correspond to the woodland alliance faction.");
            return false;
        }

        Deck<RootCard> hand = state.getSupporters();
        if (!attemptToDiscard(hand, state)) {
            return false;
        }

        RootBoardNodeWithRootEdges place = state.getGameMap().getNodeByID(locationID);
        if (!place.getSympathy()) {
            System.out.println("Trying to revolt at location which is not sympathetic");
            return false;
        }

        executeRevolt(state, place);
        return true;
    }

    private boolean canExecute(RootGameState state) {
        return playerID == state.getCurrentPlayer() &&
                state.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance;
    }

    private boolean attemptToDiscard(Deck<RootCard> hand, RootGameState state) {
        RootCard card1 = hand.pick(cardToDiscard1Idx);
        int index2 = cardToDiscard2Idx;
        if (cardToDiscard1Idx < cardToDiscard2Idx) index2--;
        RootCard card2 = hand.pick(index2);
        state.getDiscardPile().add(card1);
        state.getDiscardPile().add(card2);
        return true;
    }

    private void executeRevolt(RootGameState state, RootBoardNodeWithRootEdges place) {
        //Place Base
        place.build(getBuildingType(place.getClearingType()));

        //Add Warriors
        for (int e = 0; e < Math.min(state.getWoodlandWarriors(), state.getGameMap().getSympatheticClearingsOfTypeCount(place.getClearingType())); e++) {
            place.addWarrior(RootParameters.Factions.WoodlandAlliance);
            state.removeWoodlandWarrior();
        }

        //Add Officer
        if (state.getWoodlandWarriors() > 1) {
            try {
                state.addOfficer();
            } catch (Exception e) {
                System.out.println("Trying to add an officer with no available warriors");
            }
        }
        //Remove Other pieces
        //Cat
        for (int i = 0; i < place.getWarrior(RootParameters.Factions.MarquiseDeCat); i++) {
            place.removeWarrior(RootParameters.Factions.MarquiseDeCat);
            state.addCatWarrior();
        }
        for (int i = 0; i < place.getWorkshops(); i++) {
            place.removeBuilding(RootParameters.BuildingType.Workshop);
            state.addBuilding(RootParameters.BuildingType.Workshop);
            state.addGameScorePlayer(playerID,1);
        }
        for (int i = 0; i < place.getRecruiters(); i++) {
            place.removeBuilding(RootParameters.BuildingType.Recruiter);
            state.addBuilding(RootParameters.BuildingType.Recruiter);
            state.addGameScorePlayer(playerID,1);
        }
        for (int i = 0; i < place.getSawmill(); i++) {
            place.removeBuilding(RootParameters.BuildingType.Sawmill);
            state.addBuilding(RootParameters.BuildingType.Sawmill);
            state.addGameScorePlayer(playerID,1);
        }
        for (int i = 0; i < place.getWood(); i++) {
            place.removeWood();
            state.addWood();
            state.addGameScorePlayer(playerID,1);
        }

        //Bird
        for (int e = 0; e < place.getWarrior(RootParameters.Factions.EyrieDynasties); e++) {
            place.removeWarrior(RootParameters.Factions.EyrieDynasties);
            state.addBirdWarrior();
        }
        for (int e = 0; e < place.getRoost(); e++) {
            place.removeBuilding(RootParameters.BuildingType.Roost);
            state.addBuilding(RootParameters.BuildingType.Roost);
            state.addGameScorePlayer(playerID,1);
        }
    }

    private RootParameters.BuildingType getBuildingType(RootParameters.ClearingTypes clearingType) {
        return switch (clearingType) {
            case Fox -> RootParameters.BuildingType.FoxBase;
            case Mouse -> RootParameters.BuildingType.MouseBase;
            case Rabbit -> RootParameters.BuildingType.RabbitBase;
            default -> null;
        };
    }

    @Override
    public Revolt copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Revolt revolt = (Revolt) o;
        return playerID == revolt.playerID && locationID == revolt.locationID && cardToDiscard1Idx == revolt.cardToDiscard1Idx && cardToDiscard1Id == revolt.cardToDiscard1Id && cardToDiscard2Idx == revolt.cardToDiscard2Idx && cardToDiscard2Id == revolt.cardToDiscard2Id && passSubGamePhase == revolt.passSubGamePhase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID, cardToDiscard1Idx, cardToDiscard1Id, cardToDiscard2Idx, cardToDiscard2Id, passSubGamePhase);
    }

    @Override
    public String toString() {
        return "p" + playerID + " revolts at " + locationID + " by discarding cards " + cardToDiscard1Idx + " and " + cardToDiscard2Idx;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card1 = (RootCard) gs.getComponentById(cardToDiscard1Id);
        RootCard card2 = (RootCard) gs.getComponentById(cardToDiscard2Id);
        return gs.getPlayerFaction(playerID).toString()  + " revolts at " + gs.getGameMap().getNodeByID(locationID).identifier + " by discarding " + card1.cardType.toString() + " and " + card2.cardType.toString();
    }
}

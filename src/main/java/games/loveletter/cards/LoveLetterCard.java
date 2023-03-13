package games.loveletter.cards;

import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Card;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.loveletter.actions.*;
import games.loveletter.actions.deep.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

public class LoveLetterCard extends Card {

    // each card consists of a type and a value. the card type defines the actions available to the player
    public enum CardType {
        Princess(8, "In case the princess is discarded or played the player is immediately removed from the game."),
        Countess(7, "The Countess needs to be discarded in case the player also hold a King or a Prince card."),
        King(6, "The King lets two players swap their hand cards."),
        Prince(5, "The targeted player discards its current and draws a new one."),
        Handmaid(4, "The handmaid protects the player from any targeted effects until the next turn."),
        Baron(3, "The Baron lets two players compare their hand card. The player with the lesser valued card is removed from the game."),
        Priest(2, "The Priest allows a player to see another player's hand cards."),
        Guard(1, "The guard allows to attempt guessing another player's card. If the guess is correct, the targeted opponent is removed from the game.");

        private final String cardText;
        private final int value;
        CardType(int value, String text){
            this.value = value;
            this.cardText = text;
        }
        public int getValue(){ return value;}
        public static int getMaxCardValue() { return 8; }
        public String getCardText(LoveLetterParameters params) {
            return this.name() + " (" + value + "; x" + params.cardCounts.get(this) + "): " + cardText;
        }

        private BiFunction<LoveLetterGameState, PlayCard, List<AbstractAction>> generateFlatActions, generateDeepActions;
        static {
            Princess.generateFlatActions = (gs, play) -> Collections.singletonList(new PrincessAction(play.getPlayerID()));
            Handmaid.generateFlatActions = (gs, play) -> Collections.singletonList(new HandmaidAction(play.getPlayerID()));
            Countess.generateFlatActions = (gs, play) -> Collections.singletonList(new CountessAction(play.getPlayerID(), gs.needToForceCountess(gs.getPlayerHandCards().get(play.getPlayerID()))));
            Priest.generateFlatActions = (gs, play) -> {
                int p = play.getPlayerID();
                boolean discard = play.isDiscard();
                List<AbstractAction> cardActions = new ArrayList<>();
                for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                    if (targetPlayer == p || gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                        continue;
                    cardActions.add(new PriestAction(p, targetPlayer, true, discard));
                }
                if (cardActions.size() == 0) cardActions.add(new PriestAction(p, -1, false, discard));
                return cardActions;
            };
            Priest.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepPriestAction(play.getPlayerID()));
            King.generateFlatActions = (gs, play) -> {
                int p = play.getPlayerID();
                boolean discard = play.isDiscard();
                List<AbstractAction> cardActions = new ArrayList<>();
                for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                    if (targetPlayer == p || gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                        continue;
                    cardActions.add(new KingAction(p, targetPlayer, true, discard));
                }
                if (cardActions.size() == 0) cardActions.add(new KingAction(p, -1, false, discard));
                return cardActions;
            };
            King.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepKingAction(play.getPlayerID()));
            Baron.generateFlatActions = (gs, play) -> {
                int p = play.getPlayerID();
                boolean discard = play.isDiscard();
                List<AbstractAction> cardActions = new ArrayList<>();
                for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                    if (targetPlayer == p || gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                        continue;
                    cardActions.add(new BaronAction(p, targetPlayer, true, discard));
                }
                if (cardActions.size() == 0) cardActions.add(new BaronAction(p, -1, false, discard));
                return cardActions;};
            Baron.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepBaronAction(play.getPlayerID()));
            Prince.generateFlatActions = (gs, play) -> {
                int p = play.getPlayerID();
                boolean discard = play.isDiscard();
                List<AbstractAction> cardActions = new ArrayList<>();
                for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                    if (gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                        continue;
                    cardActions.add(new PrinceAction(p, targetPlayer, true, discard));
                }
                if (cardActions.size() == 0) cardActions.add(new PrinceAction(p, -1, false, discard));
                return cardActions;};
            Prince.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepPrinceAction(play.getPlayerID()));
            Guard.generateFlatActions = (gs, play) -> {
                int p = play.getPlayerID();
                boolean discard = play.isDiscard();
                int target = play.getTargetPlayer();

                List<AbstractAction> cardActions = new ArrayList<>();
                if (target == -1) {
                    for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                        if (targetPlayer == p || gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                            continue;
                        for (LoveLetterCard.CardType type : LoveLetterCard.CardType.values()) {
                            if (type != LoveLetterCard.CardType.Guard) {
                                cardActions.add(new GuardAction(p, targetPlayer, type, true, discard));
                            }
                        }
                    }
                } else {
                    for (LoveLetterCard.CardType type : LoveLetterCard.CardType.values()) {
                        if (type != LoveLetterCard.CardType.Guard) {
                            cardActions.add(new GuardAction(p, target, type, true, discard));
                        }
                    }
                }
                if (cardActions.size() == 0) cardActions.add(new GuardAction(p, -1, null, false, discard));
                return cardActions;
            };
            Guard.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepGuardAction(play.getPlayerID()));
        }

        public List<AbstractAction> getFlatActions(LoveLetterGameState gs, PlayCard play) {
            if (generateFlatActions != null) return generateFlatActions.apply(gs, play);
            return new ArrayList<>();
        }
        public List<AbstractAction> getFlatActions(LoveLetterGameState gs, int playerId, boolean discard) {
            if (generateFlatActions != null) return generateFlatActions.apply(gs, new PlayCard(playerId, discard));
            return new ArrayList<>();
        }
        public List<AbstractAction> getDeepActions(LoveLetterGameState gs, PlayCard play) {
            if (generateDeepActions != null) return generateDeepActions.apply(gs, play);
            return new ArrayList<>();
        }
        public List<AbstractAction> getDeepActions(LoveLetterGameState gs, int playerId, boolean discard) {
            if (generateDeepActions != null) return generateDeepActions.apply(gs, new PlayCard(playerId, discard));
            return new ArrayList<>();
        }
    }

    public final CardType cardType;

    public LoveLetterCard(CardType cardType) {
        super(cardType.toString());
        this.cardType = cardType;
    }

    public LoveLetterCard(CardType cardType, int componentID) {
        super(cardType.toString(), componentID);
        this.cardType = cardType;
    }

    public String toString(){
        return cardType.toString();
    }

    @Override
    public LoveLetterCard copy() {
        return new LoveLetterCard(cardType, componentID);
    }
}

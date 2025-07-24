package games.loveletter.cards;

import core.CoreConstants;
import core.actions.AbstractAction;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.loveletter.actions.*;
import games.loveletter.actions.deep.*;

import java.util.*;
import java.util.function.*;

// each card consists of a type and a value (between 1 and 8). the card type defines the actions available to the player
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

    CardType(int value, String text) {
        this.value = value;
        this.cardText = text;
    }

    /** Numeric value of card from 1 to 8 */
    public int getValue() {
        return value;
    }

    // The value of the highest card in the game
    public static int getMaxCardValue() {
        return 8;
    }

    // Summarises the functionality of the card
    public String getCardText(LoveLetterParameters params) {
        return this.name() + " (" + value + "; x" + params.cardCounts.get(this) + "): " + cardText;
    }

    // Action factory
    private BiFunction<LoveLetterGameState, PlayCard, List<AbstractAction>> generateFlatActions, generateDeepActions;

    static {
        Princess.generateFlatActions = (gs, play) -> Collections.singletonList(new PlayCard(CardType.Princess, play.getCardIdx(), play.getPlayerID(), -1, null, null, true, true));
        Handmaid.generateFlatActions = (gs, play) -> Collections.singletonList(new HandmaidAction(play.getCardIdx(), play.getPlayerID()));
        Countess.generateFlatActions = (gs, play) -> Collections.singletonList(
                new PlayCard(Countess, play.getCardIdx(), play.getPlayerID(), -1, null, gs.needToForceCountess(gs.getPlayerHandCards().get(play.getPlayerID())), true, play.isDiscard()));
        Priest.generateFlatActions = (gs, play) -> {
            int p = play.getPlayerID();
            boolean discard = play.isDiscard();
            List<AbstractAction> cardActions = new ArrayList<>();
            for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                if (targetPlayer == p || !gs.isCurrentlyActive(targetPlayer) || gs.isProtected(targetPlayer))
                    continue;
                cardActions.add(new PriestAction(play.getCardIdx(), p, targetPlayer, true, discard));
            }
            if (cardActions.isEmpty()) cardActions.add(new PriestAction(play.getCardIdx(), p, p, false, discard));
            return cardActions;
        };
        Priest.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepPriestAction(play.getCardIdx(), play.getPlayerID()));
        King.generateFlatActions = (gs, play) -> {
            int p = play.getPlayerID();
            boolean discard = play.isDiscard();
            List<AbstractAction> cardActions = new ArrayList<>();
            for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                if (targetPlayer == p || !gs.isCurrentlyActive(targetPlayer) || gs.isProtected(targetPlayer))
                    continue;
                cardActions.add(new KingAction(play.getCardIdx(), p, targetPlayer, true, discard));
            }
            if (cardActions.isEmpty()) cardActions.add(new KingAction(play.getCardIdx(), p, p, false, discard));
            return cardActions;
        };
        King.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepKingAction(play.getCardIdx(), play.getPlayerID()));
        Baron.generateFlatActions = (gs, play) -> {
            int p = play.getPlayerID();
            boolean discard = play.isDiscard();
            List<AbstractAction> cardActions = new ArrayList<>();
            for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                if (targetPlayer == p || !gs.isCurrentlyActive(targetPlayer) || gs.isProtected(targetPlayer))
                    continue;
                cardActions.add(new BaronAction(play.getCardIdx(), p, targetPlayer, true, discard));
            }
            if (cardActions.isEmpty()) cardActions.add(new BaronAction(play.getCardIdx(), p, p, false, discard));
            return cardActions;
        };
        Baron.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepBaronAction(play.getCardIdx(), play.getPlayerID()));
        Prince.generateFlatActions = (gs, play) -> {
            int p = play.getPlayerID();
            boolean discard = play.isDiscard();
            List<AbstractAction> cardActions = new ArrayList<>();
            for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                if (!gs.isCurrentlyActive(targetPlayer) || gs.isProtected(targetPlayer))
                    continue;
                cardActions.add(new PrinceAction(play.getCardIdx(), p, targetPlayer, true, discard));
            }
            return cardActions;
        };
        Prince.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepPrinceAction(play.getCardIdx(), play.getPlayerID()));
        Guard.generateFlatActions = (gs, play) -> {
            int p = play.getPlayerID();
            boolean discard = play.isDiscard();
            int target = play.getTargetPlayer();

            List<AbstractAction> cardActions = new ArrayList<>();
            if (target == -1) {
                for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                    if (targetPlayer == p || !gs.isCurrentlyActive(targetPlayer) || gs.isProtected(targetPlayer))
                        continue;
                    for (CardType type : CardType.values()) {
                        if (type != CardType.Guard) {
                            cardActions.add(new GuardAction(play.getCardIdx(), p, targetPlayer, type, true, discard));
                        }
                    }
                }
            } else {
                for (CardType type : CardType.values()) {
                    if (type != CardType.Guard) {
                        cardActions.add(new GuardAction(play.getCardIdx(), p, target, type, true, discard));
                    }
                }
            }
            if (cardActions.isEmpty()) {
                // Target yourself, but don't guess yourself out of the game
                cardActions.add(new GuardAction(play.getCardIdx(), p, p, Guard, false, discard));
            }
            return cardActions;
        };
        Guard.generateDeepActions = (gs, play) -> Collections.singletonList(new DeepGuardAction(play.getCardIdx(), play.getPlayerID()));
    }

    public List<AbstractAction> flatActions(LoveLetterGameState gs, PlayCard play) {
        if (generateFlatActions != null) return generateFlatActions.apply(gs, play);
        return new ArrayList<>();
    }

    public List<AbstractAction> flatActions(LoveLetterGameState gs, int cardIdx, int playerId, boolean discard) {
        if (generateFlatActions != null) return generateFlatActions.apply(gs, new PlayCard(cardIdx, playerId, discard));
        return new ArrayList<>();
    }

    public List<AbstractAction> deepActions(LoveLetterGameState gs, int cardIdx, int playerId, boolean discard) {
        PlayCard play = new PlayCard(cardIdx, playerId, discard);
        if (generateDeepActions != null) return generateDeepActions.apply(gs, play);
        return flatActions(gs, play);
    }

    // To string
    private Function<PlayCard, String> generateString;

    static {
        Princess.generateString = play -> "Princess (" + play.getPlayerID() + " loses the game)";
        Countess.generateString = play -> {
            if (play.getForcedCountessCardType() == null) return "Countess (no effect)";
            return "Countess (auto discard with " + play.getForcedCountessCardType() + ")";
        };
        King.generateString = play -> "King (" + play.getPlayerID() + " trades hands with " + play.getTargetPlayer() + ")";
        Prince.generateString = play -> "Prince (" + play.getTargetPlayer() + " discards " + (play.getTargetCardType() != null ? play.getTargetCardType() : "card") + " and draws a new card)";
        Handmaid.generateString = play -> "Handmaid (" + play.getPlayerID() + " is protected until their next turn)";
        Baron.generateString = play -> {
            if (play.getTargetCardType() == null) {
                return "Baron (" + play.getPlayerID() + " compares cards with " + play.getTargetPlayer() + ")";
            } else {
                return "Baron (" + play.getPlayerID() + " " + play.getOtherCardInHand() + " vs " + play.getTargetPlayer() + " " + play.getTargetCardType() + ")";
            }
        };
        Priest.generateString = play -> "Priest (" + play.getPlayerID() + " sees " + (play.getTargetCardType() != null ? play.getTargetCardType() : "card") + " of " + play.getTargetPlayer() + ")";
        Guard.generateString = play -> "Guard (" + play.getPlayerID() + " guess " + play.getTargetPlayer() + " holds card " + play.getTargetCardType().name() + ")";
    }

    public String getString(PlayCard play) {
        if (generateString != null) {
            return generateString.apply(play);
        } else return "";
    }

    // Execute
    private BiConsumer<LoveLetterGameState, PlayCard> execute;

    public void execute(LoveLetterGameState gs, PlayCard play) {
        if (execute != null) {
            execute.accept(gs, play);
        }
    }

    static {
        Princess.execute = (gs, play) -> gs.killPlayer(play.getPlayerID(), play.getPlayerID(), Princess);

    }
}

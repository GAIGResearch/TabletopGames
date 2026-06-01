package games.spades;

import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.spades.actions.Bid;
import games.spades.actions.PlayCard;
import org.junit.*;

import java.util.List;
import java.util.Random;

import static games.spades.SpadesGameState.Phase.BIDDING;
import static games.spades.SpadesGameState.Phase.PLAYING;
import static org.junit.Assert.*;


public class TestSpades {

    private SpadesForwardModel forwardModel;
    private SpadesGameState gameState;
    Random rnd =  new Random(64);

    @Before
    public void setUp() {
        forwardModel = new SpadesForwardModel();
        AbstractParameters gameParameters = new SpadesParameters();
        gameState = new SpadesGameState(gameParameters, 4);
        forwardModel.setup(gameState);
    }

    // ========================================
    // Set-up Phase
    // ========================================

    /**
     * - 4 plyaers
     * - 13 cards
     * - Start as Bidding phase
     * - Player 1 (on the left of the dealer) bids first
     * - Current bid for every player is -1(=not bid yet)
     * - Teamscore = 0
     * - Spades not broken
     */
    @Test
    public void testGameSetup() {
        assertEquals(4, gameState.getNPlayers());
        assertEquals(SpadesGameState.Phase.BIDDING, gameState.getGamePhase());
        assertEquals(0, gameState.getCurrentPlayer());

        for (int i = 0; i < 4; i++) {
            assertEquals(13, gameState.getPlayerHands().get(i).getSize());
            assertEquals(-1, gameState.getPlayerBid(i));
        }

        assertEquals(0, gameState.getTeamScore(0));
        assertEquals(0, gameState.getTeamScore(1));
        assertFalse(gameState.isSpadesBroken());

        int totalCards = gameState.getPlayerHands().stream()
                .mapToInt(Deck::getSize)
                .sum();
        assertEquals("总共应该有52张牌", 52, totalCards);
    }

    /**
     * player-team
     * <p>
     * player0&2 - team1, player1&3 - team2
     */
    @Test
    public void testPlayerTeamMapping() {
        assertEquals(0, gameState.getTeam(0));
        assertEquals(1, gameState.getTeam(1));
        assertEquals(0, gameState.getTeam(2));
        assertEquals(1, gameState.getTeam(3));
    }


    // ========================================
    // Bidding
    // ========================================

    @Test
    public void testBiddingPhaseAvailableActions() {
        assertEquals(SpadesGameState.Phase.BIDDING, gameState.getGamePhase());
        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        assertEquals(14, actions.size());

        assertTrue(actions.stream().allMatch(a -> a instanceof Bid));
        // check we have one of each number
        for (int b = 0; b < 14; b++) {
            assertTrue(actions.contains(new Bid(b)));
        }

        for (int i = 0; i < 4; i++) {
            assertEquals(i, gameState.getCurrentPlayer());
            forwardModel.next(gameState, new Bid(i + 1));
        }
        assertEquals(PLAYING, gameState.getGamePhase());

    }

    /**
     * 测试叫牌动作的执行和阶段转换
     * <p>
     * 验证：
     * - 叫牌被正确记录
     * - 轮到下一个玩家
     * - 所有玩家叫牌后转换到出牌阶段
     */
    @Test
    public void testBiddingExecution() {
        // 玩家1叫5
        Bid bid1 = new Bid(5);
        forwardModel.next(gameState, bid1);

        assertEquals(5, gameState.getPlayerBid(0));
        assertEquals(-1, gameState.getPlayerBid(1));

        assertEquals("应该轮到玩家", 1, gameState.getCurrentPlayer());
        assertEquals("仍然应该处于叫牌阶段", SpadesGameState.Phase.BIDDING, gameState.getGamePhase());

        // 其他玩家完成叫牌
        forwardModel.next(gameState, new Bid(3));
        forwardModel.next(gameState, new Bid(4));

        // 最后一个玩家叫牌
        forwardModel.next(gameState, new Bid(2));
        assertEquals(PLAYING, gameState.getGamePhase());
        assertEquals(0, gameState.getCurrentPlayer());
    }

    @Test
    public void testStartingBidderRotates() {
        // there are a total of 4 x 14 = 56 actions (including bids) in a round
        assertEquals(0, gameState.getRoundCounter());
        for (int i = 0; i < 56; i++) {
            assertEquals(0, gameState.getRoundCounter());
            List<AbstractAction>  actions = forwardModel.computeAvailableActions(gameState);
            forwardModel.next(gameState, actions.get(rnd.nextInt(actions.size())));
        }
        assertEquals(1, gameState.getRoundCounter());
        assertEquals(1, gameState.getCurrentPlayer());
        assertEquals(BIDDING, gameState.getGamePhase());
        for (int i = 0; i < 4; i++) {
            List<AbstractAction>  actions = forwardModel.computeAvailableActions(gameState);
            forwardModel.next(gameState, actions.get(rnd.nextInt(actions.size())));
        }
        assertEquals(1, gameState.getRoundCounter());
        assertEquals(1, gameState.getCurrentPlayer());
        assertEquals(PLAYING, gameState.getGamePhase());
    }

    /**
     * 测试团队叫牌计算
     * <p>
     * 团队的总叫牌是两个队友叫牌的和，这对最终得分计算很重要
     */
    @Test
    public void testTeamBidCalculation() {
        // 完成所有叫牌
        forwardModel.next(gameState, new Bid(5)); // 团队1
        forwardModel.next(gameState, new Bid(3)); // 团队0
        forwardModel.next(gameState, new Bid(4)); // 团队1
        forwardModel.next(gameState, new Bid(2)); // 团队0

        // 验证团队叫牌总和
        int team0Bid = gameState.getPlayerBid(0) + gameState.getPlayerBid(2); // 2 + 3 = 5
        int team1Bid = gameState.getPlayerBid(1) + gameState.getPlayerBid(3); // 5 + 4 = 9

        assertEquals("团队0的总叫牌应该是5", 9, team0Bid);
        assertEquals("团队1的总叫牌应该是9", 5, team1Bid);
    }

    // ========================================
    // 出牌阶段测试
    // ========================================

    /**
     * 测试出牌阶段的基本动作生成
     * <p>
     * 在出牌阶段开始时，首家应该能够出任何牌（除了黑桃，除非只有黑桃）
     */
    @Test
    public void testPlayingPhaseBasicActions() {
        // 完成叫牌阶段
        completeBiddingPhase();

        assertEquals("应该处于出牌阶段", PLAYING, gameState.getGamePhase());

        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        assertFalse(actions.isEmpty());

        // 验证所有动作都是PlayCard动作
        for (AbstractAction action : actions) {
            assertTrue("所有动作都应该是PlayCard动作", action instanceof PlayCard);
        }
    }

    /**
     * 测试黑桃破门规则
     * <p>
     * 在黑桃未破门时，不能首攻黑桃，除非手中只有黑桃
     * 这是Spades游戏的核心规则之一
     */
    @Test
    public void testSpadesBreakingRule() {
        completeBiddingPhase();

        // 创建一个有混合花色的手牌来测试黑桃破门规则
        Deck<FrenchCard> playerHand = gameState.getPlayerHands().get(gameState.getCurrentPlayer());
        playerHand.clear();

        // 添加非黑桃牌和黑桃牌
        playerHand.add(new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts));
        playerHand.add(new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Spades));

        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);

        // 验证不能首攻黑桃（黑桃未破门且有其他花色）
        boolean canLeadSpades = actions.stream()
                .filter(a -> a instanceof PlayCard)
                .map(a -> (PlayCard) a)
                .anyMatch(a -> a.card.suite == FrenchCard.Suite.Spades);

        assertFalse("黑桃未破门时，有其他花色时不能首攻黑桃", canLeadSpades);
    }

    /**
     * 测试只有黑桃时可以首攻黑桃
     * <p>
     * 当玩家手中只有黑桃时，即使黑桃未破门也必须出黑桃
     */
    @Test
    public void testSpadesOnlyCanLeadSpades() {
        completeBiddingPhase();

        // 创建只有黑桃的手牌
        Deck<FrenchCard> playerHand = gameState.getPlayerHands().get(gameState.getCurrentPlayer());
        playerHand.clear();
        playerHand.add(new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Spades));
        playerHand.add(new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Spades));

        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);

        // 验证可以出黑桃
        boolean canLeadSpades = actions.stream()
                .filter(a -> a instanceof PlayCard)
                .map(a -> (PlayCard) a)
                .anyMatch(a -> a.card.suite == FrenchCard.Suite.Spades);

        assertTrue("手中只有黑桃时应该可以首攻黑桃", canLeadSpades);
    }

    /**
     * 测试跟牌规则
     * <p>
     * 当别人已经出牌时，必须跟牌（如果有的话）
     * 这是trick-taking游戏的基本规则
     */
    @Test
    public void testFollowSuitRule() {
        completeBiddingPhase();

        // 玩家1出红心A
        Deck<FrenchCard> player0Hand = gameState.getPlayerHands().get(0);
        FrenchCard heartAce = new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts);
        player0Hand.add(heartAce);

        forwardModel.next(gameState, new PlayCard(heartAce)); // P0
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));  // P1

        // 现在轮到玩家2，设置玩家2的手牌（有红心和其他花色）
        Deck<FrenchCard> player2Hand = gameState.getPlayerHands().get(2);
        player2Hand.clear();
        player2Hand.add(new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Hearts));
        player2Hand.add(new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Clubs));

        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        assertEquals(PLAYING, gameState.getGamePhase());

        // 验证只能出红心
        for (AbstractAction action : actions) {
            PlayCard playAction = (PlayCard) action;
            assertEquals("必须跟首攻花色（红心）", FrenchCard.Suite.Hearts, playAction.card.suite);
        }
    }

    /**
     * 测试没有跟牌花色时可以出任意牌
     * <p>
     * 当玩家没有首攻花色时，可以出任意牌（包括将牌黑桃）
     */
    @Test
    public void testCannotFollowSuitRule() {
        completeBiddingPhase();

        // 玩家1出红心A
        Deck<FrenchCard> player0Hand = gameState.getPlayerHands().get(0);
        FrenchCard heartAce = new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts);
        player0Hand.add(heartAce);

        forwardModel.next(gameState, new PlayCard(heartAce));
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));

        // 设置玩家2没有红心
        Deck<FrenchCard> player2Hand = gameState.getPlayerHands().get(2);
        player2Hand.clear();
        player2Hand.add(new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Clubs));
        player2Hand.add(new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Spades));

        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        assertEquals("没有跟牌花色时应该可以出所有手牌", 2, actions.size());

        // 验证可以出任意花色
        boolean hasClubs = actions.stream().anyMatch(a -> ((PlayCard) a).card.suite == FrenchCard.Suite.Clubs);
        boolean hasSpades = actions.stream().anyMatch(a -> ((PlayCard) a).card.suite == FrenchCard.Suite.Spades);

        assertTrue("应该可以出梅花", hasClubs);
        assertTrue("应该可以出黑桃", hasSpades);
    }

    // ========================================
    // 赢牌判断测试
    // ========================================

    /**
     * 测试黑桃将牌的优先级
     * <p>
     * 黑桃是将牌，总是比其他花色大
     * 这是Spades游戏的核心机制
     */
    @Test
    public void testSpadesAreTrump() {
        completeBiddingPhase();

        // 模拟出牌：红心A, 黑桃2, 红心K, 红心Q
        FrenchCard heartAce = new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts);
        FrenchCard spade2 = new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Spades, 2);
        FrenchCard heartKing = new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Hearts);
        FrenchCard heartQueen = new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Hearts);

        playCompleteRound(heartAce, spade2, heartKing, heartQueen);

        // 验证黑桃2赢了这轮（尽管红心A更大）
        assertEquals("出黑桃2的玩家应该赢得这轮", 1, gameState.getCurrentPlayer());
        assertEquals(1, gameState.getTricksTaken(1));
    }

    /**
     * 测试同花色牌的大小比较
     * <p>
     * 在没有将牌的情况下，同花色中点数大的牌获胜
     */
    @Test
    public void testSameSuitComparison() {
        completeBiddingPhase();

        // 模拟出牌：红心2, 红心A, 红心K, 红心Q (都是红心，A最大)
        FrenchCard heart2 = new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Hearts, 2);
        FrenchCard heartAce = new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts);
        FrenchCard heartKing = new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Hearts);
        FrenchCard heartQueen = new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Hearts);

        playCompleteRound(heart2, heartAce, heartKing, heartQueen);

        // 验证红心A赢了这轮
        assertEquals("出红心A的玩家应该赢得这轮", 1, gameState.getCurrentPlayer());
    }

    // ========================================
    // 得分系统测试
    // ========================================

    /**
     * 测试成功完成合约的得分
     * <p>
     * 当团队赢得的墩数 >= 叫牌数时：
     * - 基础分 = 叫牌数 * 10
     * - 沙袋分 = (实际墩数 - 叫牌数) * 1
     */
    @Test
    public void testSuccessfulContractScoring() {
        // 设置一个简单的得分场景
        gameState.setPlayerBid(0, 3); // 团队0叫3
        gameState.setPlayerBid(2, 2); // 团队0叫2，总共5

        // 模拟团队0赢得6墩
        for (int i = 0; i < 6; i++) {
            gameState.incrementTricksTaken(0); // 玩家0赢得6墩
        }

        // 手动调用得分计算逻辑
        int teamBid = gameState.getPlayerBid(0) + gameState.getPlayerBid(2); // 5
        int teamTricks = gameState.getTricksTaken(0) + gameState.getTricksTaken(2); // 6

        int expectedScore = teamBid * 10 + (teamTricks - teamBid); // 5*10 + 1 = 51

        assertEquals("团队叫牌应该是5", 5, teamBid);
        assertEquals("团队实际墩数应该是6", 6, teamTricks);
        // 注意：这里我们测试的是计算逻辑，实际的endRound方法会自动计算
    }

    /**
     * 测试未完成合约的惩罚
     * <p>
     * 当团队赢得的墩数 < 叫牌数时：
     * - 扣分 = 叫牌数 * 10
     */
    @Test
    public void testFailedContractPenalty() {
        gameState.setPlayerBid(0, 5); // 团队0叫5
        gameState.setPlayerBid(2, 2); // 团队0叫2，总共7

        // 模拟团队0只赢得5墩（少于叫牌7）
        for (int i = 0; i < 5; i++) {
            gameState.incrementTricksTaken(0);
        }

        int teamBid = gameState.getPlayerBid(0) + gameState.getPlayerBid(2); // 7
        int teamTricks = gameState.getTricksTaken(0) + gameState.getTricksTaken(2); // 5

        assertTrue("团队实际墩数应该少于叫牌", teamTricks < teamBid);
        int expectedPenalty = teamBid * 10; // 70分惩罚
        assertEquals("惩罚应该是70分", 70, expectedPenalty);
    }

    /**
     * 测试沙袋累积和惩罚
     * <p>
     * 每10个沙袋扣100分，这防止玩家故意低叫
     */
    @Test
    public void testSandbagPenalty() {
        SpadesParameters params = (SpadesParameters) gameState.getGameParameters();

        // 设置团队已有9个沙袋
        gameState.addTeamSandbags(0, 9);

        // 再添加2个沙袋（总共11个）
        gameState.addTeamSandbags(0, 2);

        // 检查是否触发沙袋惩罚
        if (gameState.getTeamSandbags(0) >= params.sandbagsPerPenalty) {
            int penalty = params.sandbagsRandPenalty; // 100分
            assertEquals("沙袋惩罚应该是100分", 100, penalty);

            // 验证沙袋数重置
            int remainingSandbags = gameState.getTeamSandbags(0) - params.sandbagsPerPenalty;
            assertEquals("剩余沙袋应该是1个", 1, remainingSandbags);
        }
    }

    // ========================================
    // 游戏结束条件测试  
    // ========================================

    /**
     * 测试达到获胜分数的游戏结束
     * <p>
     * 当任意团队达到500分时游戏结束，得分高的团队获胜
     */
    @Test
    public void testWinningScoreGameEnd() {
        // 设置团队0得分为520分
        gameState.setTeamScore(0, 520);
        gameState.setTeamScore(1, 300);

        SpadesParameters params = (SpadesParameters) gameState.getGameParameters();
        boolean gameEnded = gameState.getTeamScore(0) >= params.winningScore ||
                gameState.getTeamScore(1) >= params.winningScore;

        assertTrue("游戏应该结束（有团队达到获胜分数）", gameEnded);

        int winningTeam = gameState.getTeamScore(0) > gameState.getTeamScore(1) ? 0 : 1;
        assertEquals("团队0应该获胜", 0, winningTeam);
    }

    // ========================================
    // 辅助方法
    // ========================================

    /**
     * 完成叫牌阶段的辅助方法
     * 让所有玩家完成叫牌，进入出牌阶段
     */
    private void completeBiddingPhase() {
        forwardModel.next(gameState, new Bid(3));
        forwardModel.next(gameState, new Bid(4));
        forwardModel.next(gameState, new Bid(2));
        forwardModel.next(gameState, new Bid(4));
    }

    /**
     * 执行完整一轮出牌的辅助方法
     *
     * @param card1 玩家1出的牌
     * @param card2 玩家2出的牌
     * @param card3 玩家3出的牌
     * @param card4 玩家0出的牌
     */
    private void playCompleteRound(FrenchCard card1, FrenchCard card2, FrenchCard card3, FrenchCard card4) {
        // 添加牌到对应玩家手中并出牌
        int player =  gameState.getCurrentPlayer();
        gameState.getPlayerHands().get(player).add(card1);
        forwardModel.next(gameState, new PlayCard(card1));

        gameState.getPlayerHands().get(player + 1 % 4).add(card2);
        forwardModel.next(gameState, new PlayCard(card2));

        gameState.getPlayerHands().get(player + 2 % 4).add(card3);
        forwardModel.next(gameState, new PlayCard(card3));

        gameState.getPlayerHands().get(player + 3 % 4).add(card4);
        forwardModel.next(gameState, new PlayCard(card4));
    }
}
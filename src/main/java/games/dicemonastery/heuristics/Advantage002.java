package games.dicemonastery.heuristics;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Advantage002 extends AbstractPlayer {

    Random rnd = new Random(System.currentTimeMillis());

    private final double RND_WEIGHT;

    Map<Integer, Double> actionAdvantage = new HashMap<>();

    public Advantage002() {
        this(0.5);
    }

    public Advantage002(double rndWeight) {
        this.RND_WEIGHT = rndWeight;

        actionAdvantage.put(671907029, 0.727883912);
        actionAdvantage.put(858461023, 0.671131120);
        actionAdvantage.put(671907026, 0.626346918);
        actionAdvantage.put(671907027, 0.619790030);
        actionAdvantage.put(858461021, 0.581771712);
        actionAdvantage.put(-225521174, 0.571708960);
        actionAdvantage.put(-942379020, 0.545747853);
        actionAdvantage.put(-225521175, 0.521114635);
        actionAdvantage.put(-12051179, 0.520906508);
        actionAdvantage.put(858461020, 0.496330604);
        actionAdvantage.put(870037104, 0.488009806);
        actionAdvantage.put(-942379019, 0.480662520);
        actionAdvantage.put(858461022, 0.480523356);
        actionAdvantage.put(-2016553079, 0.479263167);
        actionAdvantage.put(3094, 0.463607617);
        actionAdvantage.put(-1489990695, 0.460303791);
        actionAdvantage.put(-1153212376, 0.451214700);
        actionAdvantage.put(640694721, 0.450358804);
        actionAdvantage.put(-1554978260, 0.373293941);
        actionAdvantage.put(671907028, 0.363527342);
        actionAdvantage.put(118, 0.307719884);
        actionAdvantage.put(398321, 0.302076468);
        actionAdvantage.put(-1231443999, 0.297187702);
        actionAdvantage.put(62, 0.294031436);
        actionAdvantage.put(1157740463, 0.280907274);
        actionAdvantage.put(3, 0.260105356);
        actionAdvantage.put(30243, 0.242514805);
        actionAdvantage.put(13919, 0.225639154);
        actionAdvantage.put(-1676953787, 0.187540371);
        actionAdvantage.put(-1311818087, 0.184707331);
        actionAdvantage.put(177, 0.179903847);
        actionAdvantage.put(30913, 0.173162396);
        actionAdvantage.put(-1554978261, 0.173096768);
        actionAdvantage.put(-1311818085, 0.163089490);
        actionAdvantage.put(1556957245, 0.161319727);
        actionAdvantage.put(-1311818088, 0.159552266);
        actionAdvantage.put(-1311818086, 0.159146348);
        actionAdvantage.put(6, 0.155127786);
        actionAdvantage.put(-1329523947, 0.145359071);
        actionAdvantage.put(1705930752, 0.134327730);
        actionAdvantage.put(-448477385, 0.130057064);
        actionAdvantage.put(-448477383, 0.122259193);
        actionAdvantage.put(121, 0.117867141);
        actionAdvantage.put(-448477382, 0.116749889);
        actionAdvantage.put(59, 0.106620911);
        actionAdvantage.put(-448477384, 0.104781016);
        actionAdvantage.put(128213, 0.099015589);
        actionAdvantage.put(992137803, 0.082753016);
        actionAdvantage.put(1509515449, 0.070266222);
        actionAdvantage.put(692683812, 0.059240033);
        actionAdvantage.put(1556957214, 0.057491769);
        actionAdvantage.put(450, 0.053324508);
        actionAdvantage.put(692683813, 0.052556940);
        actionAdvantage.put(692683815, 0.048335465);
        actionAdvantage.put(1724086432, 0.047653700);
        actionAdvantage.put(18213, 0.046074040);
        actionAdvantage.put(1509515325, 0.045512908);
        actionAdvantage.put(692683814, 0.035371021);
        actionAdvantage.put(511834455, 0.034356996);
        actionAdvantage.put(1556957183, 0.032732501);
        actionAdvantage.put(1705930721, 0.032570093);
        actionAdvantage.put(236, 0.027087821);
        actionAdvantage.put(-1527453497, 0.024752632);
        actionAdvantage.put(238452555, 0.024440888);
        actionAdvantage.put(1509515480, 0.020339504);
        actionAdvantage.put(511834424, 0.013496967);
        actionAdvantage.put(1705930690, 0.008932361);
        actionAdvantage.put(1297686928, 0.006565983);
        actionAdvantage.put(1221556999, 0.004366808);
        actionAdvantage.put(511834393, 0.003332020);
        actionAdvantage.put(651, 0.002757518);
        actionAdvantage.put(1297686866, 0.002493582);
        actionAdvantage.put(1221556937, 0.001606824);
        actionAdvantage.put(1297686897, -0.001786777);
        actionAdvantage.put(572, -0.002003027);
        actionAdvantage.put(992137679, -0.008453308);
        actionAdvantage.put(1509514364, -0.009595668);
        actionAdvantage.put(1297686835, -0.011295346);
        actionAdvantage.put(575593575, -0.011969229);
        actionAdvantage.put(1188392295, -0.012386958);
        actionAdvantage.put(2482, -0.012885322);
        actionAdvantage.put(-1918134964, -0.012985325);
        actionAdvantage.put(1705930783, -0.014656263);
        actionAdvantage.put(511834362, -0.016214351);
        actionAdvantage.put(992137741, -0.016312502);
        actionAdvantage.put(1509515418, -0.018129698);
        actionAdvantage.put(1221556968, -0.019981951);
        actionAdvantage.put(992137710, -0.020817980);
        actionAdvantage.put(214074868, -0.024352958);
        actionAdvantage.put(511834331, -0.026396442);
        actionAdvantage.put(-1886516373, -0.028576370);
        actionAdvantage.put(-596922648, -0.029488815);
        actionAdvantage.put(201556483, -0.035539528);
        actionAdvantage.put(493, -0.040259676);
        actionAdvantage.put(1509515387, -0.049155270);
        actionAdvantage.put(1509515356, -0.049268150);
        actionAdvantage.put(992137772, -0.049681546);
        actionAdvantage.put(-1925829325, -0.049919115);
        actionAdvantage.put(-280415468, -0.050357091);
        actionAdvantage.put(839, -0.056037789);
        actionAdvantage.put(1574772096, -0.060583789);
        actionAdvantage.put(1182320432, -0.060913059);
        actionAdvantage.put(730, -0.061285698);
        actionAdvantage.put(967765295, -0.066550624);
        actionAdvantage.put(582925235, -0.067074586);
        actionAdvantage.put(1574772097, -0.068073402);
        actionAdvantage.put(1716392071, -0.069333170);
        actionAdvantage.put(992137648, -0.069923087);
        actionAdvantage.put(1574772098, -0.072797527);
        actionAdvantage.put(1734853116, -0.075315095);
        actionAdvantage.put(1574772095, -0.091229730);
        actionAdvantage.put(1705930659, -0.102369757);
        actionAdvantage.put(1556957152, -0.103796270);
        actionAdvantage.put(1228, -0.112202115);
        actionAdvantage.put(1556957090, -0.114725169);
        actionAdvantage.put(1556957121, -0.116590216);
        actionAdvantage.put(1617, -0.119837106);
        actionAdvantage.put(-2021811867, -0.124837853);
        actionAdvantage.put(1705930628, -0.145399945);
        actionAdvantage.put(333797722, -0.152687849);
        actionAdvantage.put(-785255702, -0.170406967);
        actionAdvantage.put(-785255703, -0.175296318);
        actionAdvantage.put(809, -0.178324340);
        actionAdvantage.put(2398734, -0.182171818);
        actionAdvantage.put(-1688792581, -0.195678048);
        actionAdvantage.put(1345429714, -0.208285864);
        actionAdvantage.put(246146916, -0.210006449);
        actionAdvantage.put(1345429712, -0.211616704);
        actionAdvantage.put(-785255701, -0.217062979);
        actionAdvantage.put(-288109829, -0.220400647);
        actionAdvantage.put(-2054127287, -0.227454710);
        actionAdvantage.put(-785255704, -0.230012695);
        actionAdvantage.put(1345429715, -0.235482203);
        actionAdvantage.put(626405447, -0.242864567);
        actionAdvantage.put(1345429713, -0.259942813);
        actionAdvantage.put(-236935256, -0.262660536);
        actionAdvantage.put(289627128, -0.270881028);
        actionAdvantage.put(2097707489, -0.284908108);
        actionAdvantage.put(333797721, -0.286136091);
        actionAdvantage.put(39307, -0.287108252);
        actionAdvantage.put(1767566644, -0.300667596);
        actionAdvantage.put(575230874, -0.325294952);
        actionAdvantage.put(-1696486942, -0.332999329);
        actionAdvantage.put(-1874654752, -0.340651959);
        actionAdvantage.put(65, -0.344426126);
        actionAdvantage.put(2006, -0.358251264);
        actionAdvantage.put(0, -0.367587162);
        actionAdvantage.put(180, -0.374992045);
        actionAdvantage.put(-64045962, -0.384377352);
        actionAdvantage.put(-1473981366, -0.422692309);
        actionAdvantage.put(-1473981365, -0.466810721);
        actionAdvantage.put(295, -0.506144879);
        actionAdvantage.put(537694684, -0.506527814);
        actionAdvantage.put(-1645312369, -0.596769118);
        actionAdvantage.put(-64045961, -0.650392667);
        actionAdvantage.put(9, -0.720598310);
        actionAdvantage.put(-2002771307, -0.721020220);
        actionAdvantage.put(124, -0.821740042);
        actionAdvantage.put(2395, -0.836514840);
        actionAdvantage.put(1829047466, -0.870104792);
        actionAdvantage.put(239, -0.922054763);
        actionAdvantage.put(-2054127286, -0.943743487);
        actionAdvantage.put(354, -1.037314328);
        actionAdvantage.put(-2002771306, -1.236733572);
        actionAdvantage.put(1551878344, -1.368315043);
        actionAdvantage.put(183, -1.406012102);
        actionAdvantage.put(68, -1.416832571);
        actionAdvantage.put(298, -1.417183587);
        actionAdvantage.put(1829047465, -1.696991598);
        actionAdvantage.put(413, -1.747577867);
        actionAdvantage.put(127, -2.002122491);
        actionAdvantage.put(1917522137, -2.134871795);
        actionAdvantage.put(357, -2.168479248);
        actionAdvantage.put(242, -2.377529295);
        actionAdvantage.put(12, -2.499084690);
        actionAdvantage.put(309934, -2.510972657);
        actionAdvantage.put(301, -2.939603061);
        actionAdvantage.put(186, -2.947442521);
        actionAdvantage.put(71, -3.243367768);
        actionAdvantage.put(416, -3.468694823);
        actionAdvantage.put(-886679665, -3.614685221);
        actionAdvantage.put(360, -3.722282430);
        actionAdvantage.put(650031938, -3.791418341);
        actionAdvantage.put(245, -4.101748481);
        actionAdvantage.put(130, -4.159914256);
        actionAdvantage.put(304, -4.695631872);
        actionAdvantage.put(189, -5.180071138);
        actionAdvantage.put(419, -5.279596083);
        actionAdvantage.put(248, -5.981021552);
        actionAdvantage.put(307, -6.943461012);
        actionAdvantage.put(425, -8.989915639);
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {

        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;

        double bestValue = Double.NEGATIVE_INFINITY;
        AbstractAction retValue = possibleActions.get(0);
        for (AbstractAction action : possibleActions) {
            double actionValue = actionAdvantage.getOrDefault(action.hashCode(), 0.0) + rnd.nextDouble() * RND_WEIGHT;
            if (actionValue > bestValue) {
                retValue = action;
                bestValue = actionValue;
            }
        }
        return retValue;
    }
}

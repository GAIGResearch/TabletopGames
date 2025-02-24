package games.conquest;

import core.AbstractGameState;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.conquest.components.CommandType;
import players.PlayerParameters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.conquest.components.CommandType.*;

/**
 * <p>This class should hold a series of variables representing game parameters (e.g. number of cards dealt to players,
 * maximum number of rounds in the game etc.). These parameters should be used everywhere in the code instead of
 * local variables or hard-coded numbers, by accessing these parameters from the game state via {@link AbstractGameState#getGameParameters()}.</p>
 *
 * <p>It should then implement appropriate {@link #_copy()}, {@link #_equals(Object)} and {@link #hashCode()} functions.</p>
 *
 * <p>The class can optionally extend from {@link TunableParameters} instead, which allows to use
 * automatic game parameter optimisation tools in the framework.</p>
 */
public class CQParameters extends TunableParameters {
    // Game parameters
    public final int gridWidth = 20;
    public final int gridHeight = 20;
    public final int setupPoints = 1000;
    public final int maxTroops = 10;
    public final int maxCommands = 4;
    public final int nSetupRows = 3; // setup allowed in first 3 rows only
    public static final String dataPath = "data/conquest/";
    List<Setup> setups;
    public Setup p0TroopSetup;
    public Setup p1TroopSetup;
    boolean testSetup = false;

    // All of the following pre-made setups are taken from the RuneScape Wiki's strategy guide accessed in 2024.
    // The names used are those used in said strategy guide: https://runescape.wiki/w/Conquest/Strategies?oldid=35427217
    // The placement of each of the troops, unless provided in the strategy guide, is done based on the strategy description
    public enum Setup {
        Test("C", BattleCry, Charge, Chastise),
        Default("    S A F  F M S\n" +
                   "     H K    C H",
                   BattleCry, Charge, Chastise),
        Rush("       S K   H S\n"+
                "      C S S S S C",
                BattleCry, Charge, ShieldWall, Stoicism),
        AntiRush("      C S   F C\n"+
                    "     H S S S S H",
                    Charge, Stoicism, Chastise, BattleCry),
        FootySpam("  SSCF\n"+
                     "\n"+
                     "FHCSSF",
                     Charge, BattleCry, Stoicism, Chastise),
        RegenerateSetup("C H S\n"+
                           "    SS\n"+
                           "SSSSC",
                           Charge, BattleCry, Chastise, Regenerate),
        WindsOfFateSetup("C H S\n"+
                            "    SS\n"+
                            "SSSSC",
                            Charge, BattleCry, Chastise, WindsOfFate),
        BombardSetup("   S S C C S S\n"+
                        "     S S S S",
                        Charge, BattleCry, Chastise, Bombard),
        DefaultVariantArcher("    S A    A M S\n" +
                                "     H K    C H",
                                Charge, BattleCry, Chastise),
        DefaultVariant1("      S A H  H A S\n" +
                           "       M C    S M",
                           BattleCry, Chastise, Vigilance),
        DefaultVariant2("      S   C   S\n" +
                           "       KA M AK",
                           Charge, BattleCry, Chastise, Vigilance),
        ChampionMage("        K S\n"+
                        "     C M   M C",
                        BattleCry, Vigilance, Regenerate),
        Championeer("     C C C\n"+
                       "      S S",
                       BattleCry, Charge, Regenerate, ShieldWall),
        ChampioneerKiller("   H C H\n"+
                             "    H H\n"+
                             "  M S S A",
                             Charge, BattleCry, ShieldWall, Chastise),
        Devastation("       KCC M\n"+
                       "        S A",
                       Charge, ShieldWall, BattleCry, Stoicism),
        FakeBombard("   S S C C S S\n"+
                       "     S S S S",
                       Charge, BattleCry, Regenerate, Stoicism), // Leaves 25 setup points unused
        WindsOfStab("   S S C C S S\n"+
                       "    S S H S S",
                       Charge, BattleCry, ShieldWall, WindsOfFate),
        HealthOverDamage("     K CM CM F",
                         Stoicism, Charge, ShieldWall, Vigilance),
        ItsATrap("    S S S S S\n"+
                    "        A\n"+
                    "    M  K C  M",
                    BattleCry, Stoicism, Charge, ShieldWall),
        OneLessChampion("    M C M\n"+
                           "   S H H S",
                           Bombard, Vigilance, Charge, BattleCry),
        TwoTwoTwo("\n"+
                     "\n"+
                     "      CS AA CS",
                     Chastise, BattleCry, Stoicism, Regenerate),
        GlassCannon("       C\n"+
                       "    M A A M"+
                       "   S S S S S",
                       Charge, BattleCry, Stoicism, Chastise),
        WrathOfBombard("   S H  C  H S"+
                          "    S S H S S",
                          BattleCry, Charge, Stoicism, Bombard),
        Venom("    M A H A M\n"+
                 "     K S S K",
                 Stoicism, BattleCry, Charge, ShieldWall),
        MeleeMeltdown("      H CHC H\n"+
                         "     S S H S S",
                         BattleCry, Charge, Chastise, ShieldWall),
        HalberderHedge("    H K C K H\n"+
                          "      H S H",
                          BattleCry, Charge, Chastise, Stoicism),
        HalberderHedgeVariant("    H K C H H\n"+
                                 "     H S S H",
                                 BattleCry, Charge, ShieldWall, Stoicism),
        OutrangeOutplay("        HMH\n"+
                           "        SCS\n"+
                           "S       SSS",
                           Charge, BattleCry, Chastise, Bombard),
        SuperChampioneer("      C C C C",
                            Regenerate, Vigilance),
        AntiMelee("     SAMAKMAMS",
                     Chastise, BattleCry, Charge, Vigilance),
        IAmWarrior("        C C C",
                      Charge, Stoicism, Regenerate, BattleCry),
        HalberderPure("       H  H  H\n" +
                         "    H S H H H S H",
                         Charge, BattleCry, Chastise, Stoicism),
        Empty(""); // Used when needing the player to set up troops in their first turn.

        public final String troops;
        public final HashSet<CommandType> commands;
        Setup(String tr, CommandType c1, CommandType c2, CommandType c3, CommandType c4) {
            this.troops = tr;
            this.commands = new HashSet<>(Set.of(c1, c2, c3, c4));
        }
        Setup(String tr, CommandType c1, CommandType c2, CommandType c3) {
            this.troops = tr;
            this.commands = new HashSet<>(Set.of(c1, c2, c3));
        }
        Setup(String tr, CommandType c1, CommandType c2) {
            this.troops = tr;
            this.commands = new HashSet<>(Set.of(c1, c2));
        }
        Setup(String tr) {
            this.troops = tr;
            this.commands = new HashSet<>();
        }
    }

    public CQParameters() {
        setups = Arrays.asList(
            Setup.Test,
            Setup.Default,
            Setup.Rush,
            Setup.AntiRush,
            Setup.FootySpam,
            Setup.RegenerateSetup,
            Setup.WindsOfFateSetup,
            Setup.BombardSetup,
            Setup.DefaultVariantArcher,
            Setup.DefaultVariant1,
            Setup.DefaultVariant2,
            Setup.ChampionMage,
            Setup.Championeer,
            Setup.ChampioneerKiller,
            Setup.Devastation,
            Setup.FakeBombard,
            Setup.WindsOfStab,
            Setup.HealthOverDamage,
            Setup.ItsATrap,
            Setup.OneLessChampion,
            Setup.TwoTwoTwo,
            Setup.GlassCannon,
            Setup.WrathOfBombard,
            Setup.Venom,
            Setup.MeleeMeltdown,
            Setup.HalberderHedge,
            Setup.HalberderHedgeVariant,
            Setup.OutrangeOutplay,
            Setup.SuperChampioneer,
            Setup.AntiMelee,
            Setup.IAmWarrior,
            Setup.HalberderPure
        );
        if (testSetup) {
            p0TroopSetup = Setup.Empty;
            p1TroopSetup = Setup.Empty;
        } else {
            p0TroopSetup = Setup.HalberderHedgeVariant;
            p1TroopSetup = Setup.HalberderPure;
        }
        addTunableParameter("p0TroopSetup", p0TroopSetup, setups);
        addTunableParameter("p1TroopSetup", p1TroopSetup, setups);
    }

    @Override
    protected TunableParameters _copy() {
        CQParameters copy = new CQParameters();
        copy.p0TroopSetup = this.p0TroopSetup;
        copy.p1TroopSetup = this.p1TroopSetup;
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CQParameters && ((CQParameters) o).p0TroopSetup == p0TroopSetup && ((CQParameters) o).p1TroopSetup == p1TroopSetup;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public Object instantiate() {
        return GameType.Conquest.createGameInstance(2, this);
    }

    @Override
    public void _reset() {
        p0TroopSetup = (Setup) getParameterValue("p0TroopSetup");
        p1TroopSetup = (Setup) getParameterValue("p1TroopSetup");
    }
}

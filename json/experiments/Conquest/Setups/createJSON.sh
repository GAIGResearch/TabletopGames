#/bin/bash

names=(
  Default Rush AntiRush FootySpam RegenerateSetup WindsOfFateSetup BombardSetup
  DefaultVariantArcher DefaultVariant1 DefaultVariant2 ChampionMage Championeer
  ChampioneerKiller Devastation FakeBombard WindsOfStab HealthOverDamage ItsATrap
  OneLessChampion TwoTwoTwo GlassCannon WrathOfBombard Venom MeleeMeltdown
  HalberderHedge HalberderHedgeVariant OutrangeOutplay SuperChampioneer AntiMelee
  IAmWarrior HalberderPure
)

for i in "${!names[@]}"; do
  name="${names[i]}"
  cat > "${name}.json" <<EOL
{
  "rolloutIncrementType" : "TURN",
  "rolloutTermination" : "END_TURN",
  "rolloutLength" : 4,
  "actionHeuristic" : {
    "class" : "games.conquest.players.CQRandomSearchHeuristic"
  },
  "rolloutType": "PARAMS",
  "rolloutPolicyParams" : {
    "class" : "players.simple.BoltzmannActionParams"
  },
  "FPU" : 10.0,
  "K" : 0.5,
  "class" : "players.mcts.MCTSParams",
  "maxTreeDepth" : 3,
  "budget" : 3000,
  "treePolicy" : "AlphaGo",
  "reuseTree" : true,
  "instantiationClass": "games.conquest.players.CQSetupPlayer",
  "omaVisits": $i
}
EOL
done

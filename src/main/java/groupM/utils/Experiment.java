package groupM.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.hadoop.shaded.com.nimbusds.jose.shaded.json.JSONObject;

/**
 * Class to create experiment directories easily for sushi go assigment
 * 
 * Will create a directory in the root/experiments folder of this repo
 * Will contain:
 * - config file to provide to 'RunGames' class (experiment.json)
 * - results directory wheree the results will go
 * - agents folder with n GroupMMCTS agents 
 * 
 * Steps to use:
 * 1) Edit the hyperparameters in this class
 * 2) Run the main method of the class to create the directory
 * 3) Manually edit the agent json files to configure your experiment (for hyperparams not included in this class).
 * 4) Remember to edit the config argument to the runGames method to run your experiment.
 */
public class Experiment {
    // experiment config
    public static String directory = "testing";
    public static int nAgents = 2;
    public static int matchups = 25;
    public static long seed = 4567654;

    // Group M MCTS config
    public static String budgetType = "BUDGET_TIME";
    public static long budget = 40;
    public static String explorationStrategy = "UCB1";

    public static void main(String[] args){
        
        Path experimentDir = makeExperimentDir(directory);
        if(experimentDir == null) return;

        for (int i=0; i<nAgents; i++){
            if(!makeAgents(experimentDir, i)){
                return;
            }
        }

        if(!makeExperimentFile(experimentDir)){
            return;
        }

        System.out.println("Sucessfully create experiment");
        System.out.println(experimentDir.toAbsolutePath()+"/experiment.json");

    }

    private static Path makeExperimentDir(String dir){
        try {
            Path rootPath = FileSystems.getDefault().getPath("").toAbsolutePath();

            // create root experiment folder
            Path experimentsRoot = Paths.get(rootPath + "/experiments/");
            Files.createDirectories(experimentsRoot);
            
            Path experimentPath = Paths.get(rootPath + "/experiments/"+dir);
            Path experimentDir = Files.createDirectory(experimentPath);
            
            Files.createDirectory(Paths.get(experimentDir + "/agents"));
            Files.createDirectory(Paths.get(experimentDir + "/results"));

            return experimentDir;
        }
        catch(IOException e) {
            System.out.println("Experiment "+ dir +" already exists!");
            return null;
        }
    }

    private static boolean makeExperimentFile(Path experimentDir){
        JSONObject experimentJson = new JSONObject();
        experimentJson.put("game", "SushiGo");
        experimentJson.put("nPlayers", 2);
        experimentJson.put("mode", "exhaustive");
        experimentJson.put("verbose", false);
        experimentJson.put("listener", "json/listeners/basiclistener.json");
        
        experimentJson.put("seed", seed);
        experimentJson.put("matchups", matchups);
        experimentJson.put("destDir", experimentDir.toString()+"/results");
        experimentJson.put("output", experimentDir.toString()+"/results/Tournament.log");
        experimentJson.put("playerDirectory", experimentDir.toString()+"/agents");

        return writeToFile(experimentDir, "experiment.json", experimentJson);
       
    }

    private static boolean makeAgents(Path experimentDir, int i){
        JSONObject agentJson = new JSONObject();
        agentJson.put("class", "groupM.players.mcts.GroupMMCTSParams");
        agentJson.put("K", 1);
        agentJson.put("rolloutLength", 0);
        agentJson.put("maxTreeDepth", 30);
        agentJson.put("name", "Experiment MCTS " + i);


        JSONObject heuristic = new JSONObject();
        heuristic.put("class", "players.heuristics.ScoreHeuristic");
        agentJson.put("heuristic", heuristic);
        agentJson.put("explorationStrategy", explorationStrategy);

        return writeToFile(Paths.get(experimentDir+"/agents"), "agent"+i+".json", agentJson);
       
    }

    private static boolean writeToFile(Path dir, String filename, JSONObject jsonObject){
        try {
            FileWriter file = new FileWriter(dir.toString() + "/" +filename);
            file.write(jsonObject.toJSONString());
            file.close();
            return true;
        } catch (IOException e){
            return false;
        }
    }

        
}

 
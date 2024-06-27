package llm;

public class TaskPrompter {
    String gameName;

    String promptsDir;

    public TaskPrompter(String gameName, String promptsDir)
    {
        this.gameName = gameName;
        this.promptsDir = promptsDir;
    }

    public String getTaskPrompt(){return null;}

    public String getFeedbackPrompt(){return null;}

}

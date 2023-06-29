package players.rl;

import java.nio.file.Path;
import java.nio.file.Paths;

public class QWDSParams {

    public boolean useSettingsFromInfile = false;

    private final String infileName;
    protected Path infilePath;

    public QWDSParams(String infileNameOrAbsPath) {
        this.infileName = infileNameOrAbsPath;
    }

    protected void initInFilePath(String defaultPath) {
        infilePath = infileName == null ? null
                : Paths.get(infileName).isAbsolute() ? Paths.get(infileName)
                        : Paths.get(defaultPath, infileName).toAbsolutePath();
    }

    public String getInfilePath() {
        return infilePath == null ? null : infilePath.toString();
    }

}

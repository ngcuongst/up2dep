package model.libInfo;


import java.util.ArrayList;

public class LibStats {
    private final String pluginId;
    private final String projectName;
    private final String libName;
    private final ArrayList<String> quickfixes;
    public LibStats(String pluginId, String projectName, String libName, ArrayList<String> quickfixes) {
        this.pluginId = pluginId;
        this.projectName =projectName;
        this.libName = libName;
        this.quickfixes = quickfixes;
    }

}

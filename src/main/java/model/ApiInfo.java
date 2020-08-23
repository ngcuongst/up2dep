package model;

import com.intellij.openapi.project.Project;
import util.LibraryDependenciesBuilder;
import util.LibraryUsageHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ApiInfo {
    private String api;
    private HashSet<String> libVersions;
    private String libName;

    public ApiInfo(Project project, String api, HashMap<String, HashSet<String>> libVersions) {
        this.api = api;
        LibraryDependenciesBuilder libraryUsageHelper = LibraryDependenciesBuilder.getInstance(project);
        for (Map.Entry<String, HashSet<String>> entry : libVersions.entrySet()) {
            String currentLibName = entry.getKey();

            if (libraryUsageHelper.isLibIncluded(currentLibName)) {
                this.libName = currentLibName;
                this.libVersions = entry.getValue();
            }
        }
    }

    public String getApi() {
        return api;
    }

    public HashSet<String> getVersions() {
        return this.libVersions;
    }

    public String getLibName() {
        return this.libName;
    }
}

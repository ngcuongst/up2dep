package model.libInfo;


import model.gradle.GradleCoordinate;
import java.util.HashMap;

public class TransLibInfo {

    private GradleCoordinate parentLib;
    private HashMap<String, UsedLibInfo> libUsedInfos;


    public TransLibInfo(GradleCoordinate parentLib,  HashMap<String, UsedLibInfo> libUsedInfos) {
        this.parentLib = parentLib;
        this.libUsedInfos = libUsedInfos;
    }

    public HashMap<String, UsedLibInfo> getLibUsedInfos() {
        return libUsedInfos;
    }
}

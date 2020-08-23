package model.gradle;

import model.gradle.LocalGradleCoordinate;

public class TransitiveLocalGradleCoordinate extends LocalGradleCoordinate {
    private LocalGradleCoordinate parentLib;
    public TransitiveLocalGradleCoordinate(String groupId, String artifactId, String revision, LocalGradleCoordinate parentLib) {
        super(groupId, artifactId, revision);
        this.parentLib = parentLib;
    }

    public LocalGradleCoordinate getParentLib() {
        return parentLib;
    }
}

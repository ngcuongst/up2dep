package model.gradle;

public interface  GradleCoordinate {
    String getName();
    void setIsUsed(boolean isUsed);
    boolean isUsed();
    String getRevision();
    String getGroupId();
    String getArtifactId();
}

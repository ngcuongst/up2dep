package model;

import com.intellij.psi.PsiElement;
import model.gradle.LocalGradleCoordinate;

public class PsiDependency {
    private PsiElement element;
    private LocalGradleCoordinate coordinate;
    private String newerVersion;

    public PsiDependency(PsiElement element, LocalGradleCoordinate coordinate, String newerVersion){
        this.element = element;
        this.coordinate = coordinate;
        this.newerVersion = newerVersion;
    }

    public PsiElement getElement() {
        return element;
    }

    public LocalGradleCoordinate getCoordinate() {
        return coordinate;
    }

    public String getNewerVersion() {
        return newerVersion;
    }
}

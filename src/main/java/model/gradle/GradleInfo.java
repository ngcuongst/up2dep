package model.gradle;

import com.intellij.psi.PsiElement;
import model.gradle.LocalGradleCoordinate;

public class GradleInfo {
    private final PsiElement element;
    private final LocalGradleCoordinate gradleCoordinate;

    public GradleInfo(PsiElement dependencyElement, LocalGradleCoordinate gradleCoordinate){
        this.element = dependencyElement;
        this.gradleCoordinate = gradleCoordinate;
    }

    public PsiElement getDependencyElement() {
        return element;
    }

    public LocalGradleCoordinate getGradleCoordinate() {
        return gradleCoordinate;
    }
}

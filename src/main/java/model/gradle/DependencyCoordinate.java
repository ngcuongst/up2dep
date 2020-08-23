package model.gradle;

import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

public class DependencyCoordinate {
    private LocalGradleCoordinate gradleCoordinate;
    private GrReferenceExpression referenceExpression;


    public DependencyCoordinate(LocalGradleCoordinate gradleCoordinate, GrReferenceExpression referenceExpression){
        this.gradleCoordinate = gradleCoordinate;
        this.referenceExpression = referenceExpression;
    }

    public LocalGradleCoordinate getGradleCoordinate() {
        return gradleCoordinate;
    }

    public GrReferenceExpression getReferenceExpression() {
        return referenceExpression;
    }
}

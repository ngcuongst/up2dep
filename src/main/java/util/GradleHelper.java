package util;

import com.intellij.psi.PsiElement;
import model.gradle.GradleInfo;
import model.gradle.LocalGradleCoordinate;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.*;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringInjection;


import java.util.HashMap;

public class GradleHelper {

    public static GradleInfo extractCoordinate(PsiElement element, HashMap<String, String> extraProperties) {
        GradleInfo libDetail = null;
        try {
            if (element instanceof GrApplicationStatement) {
                GrApplicationStatement call = (GrApplicationStatement) element;
                GrExpression propertyExpression = call.getInvokedExpression();
                GrCommandArgumentList argumentList = call.getArgumentList();
                if (propertyExpression instanceof GrReferenceExpression) {
                    GrReferenceExpression propertyRef = (GrReferenceExpression) propertyExpression;
                    String property = propertyRef.getReferenceName();
                    //noinspection ConstantConditions
                    if (property != null && argumentList != null) {
                        String value = getDependencyValue(argumentList, extraProperties);
                        if(value != null) {
                            LocalGradleCoordinate coordinate = DependencyHelper.checkDependencies(value);
                            if (coordinate != null) {
                                libDetail = new GradleInfo(argumentList, coordinate);
                            }
                        }
                    }
                }
            } else if (element instanceof GrAssignmentExpression) {
                GrAssignmentExpression assignment = (GrAssignmentExpression) element;

                GrExpression lValue = assignment.getLValue();
                if (lValue instanceof GrReferenceExpression) {
                    GrReferenceExpression propertyRef = (GrReferenceExpression) lValue;
                    String property = propertyRef.getReferenceName();
                    if (property != null) {
                        GrExpression rValue = assignment.getRValue();
                        if (rValue != null) {
                            String value = rValue.getText();
                            LocalGradleCoordinate coordinate = DependencyHelper.checkDependencies(value);
                            if (coordinate != null) {
                                libDetail = new GradleInfo(rValue, coordinate);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            
        }
        return libDetail;
    }

    private static String getDependencyValue(GrCommandArgumentList argumentList, HashMap<String, String> extraProperties) {
        String value = argumentList.getText();
        GroovyPsiElement[] allArguments = argumentList.getAllArguments();
        if (allArguments.length == 1) {
            if (allArguments[0] instanceof GrString) {
                GrString argument = (GrString) allArguments[0];
                GrStringInjection[] injections = argument.getInjections();
                if (injections.length == 1) {
                    GrExpression expression = injections[0].getExpression();
                    if(expression == null){
                        //TODO consider version as variable
                        return null;
                    }
                    String key = expression.getText();
                    if (extraProperties.containsKey(key)) {

                        String tobeReplaced = injections[0].getText();
                        value = argument.getText().replace(tobeReplaced, extraProperties.get(key));
                    }
                }
            }
        }

        return value;
    }
}

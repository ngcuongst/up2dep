package inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import model.GeneralOnlineLibInfo;
import model.gradle.GradleInfo;
import model.gradle.LocalGradleCoordinate;
import model.libInfo.OnlineLibInfo;
import notification.Messages;
import notification.WarningType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import quickfix.CompatibleVersionQuickFix;
import quickfix.LatestVersionQuickFix;
import quickfix.ShowDependenciesQuickFix;
import util.DependencyHelper;
import util.GradleHelper;
import util.LibraryDependenciesBuilder;
import util.PsiElementHelper;
import util.localDb.SqliteHelper;

import java.util.ArrayList;
import java.util.HashMap;

class LibDependencyVisitor extends BaseInspectionVisitor {

    private static final String DEPENDENCIES_TAG = "dependencies";

    private HashMap<String, String> assignments = new HashMap<>();

    @Override
    public void visitAssignmentExpression(@NotNull GrAssignmentExpression expression) {
        super.visitAssignmentExpression(expression);

        GrExpression lValue = expression.getLValue();
        GrExpression rValue = expression.getRValue();
        if (rValue instanceof GrLiteral) {
            String key = lValue.getText();
            if (key.contains("ext.")) {
                key = key.replace("ext.", "");
            } else if (key.contains("project.ext.")) {
                key = key.replace("project.ext.", "");
            }
            //TODO currently ignore complex build string that refer to other vairables e.g, a = "$projectDir/src/main/assets/mmcmnc.sqlite"
            Object rawValue = ((GrLiteral) rValue).getValue();
            if (rawValue != null) {
                String value = rawValue.toString();
                assignments.put(key, value);
            }

        }
    }

    @Override
    public void visitClosure(@NotNull GrClosableBlock closure) {

        super.visitClosure(closure);

        Project project = closure.getProject();
        PsiFile file = closure.getContainingFile();
        SqliteHelper sqliteHelper = SqliteHelper.getInstance();

        if (file == null || !FileUtilRt.extensionEquals(file.getName(), GradleConstants.EXTENSION)) return;

        if (!file.isValid()) {
            return;
        }

        String parentName = PsiElementHelper.getClosureName(closure);
        if (parentName != null && parentName.equals(DEPENDENCIES_TAG)) {
            for (PsiElement element : closure.getChildren()) {
                try {
                    GradleInfo gradleInfo = GradleHelper.extractCoordinate(element, assignments);
                    if (gradleInfo != null && gradleInfo.getGradleCoordinate() != null) {
                        String libName = gradleInfo.getGradleCoordinate().getName();
                        

                        ApplicationManager.getApplication().runReadAction(() -> {
                            LibraryDependenciesBuilder libraryDependenciesBuilder = LibraryDependenciesBuilder.getInstance(project);
                            GeneralOnlineLibInfo generalOnlineLibInfo = libraryDependenciesBuilder.getLibUsage(gradleInfo.getGradleCoordinate());
//                            libraryDependenciesBuilder.getLibDependency(gradleInfo.getGradleCoordinate());

                            if (generalOnlineLibInfo != null) {
                                LocalQuickFix[] localQuickFixes = DependencyHelper.generateQuickfixes(gradleInfo, generalOnlineLibInfo);
                                if (localQuickFixes != null) {
                                    String message = DependencyHelper.getWarningMessage(gradleInfo, generalOnlineLibInfo);
                                    ProblemHighlightType problemHighlightType = DependencyHelper.getHighlightType(generalOnlineLibInfo);
                                    registerError(gradleInfo.getDependencyElement(), message, localQuickFixes, problemHighlightType);                                    
                                }

                            }
                        });
                    }
                } catch (Exception ex) {
                    
                }
            }


        }


    }


}

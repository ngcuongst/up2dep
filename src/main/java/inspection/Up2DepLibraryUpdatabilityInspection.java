package inspection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection;
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor;

public class Up2DepLibraryUpdatabilityInspection extends GradleBaseInspection {

    @Override
    protected @NotNull
    BaseInspectionVisitor buildVisitor() {
        return new LibDependencyVisitor();
    }

}

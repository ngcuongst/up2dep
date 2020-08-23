package ui.usages;

import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageView;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

public class UsageTargetNode extends Node {
    UsageTargetNode(@NotNull UsageTarget target) {

        setUserObject(target);
    }

    @Override
    public String tree2string(int indent, String lineSeparator) {
        return getTarget().getName();
    }

    @Override
    protected boolean isDataValid() {
        return getTarget().isValid();
    }

    @Override
    protected boolean isDataReadOnly() {
        return getTarget().isReadOnly();
    }

    @Override
    protected boolean isDataExcluded() {
        return false;
    }

    @NotNull
    @Override
    protected String getText(@NotNull final UsageView view) {
        return ObjectUtils.notNull(getTarget().getPresentation().getPresentableText(), "");
    }

    @NotNull
    public UsageTarget getTarget() {
        return (UsageTarget) getUserObject();
    }

    @Override
    protected void updateNotify() {
        super.updateNotify();
        getTarget().update();
    }
}

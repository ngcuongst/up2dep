package ui.usages;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.usages.UsageView;
import com.intellij.usages.rules.UsageFilteringRuleProvider;
import org.jetbrains.annotations.NotNull;
import ui.usages.LibUsageView;

import javax.swing.*;

abstract class RuleAction extends ToggleAction implements DumbAware {
    protected final LibUsageView myView;
    private boolean myState;

    RuleAction(@NotNull UsageView view, @NotNull String text, @NotNull Icon icon) {
        super(text, null, icon);
        myView = (LibUsageView) view;
        myState = getOptionValue();
    }

    protected abstract boolean getOptionValue();

    protected abstract void setOptionValue(boolean value);

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myState;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        setOptionValue(state);
        myState = state;

        Project project = e.getProject();
        if (project != null) {
            project.getMessageBus().syncPublisher(UsageFilteringRuleProvider.RULES_CHANGED).run();
        }

        myView.select();
    }
}

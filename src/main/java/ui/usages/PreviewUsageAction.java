package ui.usages;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewSettings;
import org.jetbrains.annotations.NotNull;

public class PreviewUsageAction extends RuleAction

{
    PreviewUsageAction(@NotNull UsageView usageView) {
        super(usageView, UsageViewBundle.message("preview.usages.action.text", StringUtil.capitalize(StringUtil.pluralize(usageView.getPresentation().getUsagesWord()))),
                AllIcons.Actions.PreviewDetails);
    }

    @Override
    protected boolean getOptionValue() {
        return UsageViewSettings.getInstance().isPreviewUsages();
    }

    @Override
    protected void setOptionValue(final boolean value) {
        UsageViewSettings.getInstance().setPreviewUsages(value);
    }

}

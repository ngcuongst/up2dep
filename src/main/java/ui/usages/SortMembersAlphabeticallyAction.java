package ui.usages;


import com.intellij.icons.AllIcons;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.UsageViewSettings;

public class SortMembersAlphabeticallyAction extends RuleAction {

    SortMembersAlphabeticallyAction(LibUsageView usageView) {
        super(usageView, UsageViewBundle.message("sort.alphabetically.action.text"), AllIcons.ObjectBrowser.Sorted);
    }

    @Override
    protected boolean getOptionValue() {
        return myView.getUsageViewSettings().isSortAlphabetically();
    }

    @Override
    protected void setOptionValue(final boolean value) {
        myView.getUsageViewSettings().setSortAlphabetically(value);
    }

}

package ui.usages;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.rules.UsageFilteringRule;
import com.intellij.usages.rules.UsageGroupingRule;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class UsageNodeTreeBuilder {
    private final GroupNode myRoot;
    private final Project myProject;
    private final UsageTarget[] myTargets;
    private UsageGroupingRule[] myGroupingRules;
    private UsageFilteringRule[] myFilteringRules;

    UsageNodeTreeBuilder(@NotNull UsageTarget[] targets,
                         @NotNull UsageGroupingRule[] groupingRules,
                         @NotNull UsageFilteringRule[] filteringRules,
                         @NotNull GroupNode root,
                         @NotNull Project project) {
        myTargets = targets;
        myGroupingRules = groupingRules;
        myFilteringRules = filteringRules;
        myRoot = root;
        myProject = project;
    }

    public void setGroupingRules(@NotNull UsageGroupingRule[] rules) {
        myGroupingRules = rules;
    }

    void setFilteringRules(@NotNull UsageFilteringRule[] rules) {
        myFilteringRules = rules;
    }

    public boolean isVisible(@NotNull Usage usage) {
        return Arrays.stream(myFilteringRules).allMatch(rule -> rule.isVisible(usage, myTargets));
    }

    UsageNode appendUsage(@NotNull Usage usage, @NotNull Consumer<Node> edtInsertedUnderQueue, boolean filterDuplicateLines) {
        if (!isVisible(usage)) return null;

        final boolean dumb = DumbService.isDumb(myProject);

        GroupNode groupNode = myRoot;
        for (int i = 0; i < myGroupingRules.length; i++) {
            UsageGroupingRule rule = myGroupingRules[i];
            if (dumb && !DumbService.isDumbAware(rule)) continue;

            List<UsageGroup> groups = rule.getParentGroupsFor(usage, myTargets);
            for (UsageGroup group : groups) {
                groupNode = groupNode.addOrGetGroup(group, i, edtInsertedUnderQueue);
            }
        }

        return groupNode.addUsage(usage, edtInsertedUnderQueue, filterDuplicateLines, null);
    }
}

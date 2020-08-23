package ui.usages;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.usages.LibUsageView;
import ui.usages.LibUsageViewManager;
import util.UsageInfoToUsageConverter;

import javax.swing.*;
import java.awt.*;

public abstract class UsagesPanel extends JPanel implements Disposable, DataProvider {
    protected static final Logger LOG = Logger.getInstance("#com.intellij.packageDependencies.ui.usages.UsagesPanel");

    private final Project myProject;
    ProgressIndicator myCurrentProgress;
    private JComponent myCurrentComponent;
    private LibUsageView myCurrentUsageView;
    protected final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

    public UsagesPanel(@NotNull Project project) {
        super(new BorderLayout());
        myProject = project;
    }

    public void setToInitialPosition() {
        cancelCurrentFindRequest();
        setToComponent(createLabel(getInitialPositionText()));
    }

    public abstract String getInitialPositionText();

    public abstract String getCodeUsagesString();


    void cancelCurrentFindRequest() {
        if (myCurrentProgress != null) {
            myCurrentProgress.cancel();
        }
    }

    void showUsages(@NotNull PsiElement[] primaryElements, @NotNull UsageInfo[] usageInfos) {
        if (myCurrentUsageView != null) {
            Disposer.dispose(myCurrentUsageView);
        }
        try {
            Usage[] usages = UsageInfoToUsageConverter.convert(primaryElements, usageInfos);
            UsageViewPresentation presentation = new UsageViewPresentation();

            presentation.setCodeUsagesString(getCodeUsagesString());
            myCurrentUsageView = LibUsageViewManager.getInstance(myProject).createUsageView(UsageTarget.EMPTY_ARRAY, usages, presentation, null);
            setToComponent(myCurrentUsageView.getComponent());
        } catch (ProcessCanceledException e) {
            setToCanceled();
        }
    }

    private void setToCanceled() {
        setToComponent(createLabel(AnalysisScopeBundle.message("usage.view.canceled")));
    }

    void setToComponent(final JComponent cmp) {
        SwingUtilities.invokeLater(() -> {
            if (myProject.isDisposed()) return;
            if (myCurrentComponent != null) {
                if (myCurrentUsageView != null && myCurrentComponent == myCurrentUsageView.getComponent()) {
                    Disposer.dispose(myCurrentUsageView);
                    myCurrentUsageView = null;
                }
                remove(myCurrentComponent);
            }
            myCurrentComponent = cmp;
            add(cmp, BorderLayout.CENTER);
            revalidate();
        });
    }

    @Override
    public void dispose() {
        if (myCurrentUsageView != null) {
            Disposer.dispose(myCurrentUsageView);
            myCurrentUsageView = null;
        }
    }

    private static JComponent createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    @Override
    @Nullable
    @NonNls
    public Object getData(@NonNls String dataId) {
        if (PlatformDataKeys.HELP_ID.is(dataId)) {
            return "ideaInterface.find";
        }
        return null;
    }
}

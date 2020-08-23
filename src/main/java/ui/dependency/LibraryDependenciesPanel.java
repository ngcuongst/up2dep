package ui.dependency;

import com.intellij.CommonBundle;
import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.analysis.PerformAnalysisInBackgroundOption;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.ExporterToTextFile;
import com.intellij.ide.actions.ContextHelpAction;
import com.intellij.ide.impl.FlattenModulesToggleAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.packageDependencies.actions.AnalyzeDependenciesHandler;
import com.intellij.packageDependencies.actions.BackwardDependenciesHandler;
import com.intellij.packageDependencies.DependenciesBuilder;
import com.intellij.packageDependencies.ui.DependenciesPanel;
import com.intellij.packageDependencies.ui.TreeExpansionMonitor;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.packageDependencies.ui.PackageTreeExpansionMonitor;
import com.intellij.packageDependencies.ui.PatternDialectProvider;
import com.intellij.packageDependencies.ui.FileTreeModelBuilder;
import com.intellij.packageDependencies.ui.DependencyConfigurable;
import com.intellij.packageDependencies.ui.Marker;
import com.intellij.packageDependencies.ui.FileNode;

import com.intellij.packageDependencies.DependencyRule;
import com.intellij.packageDependencies.DependencyUISettings;
import com.intellij.packageDependencies.DependenciesToolWindow;
import com.intellij.packageDependencies.BackwardDependenciesBuilder;
import com.intellij.packageDependencies.ForwardDependenciesBuilder;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.content.Content;
import com.intellij.ui.SmartExpander;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.TreeSpeedSearch;

import com.intellij.usageView.UsageViewBundle;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.PlatformIcons;
import com.intellij.util.Processor;
import com.intellij.util.SystemProperties;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;


import model.GeneralOnlineLibInfo;
import model.libInfo.OnlineLibInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.ui.treeStructure.Tree;
import ui.usages.LibraryDependenciesUsagesPanel;
import util.LibraryDependenciesBuilder;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.List;

public class LibraryDependenciesPanel extends JPanel implements Disposable {
    private final Map<PsiFile, Set<PsiFile>> myDependencies;
    private final Set<VirtualFile> incompatibileMethods;
    private Map<VirtualFile, Map<DependencyRule, Set<PsiFile>>> myIllegalDependencies;
    private final MyTree myLeftTree = new MyTree();
    private final LibraryDependenciesUsagesPanel myUsagesPanel;

    private static final HashSet<PsiFile> EMPTY_FILE_SET = new HashSet<>(0);
    private final TreeExpansionMonitor myLeftTreeExpansionMonitor;

    private final Marker myLeftTreeMarker;

    private final Project myProject;
    private final LibraryDependenciesBuilder myBuilder;
    private Content myContent;
    private final DependenciesPanel.DependencyPanelSettings mySettings = new DependenciesPanel.DependencyPanelSettings();
    private static final Logger LOG = Logger.getInstance(DependenciesPanel.class);

    private final boolean myForward;
    private final AnalysisScope myScopeOfInterest;
    private final int myTransitiveBorder;
    private final OnlineLibInfo libInfo;

    public LibraryDependenciesPanel(Project project, GeneralOnlineLibInfo generalOnlineLibInfo) {
        super(new BorderLayout());
        this.libInfo = generalOnlineLibInfo.getOnlineLibInfo();
        LibraryDependenciesBuilder dependenciesBuilder = LibraryDependenciesBuilder.getInstance(project);
        dependenciesBuilder.analyzeLibDependency(generalOnlineLibInfo, this.libInfo.getCurrentVersion().toString());
        this.myBuilder = dependenciesBuilder;
        final DependenciesBuilder main = this.myBuilder;
        this.myForward = !main.isBackward();
        this.myScopeOfInterest = main.getScopeOfInterest();
        this.myTransitiveBorder = main.getTransitiveBorder();
        this.myDependencies = new HashMap<>();
        this.incompatibileMethods = new HashSet<>();
        this.myIllegalDependencies = new HashMap<>();
        myDependencies.putAll(myBuilder.getDependencies());
//        putAllDependencies(myBuilder);

//        exclude(excluded);
        this.myProject = project;
        this.myUsagesPanel = new LibraryDependenciesUsagesPanel(this.myProject, this.myBuilder);
        Disposer.register(this, this.myUsagesPanel);

        final Splitter splitter = new Splitter(true);
        Disposer.register(this, splitter::dispose);
        splitter.setFirstComponent(myLeftTree);
        splitter.setSecondComponent(myUsagesPanel);
        add(splitter, BorderLayout.CENTER);
        add(createToolbar(), BorderLayout.NORTH);

        this.myLeftTreeExpansionMonitor = PackageTreeExpansionMonitor.install(myLeftTree, myProject);


        this.myLeftTreeMarker = file -> myIllegalDependencies.containsKey(file);

        updateLeftTreeModel();

        myLeftTree.getSelectionModel().addTreeSelectionListener(e -> {
            final StringBuffer denyRules = new StringBuffer();
            final StringBuffer allowRules = new StringBuffer();
            final TreePath[] paths = myLeftTree.getSelectionPaths();
            if (paths == null) {
                return;
            }
            for (TreePath path : paths) {
                PackageDependenciesNode selectedNode = (PackageDependenciesNode) path.getLastPathComponent();
                traverseToLeaves(selectedNode, denyRules, allowRules);
            }
            if (denyRules.length() + allowRules.length() > 0) {
                StatusBar.Info.set(AnalysisScopeBundle.message("status.bar.rule.violation.message",
                        ((denyRules.length() == 0 || allowRules.length() == 0) ? 1 : 2),
                        (denyRules.length() > 0 ? denyRules.toString() + (allowRules.length() > 0 ? "; " : "") : " ") +
                                (allowRules.length() > 0 ? allowRules.toString() : " ")), myProject);
            } else {
                StatusBar.Info.set(AnalysisScopeBundle.message("status.bar.no.rule.violation.message"), myProject);
            }

            final Set<PsiFile> searchIn = getSelectedScope(myLeftTree);
            if (searchIn.size() > 0 && this.libInfo.getUsedLibFiles().size() > 0) {
                myUsagesPanel.findUsages(searchIn, this.libInfo);
            }
        });


        initTree(myLeftTree, false);
//        initTree(myRightTree, true);

        setEmptyText(mySettings.UI_FILTER_LEGALS);

        AnalysisScope scope = myBuilder.getScope();
        if (scope.getScopeType() == AnalysisScope.FILE) {
            Set<PsiFile> oneFileSet = myDependencies.keySet();
            if (oneFileSet.size() == 1) {
                selectElementInLeftTree(oneFileSet.iterator().next());
                return;
            }
        }

        TreeUtil.selectFirstNode(myLeftTree);
    }

    private void putAllDependencies(DependenciesBuilder builder) {
        final Map<PsiFile, Map<DependencyRule, Set<PsiFile>>> dependencies = builder.getIllegalDependencies();
        for (Map.Entry<PsiFile, Map<DependencyRule, Set<PsiFile>>> entry : dependencies.entrySet()) {
            myIllegalDependencies.put(entry.getKey().getVirtualFile(), entry.getValue());
        }
    }

    private void processDependencies(final Set<PsiFile> searchIn, final Set<PsiFile> searchFor, Processor<List<PsiFile>> processor) {
        if (myTransitiveBorder == 0) return;
        Set<PsiFile> initialSearchFor = new HashSet<>(searchFor);
        for (PsiFile from : searchIn) {
            for (PsiFile to : initialSearchFor) {
                final List<List<PsiFile>> paths = myBuilder.findPaths(from, to);
                Collections.sort(paths, (p1, p2) -> p1.size() - p2.size());
                for (List<PsiFile> path : paths) {
                    if (!path.isEmpty()) {
                        path.add(0, from);
                        path.add(to);
                        if (!processor.process(path)) return;
                    }
                }
            }
        }

    }

    private void exclude(final Set<PsiFile> excluded) {
        for (PsiFile psiFile : excluded) {
            myDependencies.remove(psiFile);
            myIllegalDependencies.remove(psiFile);
        }
    }

    private void traverseToLeaves(final PackageDependenciesNode treeNode, final StringBuffer denyRules, final StringBuffer allowRules) {
        final Enumeration enumeration = treeNode.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            PsiElement childPsiElement = ((PackageDependenciesNode) enumeration.nextElement()).getPsiElement();
            if (myIllegalDependencies.containsKey(childPsiElement)) {
                final Map<DependencyRule, Set<PsiFile>> illegalDeps = myIllegalDependencies.get(childPsiElement);
                for (final DependencyRule rule : illegalDeps.keySet()) {
                    if (rule.isDenyRule()) {
                        if (denyRules.indexOf(rule.getDisplayText()) == -1) {
                            denyRules.append(rule.getDisplayText());
                            denyRules.append("\n");
                        }
                    } else {
                        if (allowRules.indexOf(rule.getDisplayText()) == -1) {
                            allowRules.append(rule.getDisplayText());
                            allowRules.append("\n");
                        }
                    }
                }
            }
        }
    }

    private JComponent createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new CloseAction());
        group.add(new RerunAction(this));
        group.add(new FlattenPackagesAction());
        group.add(new ShowFilesAction());
        if (ModuleManager.getInstance(myProject).getModules().length > 1) {
            group.add(new ShowModulesAction());
            group.add(createFlattenModulesAction());
            if (ModuleManager.getInstance(myProject).hasModuleGroups()) {
                group.add(new ShowModuleGroupsAction());
            }
        }
        group.add(new GroupByScopeTypeAction());
        //group.add(new GroupByFilesAction());
        group.add(new FilterLegalsAction());
//        group.add(new MarkAsIllegalAction());
        group.add(new ChooseScopeTypeAction());
        group.add(new EditDependencyRulesAction());
        group.add(CommonActionsManager.getInstance().createExportToTextFileAction(new DependenciesExporterToTextFile()));
        group.add(new ContextHelpAction("dependency.viewer.tool.window"));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("PackageDependencies", group, true);
        return toolbar.getComponent();
    }

    @NotNull
    private FlattenModulesToggleAction createFlattenModulesAction() {
        return new FlattenModulesToggleAction(myProject, () -> mySettings.UI_SHOW_MODULES, () -> !mySettings.UI_SHOW_MODULE_GROUPS, (value) -> {
            DependencyUISettings.getInstance().UI_SHOW_MODULE_GROUPS = !value;
            mySettings.UI_SHOW_MODULE_GROUPS = !value;
            rebuild();
        });
    }

    private void rebuild() {
        myIllegalDependencies = new HashMap<>();
        putAllDependencies(myBuilder);
        updateLeftTreeModel();
    }

    private void initTree(final MyTree tree, boolean isRightTree) {
        tree.setCellRenderer(new MyTreeCellRenderer(libInfo));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        UIUtil.setLineStyleAngled(tree);

        TreeUtil.installActions(tree);
        SmartExpander.installOn(tree);
        EditSourceOnDoubleClickHandler.install(tree);
        new TreeSpeedSearch(tree);

        PopupHandler.installUnknownPopupHandler(tree, createTreePopupActions(isRightTree), ActionManager.getInstance());


    }


    private ActionGroup createTreePopupActions(boolean isRightTree) {
        DefaultActionGroup group = new DefaultActionGroup();
        final ActionManager actionManager = ActionManager.getInstance();
        group.add(actionManager.getAction(IdeActions.ACTION_EDIT_SOURCE));
        group.add(actionManager.getAction(IdeActions.GROUP_VERSION_CONTROLS));

        if (isRightTree) {
            group.add(actionManager.getAction(IdeActions.GROUP_ANALYZE));
        } else {
            group.add(new RemoveFromScopeAction());
        }

        return group;
    }

    private TreeModel buildTreeModel(Set<PsiFile> deps, Marker marker) {
        return PatternDialectProvider.getInstance(mySettings.SCOPE_TYPE).createTreeModel(myProject, deps, marker,
                mySettings);
    }

    private void updateLeftTreeModel() {
        //TODO work here to reduce the number of e.g, usages
        Set<String> incompatibleAPIs = new HashSet<>();
        if(libInfo.getAlternatives() != null) {
            incompatibleAPIs = libInfo.getAlternatives().keySet();
        }
//        Set<PsiFile> usedLibFiles = libInfo.getUsedLibFiles();
//        Set<PsiFile> fileUsedAPIs = libInfo.getFileUsedAPi().keySet();
        Set<PsiFile> incompatibleFileUses = new HashSet<>();
        for(Map.Entry<PsiFile, HashSet<String>> entry: libInfo.getFileUsedAPi().entrySet()){
            Set<String> usedAPIs = entry.getValue();
            if(incompatibleAPIs.size() > 0) {
                usedAPIs.retainAll(incompatibleAPIs);
            }
            if(!usedAPIs.isEmpty()){
                incompatibleFileUses.add(entry.getKey());
            }

        }
//        Set<VirtualFile> usedLibVirtualFiles = new HashSet<>();
//        for(PsiFile libFile: usedLibFiles){
//            VirtualFile virtualFile = libFile.getVirtualFile();
//            usedLibVirtualFiles.add(virtualFile);
//        }
//        Set<PsiFile> psiFiles = myDependencies.keySet();
        myLeftTreeExpansionMonitor.freeze();
        myLeftTree.setModel(buildTreeModel(incompatibleFileUses, myLeftTreeMarker));
        myLeftTreeExpansionMonitor.restore();
        expandFirstLevel(myLeftTree);
    }

    private static void expandFirstLevel(Tree tree) {
        PackageDependenciesNode root = (PackageDependenciesNode) tree.getModel().getRoot();
        int count = root.getChildCount();
        if (count < 10) {
            for (int i = 0; i < count; i++) {
                PackageDependenciesNode child = (PackageDependenciesNode) root.getChildAt(i);
                expandNodeIfNotTooWide(tree, child);
            }
        }
    }

    private static void expandNodeIfNotTooWide(Tree tree, PackageDependenciesNode node) {
        int count = node.getChildCount();
        if (count > 5) return;
        //another level of nesting
        if (count == 1 && node.getChildAt(0).getChildCount() > 5) {
            return;
        }
        tree.expandPath(new TreePath(node.getPath()));
    }

    private Set<PsiFile> getSelectedScope(final Tree tree) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return EMPTY_FILE_SET;
        Set<PsiFile> result = new HashSet<>();
        for (TreePath path : paths) {
            PackageDependenciesNode node = (PackageDependenciesNode) path.getLastPathComponent();
            node.fillFiles(result, !mySettings.UI_FLATTEN_PACKAGES);
        }
        return result;
    }

    public void setContent(Content content) {
        myContent = content;
    }


    @Override
    public void dispose() {
        FileTreeModelBuilder.clearCaches(myProject);
    }

    protected class MyTreeCellRenderer extends ColoredTreeCellRenderer {
        private OnlineLibInfo onlineLibInfo;
        public MyTreeCellRenderer(OnlineLibInfo onlineLibInfo){
            this.onlineLibInfo = onlineLibInfo;
        }
        @Override
        public void customizeCellRenderer(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus
        ) {
            PackageDependenciesNode node = (PackageDependenciesNode) value;
            if (node.isValid()) {
                setIcon(node.getIcon());
            } else {
                append(UsageViewBundle.message("node.invalid") + " ", SimpleTextAttributes.ERROR_ATTRIBUTES);
            }
            append(node.toString(), node.hasMarked() && !selected ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
            append(node.getPresentableFilesCount(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    private final class CloseAction extends AnAction implements DumbAware {
        public CloseAction() {
            super(CommonBundle.message("action.close"), AnalysisScopeBundle.message("action.close.dependency.description"),
                    AllIcons.Actions.Cancel);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            Disposer.dispose(myUsagesPanel);
            DependenciesToolWindow.getInstance(myProject).closeContent(myContent);
            mySettings.copyToApplicationDependencySettings();
        }
    }

    private final class FlattenPackagesAction extends ToggleAction {
        FlattenPackagesAction() {
            super(AnalysisScopeBundle.message("action.flatten.packages"),
                    AnalysisScopeBundle.message("action.flatten.packages"),
                    PlatformIcons.FLATTEN_PACKAGES_ICON);
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return mySettings.UI_FLATTEN_PACKAGES;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            DependencyUISettings.getInstance().UI_FLATTEN_PACKAGES = flag;
            mySettings.UI_FLATTEN_PACKAGES = flag;
            rebuild();
        }
    }

    private final class ShowFilesAction extends ToggleAction {
        ShowFilesAction() {
            super(AnalysisScopeBundle.message("action.show.files"), AnalysisScopeBundle.message("action.show.files.description"),
                    AllIcons.FileTypes.Unknown);
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return mySettings.UI_SHOW_FILES;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            DependencyUISettings.getInstance().UI_SHOW_FILES = flag;
            mySettings.UI_SHOW_FILES = flag;
            if (!flag && myLeftTree.getSelectionPath() != null && myLeftTree.getSelectionPath().getLastPathComponent() instanceof FileNode) {
                TreeUtil.selectPath(myLeftTree, myLeftTree.getSelectionPath().getParentPath());
            }
            rebuild();
        }
    }

    private final class ShowModulesAction extends ToggleAction {
        ShowModulesAction() {
            super(AnalysisScopeBundle.message("action.show.modules"), AnalysisScopeBundle.message("action.show.modules.description"),
                    AllIcons.Actions.GroupByModule);
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return mySettings.UI_SHOW_MODULES;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            DependencyUISettings.getInstance().UI_SHOW_MODULES = flag;
            mySettings.UI_SHOW_MODULES = flag;
            rebuild();
        }
    }

    private final class ShowModuleGroupsAction extends ToggleAction {
        ShowModuleGroupsAction() {
            super("Show module groups", "Show module groups", AllIcons.Actions.GroupByModuleGroup);
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return mySettings.UI_SHOW_MODULE_GROUPS;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            DependencyUISettings.getInstance().UI_SHOW_MODULE_GROUPS = flag;
            mySettings.UI_SHOW_MODULE_GROUPS = flag;
            rebuild();
        }

        @Override
        public void update(@NotNull final AnActionEvent e) {
            super.update(e);
            e.getPresentation().setVisible(ModuleManager.getInstance(myProject).hasModuleGroups());
            e.getPresentation().setEnabled(mySettings.UI_SHOW_MODULES);
        }
    }

    private final class GroupByScopeTypeAction extends ToggleAction {
        GroupByScopeTypeAction() {
            super(AnalysisScopeBundle.message("action.group.by.scope.type"), AnalysisScopeBundle.message("action.group.by.scope.type.description"),
                    AllIcons.Actions.GroupByTestProduction);
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return mySettings.UI_GROUP_BY_SCOPE_TYPE;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            DependencyUISettings.getInstance().UI_GROUP_BY_SCOPE_TYPE = flag;
            mySettings.UI_GROUP_BY_SCOPE_TYPE = flag;
            rebuild();
        }

        @Override
        public void update(final AnActionEvent e) {
            super.update(e);
        }
    }


    private final class FilterLegalsAction extends ToggleAction {
        FilterLegalsAction() {
            super(AnalysisScopeBundle.message("action.show.illegals.only"), AnalysisScopeBundle.message("action.show.illegals.only.description"),
                    AllIcons.General.Filter);
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return mySettings.UI_FILTER_LEGALS;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            DependencyUISettings.getInstance().UI_FILTER_LEGALS = flag;
            mySettings.UI_FILTER_LEGALS = flag;
            setEmptyText(flag);
            rebuild();
        }
    }

    private void setEmptyText(boolean flag) {
        final String emptyText = flag ? "No illegal dependencies found" : "Nothing to show";
        myLeftTree.getEmptyText().setText(emptyText);
//        myRightTree.getEmptyText().setText(emptyText);
    }

    private final class EditDependencyRulesAction extends AnAction {
        public EditDependencyRulesAction() {
            super(AnalysisScopeBundle.message("action.edit.rules"), AnalysisScopeBundle.message("action.edit.rules.description"),
                    AllIcons.General.Settings);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            boolean applied = ShowSettingsUtil.getInstance().editConfigurable(LibraryDependenciesPanel.this, new DependencyConfigurable(myProject));
            if (applied) {
                rebuild();
            }
        }
    }


    private class DependenciesExporterToTextFile implements ExporterToTextFile {

        @Override
        public JComponent getSettingsEditor() {
            return null;
        }

        @Override
        public void addSettingsChangedListener(ChangeListener listener) throws TooManyListenersException {
        }

        @Override
        public void removeSettingsChangedListener(ChangeListener listener) {
        }

        @NotNull
        @Override
        public String getReportText() {
            final Element rootElement = new Element("root");
            rootElement.setAttribute("isBackward", String.valueOf(!myForward));
            final List<PsiFile> files = new ArrayList<>(myDependencies.keySet());
            Collections.sort(files, (f1, f2) -> {
                final VirtualFile virtualFile1 = f1.getVirtualFile();
                final VirtualFile virtualFile2 = f2.getVirtualFile();
                if (virtualFile1 != null && virtualFile2 != null) {
                    return virtualFile1.getPath().compareToIgnoreCase(virtualFile2.getPath());
                }
                return 0;
            });
            for (PsiFile file : files) {
                final Element fileElement = new Element("file");
                fileElement.setAttribute("path", file.getVirtualFile().getPath());
                for (PsiFile dep : myDependencies.get(file)) {
                    Element depElement = new Element("dependency");
                    depElement.setAttribute("path", dep.getVirtualFile().getPath());
                    fileElement.addContent(depElement);
                }
                rootElement.addContent(fileElement);
            }
            PathMacroManager.getInstance(myProject).collapsePaths(rootElement);
            return JDOMUtil.writeDocument(new Document(rootElement), SystemProperties.getLineSeparator());
        }

        @NotNull
        @Override
        public String getDefaultFilePath() {
            return "";
        }

        @Override
        public void exportedTo(String filePath) {
        }

        @Override
        public boolean canExport() {
            return true;
        }
    }


    private class RerunAction extends AnAction {
        public RerunAction(JComponent comp) {
            super(CommonBundle.message("action.rerun"), AnalysisScopeBundle.message("action.rerun.dependency"), AllIcons.Actions.Rerun);
            registerCustomShortcutSet(CommonShortcuts.getRerun(), comp);
        }

        @Override
        public void update(AnActionEvent e) {
            boolean enabled = true;
            enabled &= myBuilder.getScope().isValid();
            e.getPresentation().setEnabled(enabled);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            DependenciesToolWindow.getInstance(myProject).closeContent(myContent);
            mySettings.copyToApplicationDependencySettings();
            SwingUtilities.invokeLater(() -> {
                final List<AnalysisScope> scopes = new ArrayList<>();
                final AnalysisScope scope = myBuilder.getScope();
                scope.invalidate();
                scopes.add(scope);

                if (!myForward) {
                    new BackwardDependenciesHandler(myProject, scopes, myScopeOfInterest, new HashSet<>()).analyze();
                } else {
                    new AnalyzeDependenciesHandler(myProject, scopes, myTransitiveBorder, new HashSet<>()).analyze();
                }
            });
        }
    }

    private static class MyTree extends Tree implements DataProvider {
        @Override
        public Object getData(String dataId) {
            PackageDependenciesNode node = getSelectedNode();
            if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
                return node;
            }
            if (CommonDataKeys.PSI_ELEMENT.is(dataId) && node != null) {
                final PsiElement element = node.getPsiElement();
                return element != null && element.isValid() ? element : null;
            }
            return null;
        }

        @Nullable
        public PackageDependenciesNode getSelectedNode() {
            TreePath[] paths = getSelectionPaths();
            if (paths == null || paths.length != 1) return null;
            return (PackageDependenciesNode) paths[0].getLastPathComponent();
        }
    }

    private class RemoveFromScopeAction extends AnAction {
        private RemoveFromScopeAction() {
            super("Remove from scope");
        }

        @Override
        public void update(final AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(!getSelectedScope(myLeftTree).isEmpty());
        }

        @Override
        public void actionPerformed(final AnActionEvent e) {
            final Set<PsiFile> selectedScope = getSelectedScope(myLeftTree);
            exclude(selectedScope);
//            myExcluded.addAll(selectedScope);
            final TreePath[] paths = myLeftTree.getSelectionPaths();
            for (TreePath path : paths) {
                TreeUtil.removeLastPathComponent(myLeftTree, path);
            }
        }
    }

    private class AddToScopeAction extends AnAction {
        private AddToScopeAction() {
            super("Add to scope");
        }

        @Override
        public void update(final AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(getScope() != null);
        }

        @Override
        public void actionPerformed(final AnActionEvent e) {
            final AnalysisScope scope = getScope();
            LOG.assertTrue(scope != null);
            final DependenciesBuilder builder;
            if (!myForward) {
                builder = new BackwardDependenciesBuilder(myProject, scope, myScopeOfInterest);
            } else {
                builder = new ForwardDependenciesBuilder(myProject, scope, myTransitiveBorder);
            }
            ProgressManager.getInstance().runProcessWithProgressAsynchronously(myProject, AnalysisScopeBundle.message("package.dependencies.progress.title"),
                    () -> builder.analyze(), () -> {
                        myDependencies.putAll(builder.getDependencies());
                        putAllDependencies(builder);
                        exclude(new HashSet<>());
                        rebuild();
                    }, null, new PerformAnalysisInBackgroundOption(myProject));
        }

        @Nullable
        private AnalysisScope getScope() {
            final Set<PsiFile> selectedScope = null;
            Set<PsiFile> result = new HashSet<>();
            ((PackageDependenciesNode) myLeftTree.getModel().getRoot()).fillFiles(result, !mySettings.UI_FLATTEN_PACKAGES);
            selectedScope.removeAll(result);
            if (selectedScope.isEmpty()) return null;
            List<VirtualFile> files = new ArrayList<>();
            final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
            for (PsiFile psiFile : selectedScope) {
                final VirtualFile file = psiFile.getVirtualFile();
                LOG.assertTrue(file != null);
                if (fileIndex.isInContent(file)) {
                    files.add(file);
                }
            }
            if (!files.isEmpty()) {
                return new AnalysisScope(myProject, files);
            }
            return null;
        }
    }


    private void selectElementInLeftTree(PsiElement elt) {
        PsiManager manager = PsiManager.getInstance(myProject);

        PackageDependenciesNode root = (PackageDependenciesNode) myLeftTree.getModel().getRoot();
        Enumeration enumeration = root.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            PackageDependenciesNode child = (PackageDependenciesNode) enumeration.nextElement();
            if (manager.areElementsEquivalent(child.getPsiElement(), elt)) {
                myLeftTree.setSelectionPath(new TreePath(((DefaultTreeModel) myLeftTree.getModel()).getPathToRoot(child)));
                break;
            }
        }
    }


    private final class ChooseScopeTypeAction extends ComboBoxAction {
        @Override
        @NotNull
        protected DefaultActionGroup createPopupActionGroup(final JComponent button) {
            final DefaultActionGroup group = new DefaultActionGroup();
            for (final PatternDialectProvider provider : Extensions.getExtensions(PatternDialectProvider.EP_NAME)) {
                group.add(new AnAction(provider.getDisplayName()) {
                    @Override
                    public void actionPerformed(final AnActionEvent e) {
                        mySettings.SCOPE_TYPE = provider.getShortName();
                        DependencyUISettings.getInstance().SCOPE_TYPE = provider.getShortName();
                        rebuild();
                    }
                });
            }
            return group;
        }

        @Override
        public void update(final AnActionEvent e) {
            super.update(e);
            final PatternDialectProvider provider = PatternDialectProvider.getInstance(mySettings.SCOPE_TYPE);
            e.getPresentation().setText(provider.getDisplayName());
            e.getPresentation().setIcon(provider.getIcon());
        }
    }
}

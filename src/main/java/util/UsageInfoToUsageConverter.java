package util;

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector;
import com.intellij.codeInsight.highlighting.ReadWriteUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.ReadWriteAccessUsageInfo2UsageAdapter;
import com.intellij.usages.Usage;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UsageInfoToUsageConverter {
    private UsageInfoToUsageConverter() {
    }

    public static class TargetElementsDescriptor {
        private final List<SmartPsiElementPointer<PsiElement>> myPrimarySearchedElements;
        private final List<SmartPsiElementPointer<PsiElement>> myAdditionalSearchedElements;

        public TargetElementsDescriptor(@NotNull PsiElement element) {
            this(new PsiElement[]{element});
        }

        public TargetElementsDescriptor(@NotNull PsiElement[] primarySearchedElements) {
            this(primarySearchedElements, PsiElement.EMPTY_ARRAY);
        }

        public TargetElementsDescriptor(@NotNull PsiElement[] primarySearchedElements, @NotNull PsiElement[] additionalSearchedElements) {
            myPrimarySearchedElements = convertToSmartPointers(primarySearchedElements);
            myAdditionalSearchedElements = convertToSmartPointers(additionalSearchedElements);
        }

        @NotNull
        private static PsiElement[] convertToPsiElements(@NotNull List<SmartPsiElementPointer<PsiElement>> primary) {
            return ContainerUtil.toArray(ContainerUtil.mapNotNull(primary, SmartPsiElementPointer::getElement), PsiElement.ARRAY_FACTORY);
        }

        @NotNull
        private static List<SmartPsiElementPointer<PsiElement>> convertToSmartPointers(@NotNull PsiElement[] primaryElements) {
            if (primaryElements.length == 0) return Collections.emptyList();

            final SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(primaryElements[0].getProject());
            return ContainerUtil.mapNotNull(primaryElements, smartPointerManager::createSmartPsiElementPointer);
        }

        /**
         * A read-only attribute describing the target as a "primary" target.
         * A primary target is a target that was the main purpose of the search.
         * All usages of a non-primary target should be considered as a special case of usages of the corresponding primary target.
         * Example: searching field and its getter and setter methods -
         *          the field searched is a primary target, and its accessor methods are non-primary targets, because
         *          for this particular search usages of getter/setter methods are to be considered as a usages of the corresponding field.
         */
        @NotNull
        public PsiElement[] getPrimaryElements() {
            return convertToPsiElements(myPrimarySearchedElements);
        }

        @NotNull
        public PsiElement[] getAdditionalElements() {
            return convertToPsiElements(myAdditionalSearchedElements);
        }

        @NotNull
        public List<PsiElement> getAllElements() {
            List<PsiElement> result = new ArrayList<>(myPrimarySearchedElements.size() + myAdditionalSearchedElements.size());
            for (SmartPsiElementPointer pointer : myPrimarySearchedElements) {
                PsiElement element = pointer.getElement();
                if (element != null) {
                    result.add(element);
                }
            }
            for (SmartPsiElementPointer pointer : myAdditionalSearchedElements) {
                PsiElement element = pointer.getElement();
                if (element != null) {
                    result.add(element);
                }
            }
            return result;
        }

        @NotNull
        public List<SmartPsiElementPointer<PsiElement>> getAllElementPointers() {
            return ContainerUtil.concat(myPrimarySearchedElements, myAdditionalSearchedElements);
        }
    }

    @NotNull
    public static Usage convert(@NotNull com.intellij.usages.UsageInfoToUsageConverter.TargetElementsDescriptor descriptor, @NotNull UsageInfo usageInfo) {
        PsiElement[] primaryElements = descriptor.getPrimaryElements();

        return convert(primaryElements, usageInfo);
    }

    @NotNull
    public static Usage convert(@NotNull PsiElement[] primaryElements, @NotNull UsageInfo usageInfo) {
        PsiElement usageElement = usageInfo.getElement();
        if (usageElement != null && primaryElements.length != 0) {
            ReadWriteAccessDetector.Access rwAccess = ReadWriteUtil.getReadWriteAccess(primaryElements, usageElement);
            if (rwAccess != null) {
                return new ReadWriteAccessUsageInfo2UsageAdapter(usageInfo,
                        rwAccess != ReadWriteAccessDetector.Access.Write,
                        rwAccess != ReadWriteAccessDetector.Access.Read);
            }
        }
        return new UsageInfo2UsageAdapter(usageInfo);
    }

    @NotNull
    public static Usage[] convert(@NotNull com.intellij.usages.UsageInfoToUsageConverter.TargetElementsDescriptor descriptor, @NotNull UsageInfo[] usageInfos) {
        Usage[] usages = new Usage[usageInfos.length];
        for (int i = 0; i < usages.length; i++) {
            usages[i] = convert(descriptor, usageInfos[i]);
        }
        return usages;
    }


    @NotNull
    public static Usage[] convert(@NotNull final PsiElement[] primaryElements, @NotNull UsageInfo[] usageInfos) {
        return ContainerUtil.map(usageInfos, info -> convert(primaryElements, info), new Usage[usageInfos.length]);
    }
}

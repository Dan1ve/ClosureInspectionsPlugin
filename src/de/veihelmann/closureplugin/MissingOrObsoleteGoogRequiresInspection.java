package de.veihelmann.closureplugin;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import de.veihelmann.closureplugin.fixes.MissingGoogRequireFix;
import de.veihelmann.closureplugin.fixes.ObsoleteRequireOrProvideFix;
import de.veihelmann.closureplugin.utils.ListMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING;

/**
 * The inspection for missing or superfluous goog.require statements in a file.
 */
public class MissingOrObsoleteGoogRequiresInspection extends LocalInspectionTool {


    @NotNull
    public String getDisplayName() {
        return "Missing or superfluous goog.require statements";
    }

    @NotNull
    public String getGroupDisplayName() {
        return GroupNames.IMPORTS_GROUP_NAME;
    }

    @NotNull
    public String getShortName() {
        return "MissingOrObsoleteGoogRequiresInspection";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new ValidateRequirementsPsiRecursiveElementVisitor(holder);
    }

    public boolean isEnabledByDefault() {
        return true;
    }

    private class ValidateRequirementsPsiRecursiveElementVisitor extends PsiElementVisitor {

        private final ProblemsHolder problemsHolder;

        ValidateRequirementsPsiRecursiveElementVisitor(ProblemsHolder holder) {
            this.problemsHolder = holder;
        }

        @Override
        public void visitFile(PsiFile file) {
            super.visitFile(file);

            ClosureDependenciesExtractor extractor = new ClosureDependenciesExtractor();
            extractor.extractDependencies(file);

            markDuplicationProblem(extractor.getDuplicateGoogRequires(), "Duplicate goog.require");
            markDuplicationProblem(extractor.getDuplicateGoogProvides(), "Duplicate goog.provide");

            markMissingRequires(extractor);
            markObsoleteRequires(extractor);
        }

        private void markObsoleteRequires(ClosureDependenciesExtractor extractor) {
            for (Map.Entry<String, PsiElement> declaredDependency : extractor.googRequires.entrySet()) {
                String namespace = declaredDependency.getKey();
                if (extractor.dependencies.containsKey(namespace)) {
                    continue;
                }
                if (extractor.rawTypesInComments.stream().anyMatch(commentString -> commentString.contains(namespace))) {
                    // One of the type parameters in the comments contains this namespace. This is thus an optional
                    // dependency and will not be reported as 'obsolete'.
                    continue;
                }
                PsiElement requireElement = declaredDependency.getValue();
                ObsoleteRequireOrProvideFix fix = new ObsoleteRequireOrProvideFix(declaredDependency.getValue(), declaredDependency.getKey(), true);
                problemsHolder.registerProblem(requireElement, "Obsolete require: " + declaredDependency.getKey(), fix);
            }
        }

        private void markMissingRequires(ClosureDependenciesExtractor extractor) {
            extractor.dependencies.keys().forEach(namespace -> {
                List<PsiElement> dependencyLocations = extractor.dependencies.getNullSafe(namespace);
                if (!isMissingRequire(extractor, namespace)) {
                    return;
                }
                for (PsiElement location : dependencyLocations) {
                    MissingGoogRequireFix fix = new MissingGoogRequireFix(location, extractor.googRequires, namespace, extractor.googProvides);
                    problemsHolder.registerProblem(location, "No goog.require for '" + namespace + "'", GENERIC_ERROR_OR_WARNING, fix);
                }
            });
        }

        private boolean isMissingRequire(ClosureDependenciesExtractor extractor, String namespace) {
            return !extractor.googProvides.containsKey(namespace) && !extractor.googRequires.containsKey(namespace)
                    && !namespaceIsPrefixOfProvidedNamespace(extractor.googProvides.keySet(), namespace);
        }

        private boolean namespaceIsPrefixOfProvidedNamespace(Set<String> providedNamespaces, String namespace) {
            return providedNamespaces.stream().anyMatch(providedNamespace -> providedNamespace.startsWith(namespace));
        }


        private void markDuplicationProblem(ListMap<String, PsiElement> duplicateElements, String message) {
            duplicateElements.keys().forEach(namespace -> {
                duplicateElements.getNullSafe(namespace).forEach(element -> problemsHolder.registerProblem(element, message,
                        new ObsoleteRequireOrProvideFix(element, namespace, false)));
            });
        }
    }
}


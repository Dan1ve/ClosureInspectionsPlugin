package de.veihelmann.closureplugin;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import de.veihelmann.closureplugin.fixes.ConvertToGoogModuleFix;
import org.jetbrains.annotations.NotNull;

public class ConvertToGoogModuleInspection extends LocalInspectionTool {

    @NotNull
    public String getDisplayName() {
        return "Use goog.module instead of goog.provide";
    }

    @NotNull
    public String getGroupDisplayName() {
        return GroupNames.MODULARIZATION_GROUP_NAME;
    }

    @NotNull
    public String getShortName() {
        return "GoogModuleInsteadOfGoogProvide";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder problemsHolder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                super.visitElement(element);

                if (!(element instanceof JSCallExpression)) {
                    return;
                }
                JSCallExpression statement = (JSCallExpression) element;
                if (statement.getText() == null || !statement.getText().startsWith("goog.provide(")) {
                    return;
                }
                problemsHolder.registerProblem(statement, "Convert to goog.module", ProblemHighlightType.WARNING, new ConvertToGoogModuleFix(statement));
            }
        };
    }

    public boolean isEnabledByDefault() {
        return true;
    }
}

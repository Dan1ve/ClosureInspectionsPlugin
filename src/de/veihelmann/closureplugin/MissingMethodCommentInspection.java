package de.veihelmann.closureplugin;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ecmascript6.psi.ES6ClassExpression;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import de.veihelmann.closureplugin.fixes.MissingMethodCommentFix;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static de.veihelmann.closureplugin.fixes.MissingMethodCommentFix.getMethodParameters;

public class MissingMethodCommentInspection extends LocalInspectionTool {

    @NotNull
    public String getDisplayName() {
        return "Missing JSDoc for method";
    }

    @NotNull
    public String getGroupDisplayName() {
        return GroupNames.COMPILER_ISSUES;
    }

    @NotNull
    public String getShortName() {
        return "MissingMethodCommentInspection";
    }


    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {

            @Override
            public void visitElement(PsiElement element) {
                super.visitElement(element);

                if (!(element instanceof ES6ClassExpression)) {
                    return;
                }

                ES6ClassExpression classElement = (ES6ClassExpression) element;
                for (int i = 0; i < classElement.getChildren().length; i++) {
                    PsiElement child = classElement.getChildren()[i];
                    if (!(child instanceof JSFunction)) {
                        continue;
                    }

                    JSFunction functionElement = (JSFunction) child;
                    boolean isConstructor = Objects.equals(functionElement.getName(), "constructor");

                    // Report undocumented method if
                    // (a) it is a constructor, but has parameters
                    // (b) it is a non-constructor method (no additional criteria)
                    if ((!isConstructor || !getMethodParameters(functionElement).isEmpty()) && methodHasNoComment(functionElement)) {
                        holder.registerProblem(functionElement, "Method has no JSDoc comment", new MissingMethodCommentFix(functionElement));
                    }
                }
            }

            private boolean methodHasNoComment(PsiElement methodELement) {
                return !(methodELement.getFirstChild() instanceof PsiComment);
            }
        };

    }

    public boolean isEnabledByDefault() {
        return true;
    }
}

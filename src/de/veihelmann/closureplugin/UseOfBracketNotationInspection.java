package de.veihelmann.closureplugin;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.javascript.psi.JSIndexedPropertyAccessExpression;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import de.veihelmann.closureplugin.fixes.BracketNotationFix;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Checks for usages of bracket notation (e.g. myVar['fieldName']), which cannot be type-checked by the Closure compiler.
 * Quick fix is to use dot notation (myVar.fieldName) instead.
 */
public class UseOfBracketNotationInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public String getShortName() {
        return "UseOfBracketNotationInspection";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                super.visitElement(element);

                if (element instanceof JSIndexedPropertyAccessExpression) {
                    checkBracketNotation((JSIndexedPropertyAccessExpression) element, holder);
                }
            }
        };
    }

    @NotNull
    public String getDisplayName() {
        return "Checks for uses of bracket notation";
    }

    @NotNull
    public String getGroupDisplayName() {
        return GroupNames.PROPERTIES_GROUP_NAME;
    }

    public boolean isEnabledByDefault() {
        return true;
    }

    private void checkBracketNotation(JSIndexedPropertyAccessExpression accessElement, ProblemsHolder problemsHolder) {
        JSLiteralExpression literal = (JSLiteralExpression) Arrays.stream(accessElement.getChildren()).filter(element -> element instanceof JSLiteralExpression).findFirst().orElse(null);
        if (literal != null && literal.getText().matches("[\"']\\w+['\"]")) {
            problemsHolder.registerProblem(accessElement, "Access of property " + literal.getText() + " cannot be type-checked (bracket notation)", new BracketNotationFix(literal));
        }
    }
}
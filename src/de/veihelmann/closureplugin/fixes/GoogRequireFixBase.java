package de.veihelmann.closureplugin.fixes;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for fixes related to goog.requires.
 */
abstract class GoogRequireFixBase extends LocalQuickFixOnPsiElement {

    protected GoogRequireFixBase(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    public String getFamilyName() {
        return "Missing or obsolete goog.require";
    }
}

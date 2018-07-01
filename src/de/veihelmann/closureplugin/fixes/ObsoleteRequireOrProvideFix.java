package de.veihelmann.closureplugin.fixes;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

/**
 * Fixes a superfluous goog.require by removing the corresponding PSI element.
 */
public class ObsoleteRequireOrProvideFix extends GoogRequireFixBase {

    public ObsoleteRequireOrProvideFix(@NotNull PsiElement requireOrProvideElement) {
        super(requireOrProvideElement);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        if (psiElement.getNextSibling() != null && psiElement.getNextSibling() instanceof PsiWhiteSpace) {
            // Remove whitespace after goog.require to avoid empty lines after removal
            psiElement.getNextSibling().delete();
        }
        psiElement.delete();
    }

    @NotNull
    @Override
    public String getText() {
        return "Remove statement";
    }
}

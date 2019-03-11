package de.veihelmann.closureplugin.fixes;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Fixes a superfluous goog.require by removing the corresponding PSI element.
 */
public class ObsoleteRequireOrProvideFix extends GoogRequireFixBase {

    private final String obsoleteNamespace;

    private final boolean removeAllRequiresForNamespace;

    public ObsoleteRequireOrProvideFix(PsiElement googRequireElement, String obsoleteNamespace, boolean removeAllRequiresForNamespace) {
        super(googRequireElement);
        this.obsoleteNamespace = obsoleteNamespace;
        this.removeAllRequiresForNamespace = removeAllRequiresForNamespace;
    }

    @NotNull
    @Override
    public String getText() {
        return "Remove goog.require for '" + obsoleteNamespace + "'";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {

        psiElement.delete();

        if (!removeAllRequiresForNamespace) {
            return;
        }
        PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
        if (manager == null) {
            return;
        }
        Document document = manager.getDocument(psiFile);
        if (document != null) {
            document.setText(document.getText()
                    .replaceAll("goog\\s*\\.\\s*require\\([\"']" + obsoleteNamespace + "[\"']\\);?\\R?", ""));
        }
    }
}

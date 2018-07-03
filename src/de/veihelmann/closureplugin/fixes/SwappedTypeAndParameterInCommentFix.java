package de.veihelmann.closureplugin.fixes;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTag;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Provides a fix for swapped type and parameter names after @param tags. Only works if {@link #canBeQuickFixed(JSDocTag)} applies.
 */
public class SwappedTypeAndParameterInCommentFix extends LocalQuickFixOnPsiElement {

    public SwappedTypeAndParameterInCommentFix(JSDocTag tag) {
        super(tag);
    }

    public static boolean canBeQuickFixed(JSDocTag tag) {
        return tag.getChildren().length > 4 && tag.getChildren()[4].getText().startsWith("{");
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Type safety";
    }

    @NotNull
    @Override
    public String getText() {
        return "Swap type and parameter";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null || !canBeQuickFixed((JSDocTag) psiElement)) {
            return;
        }
        String documentText = document.getText();
        String newDocumentText = documentText.substring(0, psiElement.getChildren()[2].getTextRange().getStartOffset())
                + psiElement.getChildren()[4].getText()
                + psiElement.getChildren()[3].getText()
                + psiElement.getChildren()[2].getText()
                + documentText.substring(psiElement.getChildren()[4].getTextRange().getEndOffset());
        document.setText(newDocumentText);
    }
}

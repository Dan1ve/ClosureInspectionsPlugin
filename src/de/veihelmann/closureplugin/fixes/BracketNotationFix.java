package de.veihelmann.closureplugin.fixes;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Fixes usages of bracket notation (e.g. myVar['field']) by inserting dot notation instead (myVar.field).
 */
public class BracketNotationFix extends LocalQuickFixOnPsiElement {

    public BracketNotationFix(@NotNull PsiElement bracketNotationElement) {
        super(bracketNotationElement);

        if (!(bracketNotationElement instanceof JSLiteralExpression)) {
            throw new AssertionError("Expected bracket notation element (JSLiteralExpression) as input");
        }
    }

    @NotNull
    @Override
    public String getText() {
        return "Change to " + getDotAccessForElement();
    }

    private String getDotAccessForElement() {
        return "." + getStartElement().getText().substring(1, getStartElement().getText().length() - 1);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement bracketNotationElement, @NotNull PsiElement psiElement1) {

        Document document = PsiDocumentManager.getInstance(project).getDocument(bracketNotationElement.getContainingFile());
        if (document == null) {
            return;
        }
        String documentText = document.getText();
        String newDocumentText = documentText.substring(0, bracketNotationElement.getTextRange().getStartOffset() - 1)
                + getDotAccessForElement()
                + documentText.substring(bracketNotationElement.getTextRange().getEndOffset() + 1);
        document.setText(newDocumentText);
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Compile safety inspections";
    }
}
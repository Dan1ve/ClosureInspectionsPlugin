package de.veihelmann.closureplugin.fixes;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick fix for converting a goog.provide to a goog.module.
 */
public class ConvertToGoogModuleFix extends LocalQuickFixOnPsiElement {

    public ConvertToGoogModuleFix(JSCallExpression provideStatement) {
        super(provideStatement);
    }

    @NotNull
    @Override
    public String getText() {
        return "Convert class to goog.module [beta]";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement targetElement, @NotNull PsiElement psiElement1) {

        JSCallExpression statement = (JSCallExpression) targetElement;
        // TODO (DV) Handle multiple goog.provides in same file
        String providedNamespace = statement.getText().substring(statement.getText().indexOf("'")).replaceAll("[\"';)]", "");
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return;
        }
        String documentText = document.getText();
        List<String> oldStyleRequiredNamespaces = extractSortedOldRequiredNamespaces(documentText);

        documentText = documentText.replace("goog.provide('" + providedNamespace + "')", "goog.module('" + providedNamespace + "');\ngoog.module.declareLegacyNamespace()");

        String newClassName = providedNamespace.substring(providedNamespace.lastIndexOf(".") + 1);

        documentText = documentText.replaceAll(Pattern.quote(providedNamespace + "."), newClassName + ".");
        documentText = documentText.replaceAll(Pattern.quote(providedNamespace + " "), newClassName + " ");
        documentText = documentText.replaceAll(Pattern.quote(providedNamespace + ")"), newClassName + ")");

        documentText = documentText.replace("class " + providedNamespace, "class " + newClassName);
        documentText = documentText.replace(newClassName + " = class ", "class " + newClassName + " ");

        documentText += "\nexports = " + newClassName + ";";

        for (String oldRequire : oldStyleRequiredNamespaces) {
            documentText = FixUtils.replaceExistingGoogRequiresWithSafeReferences(documentText, oldRequire);
        }

        document.setText(documentText);

        CodeStyleManager.getInstance(project).reformat(psiFile);
    }

    private List<String> extractSortedOldRequiredNamespaces(String documentText) {
        Set<String> result = new HashSet<>();
        Matcher matcher = Pattern.compile("goog\\.require\\(['\"](\\w+\\.(\\w+|\\.)+)").matcher(documentText);
        while (matcher.find()) {
            String requiredNamespace = matcher.group(1);
            result.add(requiredNamespace);
        }

        ArrayList<String> asList = new ArrayList<>(result);
        // We sort by length of the required namespace to avoid replacing prefixed namespaces accidentially,
        // i.e. 'goog.dom.classlist' should be handled before 'goog.dom'
        asList.sort(Comparator.comparingInt(String::length).reversed());
        return asList;
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Modules Usage";
    }
}

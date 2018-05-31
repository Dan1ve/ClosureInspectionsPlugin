package de.veihelmann.closureplugin.fixes;

import com.intellij.lang.javascript.psi.JSStatement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.util.Comparator.comparingInt;

/**
 * Fixes a missing goog.require for a namespace by inserting a corresponding statement (either within the existing goog.requires or after the goog.provide(s) in a file).
 */
public class MissingGoogRequireFix extends GoogRequireFixBase {

    private final String missingNamespace;

    private final SortedMap<String, PsiElement> currentRequires;

    private final SortedMap<String, JSStatement> googProvides;

    public MissingGoogRequireFix(@NotNull PsiElement element, SortedMap<String, PsiElement> currentRequires, String missingNamespace, SortedMap<String, JSStatement> googProvides) {
        super(element);
        this.missingNamespace = missingNamespace;
        this.currentRequires = currentRequires;
        this.googProvides = googProvides;
    }


    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {

        if (currentRequires.keySet().contains(missingNamespace)) {
            // Already fixed. This is unexpected here, but it doesn't hurt to check.
            return;
        }

        SortedSet<String> allSortedNamespaces = new TreeSet<>(Comparator.naturalOrder());
        allSortedNamespaces.addAll(currentRequires.keySet());
        allSortedNamespaces.add(missingNamespace);

        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

        if (document == null) {
            Logger.getInstance(getClass()).error("Unexpected error: Document is null");
            return;
        }

        // Remove existing goog.requires
        document.setText(document.getText().replaceAll("goog\\s*\\.\\s*require\\s*\\([^)]+\\);?[\\n\\r]+", ""));

        // Insert new goog. require (a) after goog.provide, or (b) at the top of the file (fallback).
        Optional<JSStatement> elementToInsertRequireAfter =
                googProvides.values().stream().max(comparingInt(PsiElement::getTextOffset));

        if (elementToInsertRequireAfter.isPresent()) {
            document.insertString(elementToInsertRequireAfter.get().getNextSibling().getTextOffset(), "\n" + buildRequireStatements(allSortedNamespaces));
        } else {
            document.insertString(0, "\n" + buildRequireStatements(allSortedNamespaces));
        }

        currentRequires.put(this.missingNamespace, null);
    }

    private String buildRequireStatements(SortedSet<String> allSortedNamespaces) {
        StringBuilder builder = new StringBuilder();
        String lastPrefix = null;
        for (String namespace : allSortedNamespaces) {
            if (namespace.contains(".") && !namespace.substring(0, namespace.indexOf(".")).equals(lastPrefix)) {
                // add empty line between namespaces of different origins.
                builder.append("\n");
            }
            if (namespace.contains(".")) {
                lastPrefix = namespace.substring(0, namespace.indexOf("."));
            }
            builder.append("goog.require('")
                    .append(namespace)
                    .append("');\n");
        }
        return builder.toString();
    }

    @NotNull
    @Override
    public String getText() {
        return "Add goog.require('" + missingNamespace + "')";
    }
}

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

    private final SortedMap<String, JSStatement> googModules;
    private final Map<String, String> fullNamespacesToShortReferences;

    public MissingGoogRequireFix(@NotNull PsiElement element, SortedMap<String, PsiElement> currentRequires, String missingNamespace, SortedMap<String, JSStatement> googProvides, SortedMap<String, JSStatement> googModules, Map<String, String> fullNamespacesToShortReferences) {
        super(element);
        this.missingNamespace = missingNamespace;
        this.currentRequires = currentRequires;
        this.googProvides = googProvides;
        this.googModules = googModules;
        this.fullNamespacesToShortReferences = fullNamespacesToShortReferences;
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
        document.setText(document.getText().replaceAll("((const|let|var)\\s+[\\w_\\d]+\\s+=\\s+)?goog\\s*\\.\\s*require\\s*\\([^)]+\\);?[\\n\\r]+", ""));

        SortedMap<String, JSStatement> relevantImports = this.googProvides;
        if (!googModules.isEmpty()) {
            relevantImports = this.googModules;
        }

        // Insert new goog.require (a) after goog.provide/goog.module, or (b) at the top of the file (fallback).
        Optional<JSStatement> elementToInsertRequireAfter =
                relevantImports.values().stream().max(comparingInt(PsiElement::getTextOffset));

        if (elementToInsertRequireAfter.isPresent()) {
            int insertionOffset = elementToInsertRequireAfter.get().getNextSibling().getTextOffset();
            if (!this.googModules.isEmpty()) {
                String legacyDeclaration = "goog.module.declareLegacyNamespace();";
                int indexOfLegacyDeclaration = document.getText().indexOf(legacyDeclaration);
                if (indexOfLegacyDeclaration > -1) {
                    insertionOffset = Math.max(insertionOffset, indexOfLegacyDeclaration + legacyDeclaration.length());
                }
            }
            insertRequires(document, insertionOffset, allSortedNamespaces, !this.googModules.isEmpty());
        } else {
            insertRequires(document, 0, allSortedNamespaces, !this.googModules.isEmpty());
        }

        currentRequires.put(this.missingNamespace, null);
    }

    private void insertRequires(Document document, int insertionOffset, SortedSet<String> allSortedNamespaces, boolean useVariableForGoogRequires) {
        StringBuilder builder = new StringBuilder("\n");
        String lastPrefix = null;

        List<String> namespacesToReferenceAfterwards = new ArrayList<>();

        for (String namespace : allSortedNamespaces) {
            if (namespace.contains(".") && !namespace.substring(0, namespace.indexOf(".")).equals(lastPrefix)) {
                // add empty line between namespaces of different origins.
                builder.append("\n");
            }
            if (namespace.contains(".")) {
                lastPrefix = namespace.substring(0, namespace.indexOf("."));
            }

            if (useVariableForGoogRequires) {
                String importReference = fullNamespacesToShortReferences.get(namespace);
                if (importReference == null) {
                    namespacesToReferenceAfterwards.add(namespace);
                } else {
                    builder.append("const ").append(importReference).append(" = ");
                }
            }

            builder.append("goog.require('")
                    .append(namespace)
                    .append("');\n");
        }

        document.insertString(insertionOffset, builder.toString());
        namespacesToReferenceAfterwards.forEach(namespace -> {
            document.setText(FixUtils.replaceExistingGoogRequiresWithSafeReferences(document.getText(), namespace));
        });
    }

    @NotNull
    @Override
    public String getText() {
        return "Add goog.require('" + missingNamespace + "')";
    }
}

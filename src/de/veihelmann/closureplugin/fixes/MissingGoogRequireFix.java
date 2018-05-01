package de.veihelmann.closureplugin.fixes;

import com.intellij.lang.javascript.psi.JSStatement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.SortedMap;

import static de.veihelmann.closureplugin.utils.LanguageUtils.JAVASCRIPT;
import static java.util.Comparator.comparingInt;

/**
 * Fixes a missing goog.require for a namespace by inserting a corresponding statement (either within the existing goog.requires or after the goog.provide(s) in a file).
 */
public class MissingGoogRequireFix extends GoogRequireFixBase {

    private final String missingNamespace;

    private final SortedMap<String, JSStatement> currentRequires;

    private final SortedMap<String, JSStatement> googProvides;

    public MissingGoogRequireFix(@NotNull PsiElement element, SortedMap<String, JSStatement> currentRequires, String missingNamespace, SortedMap<String, JSStatement> googProvides) {
        super(element);
        this.missingNamespace = missingNamespace;
        this.currentRequires = currentRequires;
        this.googProvides = googProvides;
    }


    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {

        PsiFileFactory factory = PsiFileFactory.getInstance(project);

        if (currentRequires.keySet().contains(missingNamespace)) {
            // Already fixed. This is unexpected here, but it doesn't hurt to check.
            return;
        }

        // Insert new goog. require (a) after existing goog.require, or (b) after goog.provide, or (c) at the top of the file (fallback).
        Optional<JSStatement> elementToInsertRequireAfter = currentRequires.values().stream().findFirst();
        if (!elementToInsertRequireAfter.isPresent()) {
            elementToInsertRequireAfter = googProvides.values().stream().max(comparingInt(PsiElement::getTextOffset));
        }
        if (!elementToInsertRequireAfter.isPresent()) {
            elementToInsertRequireAfter = Optional.of((JSStatement) psiFile.getFirstChild());
        }

        String statement = "goog.require('" + missingNamespace + "');";

        // Create a new temp file to let IntelliJ parse the PSI element(s) for our input. Apparently, this is the supposed way to do this.
        final PsiElement fileElement = factory.createFileFromText("temp.js", JAVASCRIPT, statement);
        PsiElement newElement = fileElement.getFirstChild();
        newElement = elementToInsertRequireAfter.get().getParent().addAfter(newElement, elementToInsertRequireAfter.get());

        currentRequires.put(missingNamespace, (JSStatement) newElement);
    }

    @NotNull
    @Override
    public String getText() {
        return "Add goog.require('" + missingNamespace + "')";
    }


}

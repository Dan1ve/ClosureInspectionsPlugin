package de.veihelmann.closureplugin.fixes;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.JSParameter;
import com.intellij.lang.javascript.psi.JSParameterList;
import com.intellij.lang.javascript.psi.JSType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/**
 * Quick fix for a missing method comment.
 */
public class MissingMethodCommentFix extends LocalQuickFixOnPsiElement {

    public MissingMethodCommentFix(JSFunction functionELement) {
        super(functionELement);
    }

    @NotNull
    @Override
    public String getText() {
        return "Insert JSDoc comment";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement targetElement, @NotNull PsiElement psiElement1) {
        JSFunction functionElement = (JSFunction) targetElement;
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return;
        }
        String documentText = document.getText();
        String indentation = "";
        if (functionElement.getPrevSibling() instanceof PsiWhiteSpace) {
            indentation = functionElement.getPrevSibling().getText();
            if (indentation.contains("\n")) {
                // Only consider indentation of same line as the method declaration
                indentation = indentation.substring(indentation.lastIndexOf("\n") + 1);
            }
        }

        List<String> parameters = getMethodParameters(functionElement);
        String newDocumentText = documentText.substring(0, functionElement.getTextRange().getStartOffset())
                + buildJsDoc(parameters, indentation, functionElement.getReturnType())
                + indentation + documentText.substring(functionElement.getTextRange().getStartOffset());
        document.setText(newDocumentText);
    }

    @NotNull
    private String buildJsDoc(List<String> parameters, String indentation, JSType returnType) {
        StringBuilder builder = new StringBuilder("/** \n").append(indentation).append(" * TODO").append("\n");
        if (!parameters.isEmpty()) {
            builder.append(indentation).append(" *").append("\n");
        }
        for (String parameter : parameters) {
            builder.append(indentation).append(" * @param {TODO");
            if (parameter.startsWith("opt_")) {
                builder.append("=");
            }
            builder.append("} ").append(parameter).append("\n");
        }
        if (returnType != null && !returnType.getResolvedTypeText().equals("void")) {
            String returnTypeString = returnType.getResolvedTypeText();
            if (!(returnTypeString.contains("null") || returnTypeString.contains("undefined"))) {
                returnTypeString = "!" + returnTypeString;
            }

            returnTypeString = returnTypeString.replaceAll("null\\|", "?");
            returnTypeString = replaceClosureTypes(returnTypeString);
            builder.append(indentation).append(" * @return {").append(returnTypeString).append("}\n");
        }
        return builder.append(indentation).append(" */\n").toString();
    }

    /**
     * Turns IntelliJ's inferred JS types into Closure compatible ones by lowercasing them (e.g. Boolean -> boolean)
     */
    private String replaceClosureTypes(String returnTypeString) {
        for (String type : Arrays.asList("Number", "String", "Boolean")) {
            Matcher matcher = Pattern.compile("([^\\w]?)" + type + "([^\\w]?)").matcher(returnTypeString);
            if (matcher.find()) {
                returnTypeString = matcher.replaceAll(matcher.group(1) + type.toLowerCase() + matcher.group(2));
            }
        }
        return returnTypeString;
    }

    public static List<String> getMethodParameters(PsiElement functionElement) {
        Optional<PsiElement> parameters = Arrays.stream(functionElement.getChildren()).filter(child -> child instanceof JSParameterList).findFirst();
        if (parameters.isPresent()) {
            return Arrays.stream(parameters.get().getChildren()).filter(parameterOrWhitespace -> parameterOrWhitespace instanceof JSParameter)
                    .map(PsiElement::getText).collect(toList());
        }
        return Collections.emptyList();
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Type safety";
    }
}

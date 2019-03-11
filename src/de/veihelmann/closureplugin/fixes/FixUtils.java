package de.veihelmann.closureplugin.fixes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixUtils {

    private static final HashSet<String> RESERVED_KEYWORDS = new HashSet<>(Arrays.asList("document", "Array", "localStorage", "Map", "Set", "string", "number", "Object"));

    public static String replaceExistingGoogRequiresWithSafeReferences(String documentText, String requiredNamespace) {
        String newShortName = findSafeReferenceForGoogRequire(documentText, requiredNamespace);

        // TODO (DV) Do something more sophisticated here, this is slow and error-prone
        documentText = documentText.replace("goog.require('" + requiredNamespace + "')",
                "const " + newShortName + " = goog.require('" + requiredNamespace + "')");
        documentText = documentText.replace("goog.require(\"" + requiredNamespace + "\")",
                "const " + newShortName + " = goog.require(\"" + requiredNamespace + "\")");

        Matcher matcher = Pattern.compile("([^\\w'][!?<]?)" + Pattern.quote(requiredNamespace) + "([^\\w'])").matcher(documentText);
        if (matcher.find()) {
            documentText = matcher.replaceAll("$1" + newShortName + "$2");
        }
        return documentText;
    }

    public static String findSafeReferenceForGoogRequire(String documentText, String requiredNamespace) {
        String[] parts = requiredNamespace.split("\\.");
        String newShortName = parts[parts.length - 1];
        int index = parts.length - 1;
        while (RESERVED_KEYWORDS.contains(newShortName) || Pattern.compile("[^.\\w]" + newShortName + "\\.").matcher(documentText).find()) {
            index--;
            if (index >= 0) {
                newShortName = parts[index] + "_" + newShortName;
            } else if (!newShortName.endsWith("s")) {
                newShortName += "s";
            } else {
                newShortName = "_" + newShortName;
            }
        }

        return newShortName;
    }
}

package de.veihelmann.closureplugin.fixes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixUtils {

    private static final HashSet<String> RESERVED_KEYWORDS = new HashSet<>(Arrays.asList("document", "Array", "localStorage", "Map", "Set", "string", "number", "Object"));

    private static final Map<String, String> DEFAULT_REPLACEMENTS = new HashMap<>();

    static  {
        DEFAULT_REPLACEMENTS.put("string", "strings");
        DEFAULT_REPLACEMENTS.put("number", "numbers");
    }


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
        String[] namespaceParts = requiredNamespace.split("\\.");
        String newShortName = namespaceParts[namespaceParts.length - 1];

        boolean needsToBeUppercase = Character.isUpperCase(newShortName.charAt(0));

        int namespacePartIndex = namespaceParts.length - 1;

        while (RESERVED_KEYWORDS.contains(newShortName)) {

            if (DEFAULT_REPLACEMENTS.containsKey(newShortName)) {
                newShortName = DEFAULT_REPLACEMENTS.get(newShortName);
                continue;
            }

            namespacePartIndex--;
            if (namespacePartIndex >= 0) {
                newShortName = namespaceParts[namespacePartIndex] + newShortName;
            } else {
                newShortName = "_" + newShortName;
            }
        }


        while (Pattern.compile("[^.\\w]" + newShortName + "\\.").matcher(documentText).find()) {
            namespacePartIndex--;
            if (namespacePartIndex >= 0) {
                newShortName = namespaceParts[namespacePartIndex] + "_" + newShortName;
            } else if (!newShortName.endsWith("s")) {
                newShortName += "s";
            } else {
                newShortName = "_" + newShortName;
            }
        }

        if (needsToBeUppercase) {
            newShortName = newShortName.substring(0, 1).toUpperCase() + newShortName.substring(1);
        }
        return newShortName;
    }
}

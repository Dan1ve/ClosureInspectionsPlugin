package de.veihelmann.closureplugin.fixes;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class FixUtilsTest {

    private String testDocument;

    @Before
    public void setup() throws IOException {
        String testFilePath = "test-resources/" + getClass().getName() + "/js-document.js";
        testDocument = String.join("\n", Files.readAllLines(Paths.get(testFilePath)));
    }

    @Test
    public void findSafeReferenceForGoogRequire()  {
        assertNamespaceSuggestion("goog.string", "strings");
        assertNamespaceSuggestion("goog.structs.Map", "StructsMap");
        assertNamespaceSuggestion("goog.array", "array");
        assertNamespaceSuggestion("goog.events.EventType", "EventType");
        assertNamespaceSuggestion("goog.dragger.EventType", "EventType");
    }

    private void assertNamespaceSuggestion(String inputNamespace, String expectedShortNamespace) {
        assertEquals(expectedShortNamespace, FixUtils.findSafeReferenceForGoogRequire(testDocument, inputNamespace));
    }
}
package org.openthinclient.api.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.api.util.Ldif2JsonModelParser;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for Ldif2JsonModelParser
 */
@RunWith(SpringRunner.class)
public class LDIFParserTest {

    @Test
    public void testLdif2JsonModelParser() throws Exception {

        File file = new File(LDIFParserTest.class.getResource("/pales-old.ldif").toURI());
        String envDN = SchemaProfileTest.envDN;
        Pair<String, String> replacement = Pair.of("#%BASEDN%#", envDN);
        Ldif2JsonModelParser f2jmp = new Ldif2JsonModelParser(file, envDN, replacement);
        List<AbstractProfileObject> result = f2jmp.parse();
        assertNotNull(result);
        assertTrue(result.size() > 1); // Hm...

        // show result (and write to files, include into directory.xml ...)
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.INDENT_OUTPUT);
        for (AbstractProfileObject apo : result) {
            System.out.println(om.writeValueAsString(apo));
            System.out.println("---------------");
        };
    }

}

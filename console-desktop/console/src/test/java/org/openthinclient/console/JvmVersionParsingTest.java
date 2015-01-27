package org.openthinclient.console;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openthinclient.console.JreFix.JvmVersion;

public class JvmVersionParsingTest {

	@Test
	public void testParseVersionsSuccess() throws Exception {

		assertCorrectParsing(7, 0, 25, "1.7.0_25");
		assertCorrectParsing(6, 3, 52, "1.6.3_52");
		assertCorrectParsing(6, 3, 52, "1.6.3_52-b20");
		
		assertNull(JvmVersion.parse("12-89"));
		assertNull(JvmVersion.parse("1.6.9-b80"));
	}

	private void assertCorrectParsing(int major, int minor, int patch,
			String version) {
		JvmVersion parsed = JvmVersion.parse(version);
		assertNotNull(parsed);
		assertEquals(major, parsed.getMajor());
		assertEquals(minor, parsed.getMinor());
		assertEquals(patch, parsed.getPatch());
	}
	
}

package org.openthinclient.pkgmgr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PackageManagerTaskSummary implements Serializable {

	private static final long serialVersionUID = 1L;
	private List<String> warnings = new ArrayList<String>();


	public List<String> getWarnings() {
		return warnings;
	}

	public void addWarning(String warning) {
		warnings.add(warning);
	}
}

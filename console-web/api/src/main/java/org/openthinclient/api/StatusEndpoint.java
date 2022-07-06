package org.openthinclient.api;

import org.openthinclient.service.update.UpdateRunnerEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusEndpoint {

	private String status = "UP";

	@GetMapping("/api/v2/server-status")
	public String status() {
		return status;
	}

	@GetMapping("/api/v2/startup-progress")
	public String progress() {
		return "1";
	}

	@EventListener
	private void updateRunnerFinished(UpdateRunnerEvent event) {
		if (event.getExitValue() == 0) {
			status = "UPDATING";
		}
	}
}

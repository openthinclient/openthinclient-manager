package org.openthinclient.api.logs;

import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Syslog Rest Endpoint
 */
@Controller
@RequestMapping("/download")
public class LogDownloadService {

    @Autowired
    private ManagerHome managerHome;

    @RequestMapping(path = "/{file_name}")
    public void download(@PathVariable("file_name") String fileName, HttpServletRequest request, HttpServletResponse response) throws IOException {

        Path path = Paths.get(managerHome.getLocation().getAbsolutePath(), "logs/" + fileName + ".log");

        response.setContentType("application/text");
        response.addHeader("Content-Disposition", "attachment; filename=" + path.getFileName());

        Files.copy(path, response.getOutputStream());
        response.getOutputStream().flush();
    }
}
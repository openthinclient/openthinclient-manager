package org.openthinclient.manager.standalone;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ManagerStandaloneServerApplication {

    public static void main(String[] args) {
        new ApplicationControl().start(args);
    }

}

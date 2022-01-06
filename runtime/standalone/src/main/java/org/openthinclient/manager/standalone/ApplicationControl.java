package org.openthinclient.manager.standalone;

import org.openthinclient.manager.standalone.config.ManagerStandaloneServerConfiguration;
import org.openthinclient.manager.standalone.patch.PatchManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.util.RestartApplicationEvent;
import org.openthinclient.wizard.WizardApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ApplicationControl {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationControl.class);
    private volatile boolean restartScheduled;
    private volatile ConfigurableApplicationContext context;

    public void start(String[] args) {

        if (context != null)
            throw new IllegalStateException("The application has already been started");

        final ManagerHomeFactory managerHomeFactory = new ManagerHomeFactory();

        if (managerHomeFactory.isManagerHomeValidAndInstalled()) {
            PatchManagerHome patchManagerHome = new PatchManagerHome(managerHomeFactory.create());
            patchManagerHome.apply();

            LOG.info("\n" + //
                            "========================================================\n" + //
                            "\n" + //
                            "Starting the open thinclient server\n" + //
                            "\n" + //
                            "Home Directory:\n" + //
                            "{}\n" + //
                            "========================================================", //
                    managerHomeFactory.getManagerHomeDirectory());

            // start the default manager application
            context = SpringApplication.run(ManagerStandaloneServerConfiguration.class, args);
        } else {
            LOG.info("\n" + //
                            "========================================================\n" + //
                            "\n" + //
                            "open thinclient\n" + //
                            "First Time Installation\n" + //
                            "\n" + //
                            "========================================================", //
                    managerHomeFactory.getManagerHomeDirectory());
            // execute the first time installation
            context = SpringApplication.run(WizardApplicationConfiguration.class, args);
        }
        context.addApplicationListener(event -> {
            if (event instanceof RestartApplicationEvent) {
                restart(args);
            }
        });
        context.start();

    }

    public void restart(String[] args) {

        if (restartScheduled)
            return;

        LOG.info("\n" + //
                "========================================================\n" + //
                "\n" + //
                "open thinclient\n" + //
                "APPLICATION RESTART REQUESTED\n" + //
                "\n" + //
                "========================================================");

        restartScheduled = true;

        final Timer timer = new Timer(getClass().getSimpleName() + "-restart");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stop();
                start(args);
                restartScheduled = false;
                timer.cancel();
            }
        }, TimeUnit.SECONDS.toMillis(5));
    }

    public void stop() {
        if (context == null)
            return;
        try {
            context.close();
        } catch (Exception e) {
            LOG.error("Application shutdown failed. Exiting", e);
            System.exit(-1);
        }
        context = null;
    }
}

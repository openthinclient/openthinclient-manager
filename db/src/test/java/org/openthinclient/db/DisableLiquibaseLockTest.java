package org.openthinclient.db;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.lockservice.LockServiceFactory;
import liquibase.lockservice.ext.NoOpLockService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DisableLiquibaseLockTest {

    @Before
    public void before() {
        LockServiceFactory.getInstance().resetAll();
    }

    @Test
    public void testNoOpLockServiceIsActive() {
        LockServiceFactory lockServiceFactory = LockServiceFactory.getInstance();
        for (Database database: DatabaseFactory.getInstance().getImplementedDatabases()) {
            assertTrue(lockServiceFactory.getLockService(database) instanceof NoOpLockService);
        }
    }
}

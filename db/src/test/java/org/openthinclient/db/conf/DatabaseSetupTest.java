package org.openthinclient.db.conf;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Connection;
import java.sql.ResultSet;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = LocalTestConfig.class)
public class DatabaseSetupTest {

    @Autowired
    DataSource dataSource;

    @Test
    public void testSetupAndTablesPresent() throws Exception {

        final Connection connection = dataSource.getConnection();

        connection.createStatement().executeQuery("SELECT * FROM otc_package");

    }

    /**
     * This test ensures that the database is correctly initialized by liquibase. If, for some
     * reason, hibernate decides to do the initialization, description will be of type varchar
     * instead of CLOB
     */
    @Test
    public void testDescriptionFieldIsCLOB() throws Exception {

        final Connection connection = dataSource.getConnection();

        final ResultSet columns = connection.getMetaData().getColumns(null, null, "OTC_PACKAGE", "DESCRIPTION");

        assertTrue(columns.next());

        assertEquals("CLOB", columns.getString("TYPE_NAME"));

    }
}

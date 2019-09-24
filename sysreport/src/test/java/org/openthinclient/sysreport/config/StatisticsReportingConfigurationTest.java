package org.openthinclient.sysreport.config;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.Test;
import org.springframework.scheduling.support.CronSequenceGenerator;

public class StatisticsReportingConfigurationTest {

  @Test
  public void testCronExpressionWorks() {

    // defining a random starting point
    final LocalDateTime today = LocalDateTime.parse("2019-04-03T09:16:53.107", DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    CronSequenceGenerator cronTrigger = new CronSequenceGenerator(StatisticsReportingConfiguration.CRON_EXPRESSION);

    System.err.println(today);
    final Date next = cronTrigger.next(Date.from(today.toInstant(ZoneOffset.systemDefault().getRules().getOffset(today))));


    System.err.println(next);
    assertEquals(119, next.getYear());
    assertEquals(3, next.getMonth());
    // friday is day 5
    assertEquals(5, next.getDay());
    assertEquals(7, next.getHours());
    assertEquals(32, next.getMinutes());

  }
}

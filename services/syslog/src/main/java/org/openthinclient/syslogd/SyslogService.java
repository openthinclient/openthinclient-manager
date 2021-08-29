/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *******************************************************************************/
package org.openthinclient.syslogd;

import java.io.IOException;

import org.openthinclient.service.common.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author levigo
 * @author jn
 */
public class SyslogService implements Service<SyslogServiceConfiguration> {

  private static final Logger logger = LoggerFactory.getLogger(SyslogService.class);

  private SyslogDaemon daemon;

  private Thread daemonThread;

  private SyslogServiceConfiguration configuration;

  @Override
  public void setConfiguration(SyslogServiceConfiguration configuration) {
  	this.configuration = configuration;
  }

  @Override
  public SyslogServiceConfiguration getConfiguration() {
  	return configuration;
  }

  @Override
  public Class<SyslogServiceConfiguration> getConfigurationClass() {
  	return SyslogServiceConfiguration.class;
  }

  public void startService() throws Exception {
    try {

      daemon = new Log4JSyslogDaemon(0 != configuration.getSyslogPort()
          ? configuration.getSyslogPort()
          : SyslogDaemon.SYSLOG_PORT);

      daemonThread = new Thread(daemon, "Syslog daemon");
      daemonThread.setDaemon(true);
      daemonThread.start();
      logger.info("Syslog service launched");
    } catch (IOException e) {
      logger.error("Exception launching Syslog service", e);
      throw e;
    }
  }

  public void stopService() throws Exception {
    if (null != daemon) {
      daemon.shutdown();
      daemonThread.join(5000);
      if (daemonThread.isAlive())
        logger.error("Syslog daemon did not shut down in time.");
      else
        logger.info("Syslog service shut down.");
    }
  }

}

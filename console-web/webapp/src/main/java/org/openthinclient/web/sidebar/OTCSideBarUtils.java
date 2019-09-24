/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openthinclient.web.sidebar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.vaadin.spring.i18n.I18N;
import org.vaadin.spring.sidebar.SideBarUtils;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import java.util.*;

/**
 * Utility methods for working with side bars. This class is a Spring managed bean and is mainly
 * intended for internal use.
 *
 * @author Petter Holmström (petter@vaadin.com)
 */
public class OTCSideBarUtils extends SideBarUtils {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    ApplicationContext applicationContext;
    Map<String, Class> nameTypeMap = new HashMap<>();

    public OTCSideBarUtils(ApplicationContext applicationContext, I18N i18n) {
        super(applicationContext, i18n);
        this.applicationContext = applicationContext;
        scanForBeans();
    }

    private void scanForBeans() {
        logger.debug("Scanning for side bar items");
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(SideBarItem.class);
        for (String beanName : beanNames) {
            logger.debug("Bean [{}] declares a side bar item", beanName);
            Class<?> beanType = applicationContext.getType(beanName);
            nameTypeMap.put(beanName, beanType);
        }
    }

    public Map<String, Class> getNameTypeMap() {
        return nameTypeMap;
    }
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}

/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.openthinclient.flow.staticmenu;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinService;

/**
 * Generic helpers for the demo.
 *
 * @author Vaadin
 */
public interface Util {

    /**
     * Gets the name of the view.
     * <p>
     * This utility method exists so we can get the title based on only the view
     * class, for the menu. This works as we do not have any dynamic view names
     * in this site.
     *
     * @param navigationTarget
     *            the navigation target class
     * @return the target's name
     */
    static String getNavigationTargetName(
        Class<? extends Component> navigationTarget) {
        Optional<String> title = AnnotationReader
                .getAnnotationFor(navigationTarget, PageTitle.class)
                .map(PageTitle::value);
        return title.orElse(VaadinService.getCurrent().getInstantiator()
                .getI18NProvider().getTranslation(navigationTarget.getName(),
                        UI.getCurrent().getLocale()));
    }
}

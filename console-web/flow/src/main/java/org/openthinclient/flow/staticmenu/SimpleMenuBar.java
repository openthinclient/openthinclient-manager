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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;

/**
 * A menu which adds all items to a single div.
 *
 * @author Vaadin
 */
public class SimpleMenuBar extends MainMenuBar implements BeforeEnterObserver {

    private Div menu;

    /**
     * Creates the view.
     */
    public void init() {
        menu = new Div();
        menu.setClassName("submenu");

        add(menu, new Div());
    }

    protected Div getMenu() {
        return menu;
    }

    /**
     * Add a new menu element to this simple menu.
     *
     * @param navigationTarget
     *         menu element navigation target
     */
    public void addMenuElement(Class<? extends Component> navigationTarget) {
        menu.add(createLink(navigationTarget));
    }

    /**
     * Add a menu element for a navigation target with a parameter.
     *
     * @param navigationTarget
     *         navigation target
     * @param parameter
     *         parameter
     * @param <T>
     *         parameter type
     */
    public <T, C extends Component & HasUrlParameter<T>> void addMenuElement(
            Class<? extends C> navigationTarget, T parameter) {
        menu.add(createLink(navigationTarget, parameter));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        clearSelection();
        if (targetExists(event.getNavigationTarget())) {
            activateMenuTarget(event.getNavigationTarget());
        } else {
            StringBuilder path = new StringBuilder();
            for (String segment : event.getLocation().getSegments()) {
                path.append(segment);
                Optional<Class> target = getTargetForPath(path.toString());
                if (target.isPresent()) {
                    activateMenuTarget(target.get());
                    break;
                }
                path.append("/");
            }
        }
    }
}

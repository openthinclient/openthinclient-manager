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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import org.openthinclient.flow.filebrowser.FileBrowserView;
import org.openthinclient.flow.packagemanager.SourcesListNavigatorView;
import org.openthinclient.flow.sampleviews.AboutView;
import org.openthinclient.flow.sampleviews.HomeView;
import org.openthinclient.flow.sampleviews.ResourcesView;
import org.openthinclient.flow.sampleviews.SimpleView;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main menu bar containing top level navigation items.
 *
 * @author Vaadin
 */
public class MainMenu extends MainMenuBar implements AfterNavigationObserver {

    @Override
    public void init() {
        initHomeLink();
        initLinkContainer();
    }

    private void initHomeLink() {
        Anchor homeLink = new Anchor("/ui/", "");
        homeLink.getElement().setAttribute("router-link", "true");
        Div logo = new Div();
        logo.setClassName("logo");
        homeLink.add(logo);
        add(homeLink);
    }

    private void initLinkContainer() {
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("topnav");
        add(layout);

//        layout.add(createLink(HomeView.class));
        layout.add(createLink(AboutView.class));
//        layout.add(createLink(ResourcesView.class));
        layout.add(createLink(FileBrowserView.class));
        layout.add(createLink(SourcesListNavigatorView.class));

        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER,
                layout.getChildren().collect(Collectors.toList())
                        .toArray(new Component[] {}));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (event.getLocation().getPath().isEmpty()) {
            clearSelection();
        } else {
            StringBuilder path = new StringBuilder();
            for (String segment : event.getLocation().getSegments()) {
                path.append(segment);
                Optional<Class> target = getTargetForPath(path.toString());
                if (target.isPresent()) {
                    clearSelection();
                    activateMenuTarget(target.get());
                    break;
                }
                path.append("/");
            }
        }
    }


}

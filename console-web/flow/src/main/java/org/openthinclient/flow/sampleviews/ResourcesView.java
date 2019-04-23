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
package org.openthinclient.flow.sampleviews;

import java.io.InputStream;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.*;
import org.openthinclient.flow.MainLayout;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.startup.FakeBrowser;

/**
 * A view that shows different resources based on the URL.
 *
 * @since
 * @author Vaadin Ltd
 */
@Route(value = ResourcesView.NAME, layout = MainLayout.class)
@RoutePrefix(value = "ui")
public final class ResourcesView extends SimpleView
        implements HasUrlParameter<String> {

    public static final String NAME = "sampleviews/resource";

    private Div content;
    private IFrame iframe;

    @Tag("iframe")
    private static final class IFrame extends HtmlComponent {
        private static final PropertyDescriptor<String, Optional<String>> srcDescriptor = PropertyDescriptors
                .optionalAttributeWithDefault("src", "");

        private IFrame() {
            getStyle().set("width", "50%");
            getStyle().set("height", "400px");
        }

        void setSrc(String src) {
            set(srcDescriptor, src);
        }
    }

    /**
     * Creates a new view.
     */
    public ResourcesView() {
        add(super.getMappingInfo("resource/*"));
        iframe = new IFrame();
        content = new Div();
        content.setClassName("content");

        HtmlContainer links = new HtmlContainer("ul");
        links.add(createLink("", "<none>", "none"));
        links.add(createLink(VaadinService.getCurrent().resolveResource(
                "frontend://images/vaadin-logo-small.png",
                FakeBrowser.getEs6()), "logo"));

        HtmlContainer header = new HtmlContainer("p");
        HtmlContainer strong = new HtmlContainer("strong");
        strong.setText("Select the resource to display");
        header.add(strong);

        content.getElement().appendChild(
                ElementFactory.createSpan("Select the resource to display"));

        content.add(links, header, iframe);

        add(content);
    }

    private static Component createLink(String resource, String id) {
        return createLink(resource, resource, id);
    }

    private static Component createLink(String resource, String caption,
            String id) {
        RouterLink link = new RouterLink(caption, ResourcesView.class,
                resource);
        link.setId(id);
        HtmlContainer li = new HtmlContainer("li");
        li.add(link);
        return li;
    }

    private static boolean resourceExists(String resourcePath) {
        InputStream stream = VaadinServletService.getCurrentServletRequest()
                .getServletContext().getResourceAsStream(resourcePath);
        return stream != null;
    }

    @Override
    public void setParameter(BeforeEvent event,
            @WildcardParameter String parameter) {
        if ("".equals(parameter)) {
            content.getElement().setChild(3,
                    ElementFactory.createDiv("No resource selected"));
        } else if (resourceExists(parameter)) {
            iframe.setSrc(parameter);
            content.getElement().setChild(3, iframe.getElement());
        } else {
            content.getElement().setChild(3, ElementFactory
                    .createDiv("Resource " + parameter + " not available"));
        }
    }

}

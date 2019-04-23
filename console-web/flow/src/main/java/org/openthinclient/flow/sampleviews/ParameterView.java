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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.*;
import org.openthinclient.flow.MainLayout;
import com.vaadin.flow.server.VaadinService;

/**
 * A dynamic view that shows different content based on a parameter in the URL.
 *
 * @since
 * @author Vaadin Ltd
 */
@Route(value = "param", layout = MainLayout.class)
@RoutePrefix(value = "ui")
public final class ParameterView extends SimpleView
        implements HasUrlParameter<Integer> {

    private Div idElement;
    private RouterLink linkToNext;

    /**
     * Creates a new dynamic view.
     */
    public ParameterView() {
        add(super.getMappingInfo("param/{id}"));
        Div content = new Div();
        content.setClassName("content");

        idElement = new Div();
        linkToNext = new RouterLink("Open the same view with another parameter",
                ParameterView.class, 1);

        content.add(idElement, linkToNext);
        add(content);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        idElement.setText("Id parameter: " + parameter);
        linkToNext.setRoute(VaadinService.getCurrent().getRouter(),
                ParameterView.class, parameter + 1);
    }

}

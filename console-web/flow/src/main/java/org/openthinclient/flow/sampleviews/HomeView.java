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
import com.vaadin.flow.router.RoutePrefix;
import org.openthinclient.flow.MainLayout;
import com.vaadin.flow.router.Route;

/**
 * The static home page.
 *
 * @since
 * @author Vaadin Ltd
 */
@Route(value = "sampleviews/home", layout = MainLayout.class)
@RoutePrefix(value = "ui")
public final class HomeView extends SimpleView {
    /**
     * Creates a new home view.
     */
    public HomeView() {
        add(super.getMappingInfo(""));
        Div div = new Div();
        div.setText("This is the home page");
        div.setClassName("content");
        add(div);
    }


}

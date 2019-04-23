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
import org.openthinclient.flow.staticmenu.BaseViewLayout;

/**
 * The static about view.
 *
 * @since
 * @author Vaadin Ltd
 */
@Route(value = "about", layout = MainLayout.class)
@RoutePrefix(value = "ui")
public final class AboutView extends BaseViewLayout {
    /**
     * Creates as new view.
     */
    public AboutView() {
//        add(super.getMappingInfo("about"));
        Div div = new Div();
        div.setText("This is the about page");
        div.setClassName("content");
        add(div);
    }
}

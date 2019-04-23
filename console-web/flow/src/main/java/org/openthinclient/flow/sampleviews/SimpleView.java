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

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.HasDynamicTitle;

/**
 * Abstract view class using the same element all the time.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class SimpleView extends Div implements HasDynamicTitle {

    protected final Div getMappingInfo(String mappingInfo) {
        Div mapping = new Div();
        HtmlContainer textElement = new HtmlContainer("strong");
        textElement.setText("Mapped using \"" + mappingInfo + "\"");
        textElement.getStyle().set("borderBottom", "1px solid black");
        mapping.getStyle().set("marginBottom", "1em");

        mapping.add(textElement);
        return mapping;

    }

    @Override
    public String getPageTitle() {
        // Title used for all views
        return "OTC Flow Web Site ";
    }
}

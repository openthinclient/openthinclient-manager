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
package org.openthinclient.flow;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.flow.server.BootstrapListener;
import com.vaadin.flow.server.BootstrapPageResponse;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Custom service initialization listener which adds bootstrap listener to
 * modify the bootstrap application page.
 * 
 * @since
 * @author Vaadin Ltd
 */
public class SiteServiceInitListener implements VaadinServiceInitListener {

    private static class SiteBootstrapListener implements BootstrapListener {

        @Override
        public void modifyBootstrapPage(BootstrapPageResponse response) {
            Document document = response.getDocument();

            Element head = document.head();

            head.appendChild(createMeta(document, "og:title", "openthinclient Manager"));

        }

        private Element createMeta(Document document, String property,
                String content) {
            Element meta = document.createElement("meta");
            meta.attr("property", property);
            meta.attr("content", content);
            return meta;
        }

    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addBootstrapListener(new SiteBootstrapListener());
    }

}

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.RoutePrefix;
import org.openthinclient.flow.MainLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

/**
 * A view that uses generated resources.
 *
 * @since
 * @author Vaadin Ltd
 */
@Route(value = "dynresource", layout = MainLayout.class)
@RoutePrefix(value = "ui")
public final class DynamicResourcesView extends SimpleView {

    private Input name;
    private Element image;

    /**
     * Creates a new view.
     */
    public DynamicResourcesView() {
        add(super.getMappingInfo("dynresource"));
        initContent();
    }

    private void initContent() {
        name = new Input();
        name.getStyle().set("display", "block");

        Div div = new Div();
        div.setText("Type a string to generate an image");
        add(name, div);
        createGenerateImageButton();
        createImageOpener();
        createImage();
    }

    private void createGenerateImageButton() {
        NativeButton button = new NativeButton("Generate image");

        button.addClickListener(event -> {
            image.setAttribute("data", createResource());
        });

        add(button);
    }

    private void createImageOpener() {
        NativeButton button = new NativeButton("Open image in a new window");
        add(button);

        // Delay until element is attached so we can find UI and Page
        button.getElement().addAttachListener(e -> {
            button.getUI().get().getPage().executeJavaScript(
                    // Connect directly on the client side so we don't have a
                    // server visit
                    "$0.addEventListener('click',function(){window.open($1.getAttribute('data'))});",
                    button, image);
        });

    }

    private void createImage() {
        image = new Element("object");
        image.setAttribute("type", "image/svg+xml");
        image.getStyle().set("display", "block");
        image.setAttribute("data", createResource());
        getElement().appendChild(image);

    }

    private StreamResource createResource() {
        return new StreamResource("image.svg", this::getImageInputStream);
    }

    private InputStream getImageInputStream() {
        String svg = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>"
                + "<svg  xmlns='http://www.w3.org/2000/svg' "
                + "xmlns:xlink='http://www.w3.org/1999/xlink'>"
                + "<rect x='10' y='10' height='100' width='100' "
                + "style=' fill: #90C3D4'/>" + "<text x='30' y='30' fill='red'>"
                + name.getValue() + "</text>" + "</svg>";
        return new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8));
    }
}

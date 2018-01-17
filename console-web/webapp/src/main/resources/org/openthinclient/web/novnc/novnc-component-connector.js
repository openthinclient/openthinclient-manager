window.org_openthinclient_web_novnc_NoVNCComponent =
    function() {
        // Create the component
        //var mycomponent =
        //    new mylibrary.MyComponent(this.getElement());

        var content = document.createElement("iframe");
        this.getElement().appendChild(content);
        content.width="100%";
        content.height="100%";
        content.setAttribute("frameBorder", "0");
        content.setAttribute("allowTransparency", "true");
        content.setAttribute("allowfullscreen", true);


        // Handle changes from the server-side
        this.onStateChange = function() {

            if(this.getState().resources.novnc != undefined) {
                content.src = this.getState().resources.novnc.uRL;
            }

        };

        // Pass user interaction to the server-side
        // var self = this;
        // mycomponent.click = function() {
        //     self.onClick(mycomponent.getValue());
        // };
    };
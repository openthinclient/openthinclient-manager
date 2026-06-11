package org.openthinclient.api.ws;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpResponse;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    static final Pattern XForwardedForPattern = Pattern.compile(
            "\\d{1,3}(\\.\\d{1,3}){3}");

    static final Pattern ForwardedForPattern = Pattern.compile(
            "(?i)(?:^|,)\\s*for=\"?(\\d{1,3}(?:\\.\\d{1,3}){3})");


    /** Get client's IP even if proxied and save as session attribute "ip" */
    @Override
    public boolean beforeHandshake( ServerHttpRequest request,
                                    ServerHttpResponse response,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes ) {
        String ip = null;
        HttpHeaders headers = request.getHeaders();
        String forwarded = headers.getFirst("Forwarded");
        if (forwarded != null) {
            Matcher m = ForwardedForPattern.matcher(forwarded);
            if (m.find()) ip = m.group(1);
        } else {
            String xForwardedFor = headers.getFirst("X-Forwarded-For");
            if (xForwardedFor != null) {
                Matcher m = XForwardedForPattern.matcher(xForwardedFor);
                if (m.find()) ip = m.group();
            }
        }
        if (ip == null) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }
        attributes.put("ip", ip);
        return true;
    }


    @Override
    public void afterHandshake( ServerHttpRequest request,
                                ServerHttpResponse response,
                                WebSocketHandler wsHandler,
                                Exception e) {
    }
}

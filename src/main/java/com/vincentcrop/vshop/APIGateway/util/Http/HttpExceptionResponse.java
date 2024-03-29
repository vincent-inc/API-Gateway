package com.vincentcrop.vshop.APIGateway.util.Http;

import org.springframework.web.server.ResponseStatusException;

import com.vincentcrop.vshop.APIGateway.util.Time;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class HttpExceptionResponse {
    private String UUID;
    private String time;
    private HttpStatus status;
    private String message;
    private String detailMessageCode;
    private String reason;
    private String localizedMessage;

    private final static String CUT_MESSAGE1 = "\"message\":";
    private final static String CUT_MESSAGE2 = "\\\"";

    public HttpExceptionResponse() {
        this.UUID = java.util.UUID.randomUUID().toString();
        this.time = Time.now().toSpring();
    }

    public HttpExceptionResponse(ResponseStatusException ex) {
        this.UUID = java.util.UUID.randomUUID().toString();
        this.time = Time.now().toSpring();
        this.message = extractMessage(ex.getMessage());
        this.status = new HttpStatus(ex.getStatusCode());
        this.reason = ex.getReason();
        this.localizedMessage = ex.getLocalizedMessage();
        this.detailMessageCode = ex.getDetailMessageCode();
    }

    public static String extractMessage(String originMessage) {
        var newMessage = originMessage;

        var index = newMessage.indexOf(CUT_MESSAGE1);

        if (index < 0)
            return originMessage;

        newMessage = newMessage.substring(index + CUT_MESSAGE1.length());

        index = newMessage.indexOf(CUT_MESSAGE2);

        if (index < 0)
            return originMessage;

        newMessage = newMessage.substring(index + CUT_MESSAGE2.length());

        index = newMessage.indexOf(CUT_MESSAGE2);

        if (index < 0)
            return originMessage;

        newMessage = newMessage.substring(0, index);

        return newMessage;
    }

    public void mask() {
        this.reason = null;
        this.localizedMessage = null;
        this.detailMessageCode = null;
    }
}

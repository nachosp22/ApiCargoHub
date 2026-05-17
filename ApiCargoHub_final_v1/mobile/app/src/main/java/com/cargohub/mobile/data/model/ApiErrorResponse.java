package com.cargohub.mobile.data.model;

import java.util.List;

public class ApiErrorResponse {

    private String message;
    private String detail;
    private List<String> details;

    public String getMessage() {
        if (message != null && !message.isEmpty()) {
            return message;
        }
        return detail;
    }

    public String getDetail() {
        return detail;
    }

    public List<String> getDetails() {
        return details;
    }
}

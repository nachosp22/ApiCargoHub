package com.cargohub.mobile.data.model;

import java.util.List;

public class ApiErrorResponse {

    private String message;
    private List<String> details;

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }
}

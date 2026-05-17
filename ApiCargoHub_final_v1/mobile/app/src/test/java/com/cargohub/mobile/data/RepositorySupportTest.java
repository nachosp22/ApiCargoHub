package com.cargohub.mobile.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class RepositorySupportTest {

    @Test
    public void fromResponse_marksUnauthorizedFor401Only() {
        RepositoryResult<String> unauthorized = RepositorySupport.fromResponse(
                Response.error(401, jsonBody("{\"message\":\"Unauthorized\"}")),
                "fallback"
        );
        RepositoryResult<String> forbidden = RepositorySupport.fromResponse(
                Response.error(403, jsonBody("{\"message\":\"Forbidden\"}")),
                "fallback"
        );

        assertFalse(unauthorized.isSuccessful());
        assertTrue(unauthorized.isUnauthorized());
        assertEquals(401, unauthorized.getCode());

        assertFalse(forbidden.isSuccessful());
        assertFalse(forbidden.isUnauthorized());
        assertEquals(403, forbidden.getCode());
    }

    private ResponseBody jsonBody(String body) {
        return ResponseBody.create(MediaType.parse("application/json"), body);
    }
}

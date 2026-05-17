package com.cargohub.mobile.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cargohub.mobile.data.model.LoginResponse;

import org.junit.Test;

import java.time.Instant;

public class SessionSnapshotTest {

    @Test
    public void fromLoginResponse_prefersConductorIdAndExpiry() {
        LoginResponse response = new LoginResponse();
        response.setAccessToken("token-value");
        response.setRol("CONDUCTOR");
        response.setId(99L);
        response.setConductorId(12L);
        response.setExpiresAt(Instant.now().plusSeconds(600).toString());

        SessionSnapshot snapshot = SessionSnapshot.fromLoginResponse(response);

        assertNotNull(snapshot.getExpiresAtEpochMs());
        assertTrue(snapshot.isActive(System.currentTimeMillis()));
        assertTrue(snapshot.getConductorId() == 12L);
    }

    @Test
    public void isActive_returnsFalseWhenExpired() {
        SessionSnapshot snapshot = new SessionSnapshot("token-value", "CONDUCTOR", 12L, System.currentTimeMillis() - 1000L);

        assertFalse(snapshot.isActive(System.currentTimeMillis()));
    }
}

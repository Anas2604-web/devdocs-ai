package com.devdocsai.auth;

import com.devdocsai.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldStoreAndRetrieveTenantId() {
        TenantContext.setCurrentTenant("tenant-abc");
        assertEquals("tenant-abc", TenantContext.getCurrentTenant());
    }

    @Test
    void shouldThrowWhenNotSet() {
        assertThrows(IllegalStateException.class, TenantContext::getCurrentTenant);
    }

    @Test
    void shouldReturnFalseWhenNotSet() {
        assertFalse(TenantContext.hasTenant());
    }

    @Test
    void shouldReturnTrueWhenSet() {
        TenantContext.setCurrentTenant("tenant-xyz");
        assertTrue(TenantContext.hasTenant());
    }

    @Test
    void shouldClearCorrectly() {
        TenantContext.setCurrentTenant("tenant-abc");
        TenantContext.clear();
        assertFalse(TenantContext.hasTenant());
    }

    @Test
    void shouldIsolateBetweenThreads() throws InterruptedException {
        TenantContext.setCurrentTenant("main-thread-tenant");
        AtomicReference<Boolean> childHasTenant = new AtomicReference<>();

        Thread child = new Thread(() -> childHasTenant.set(TenantContext.hasTenant()));
        child.start();
        child.join();

        assertFalse(childHasTenant.get(), "Child thread should NOT see main thread's tenant");
        assertEquals("main-thread-tenant", TenantContext.getCurrentTenant());
    }
}

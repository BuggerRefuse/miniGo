package org.example.admin.domain.aggregate;

import org.example.admin.domain.valueobject.AdminStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminTest {

    @Test
    void activeAdminCanLoginWithMatchingPassword() {
        Admin admin = Admin.restore(
                1L,
                "admin",
                "encoded-password",
                "系统管理员",
                AdminStatus.ACTIVE
        );

        assertTrue(admin.canLogin("123456", (raw, encoded) ->
                "123456".equals(raw) && "encoded-password".equals(encoded)));
    }

    @Test
    void disabledAdminCannotLogin() {
        Admin admin = Admin.restore(
                1L,
                "admin",
                "encoded-password",
                "系统管理员",
                AdminStatus.DISABLED
        );

        assertFalse(admin.canLogin("123456", (raw, encoded) -> true));
    }
}

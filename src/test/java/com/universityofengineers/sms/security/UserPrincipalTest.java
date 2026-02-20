package com.universityofengineers.sms.security;

import com.universityofengineers.sms.entity.Role;
import com.universityofengineers.sms.entity.UserAccount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void shouldExposeIdEmailRole_andAuthoritiesFollowingSpringConvention() {
        UserAccount account = UserAccount.builder()
                .id(42L)
                .email("user@ue.edu")
                .passwordHash("hash")
                .role(Role.TEACHER)
                .enabled(true)
                .build();

        UserPrincipal principal = new UserPrincipal(account);

        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getUsername()).isEqualTo("user@ue.edu");
        assertThat(principal.getPassword()).isEqualTo("hash");
        assertThat(principal.getRole()).isEqualTo(Role.TEACHER);
        assertThat(principal.isEnabled()).isTrue();

        assertThat(principal.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_TEACHER");
    }

    @Test
    void disabledAccount_shouldBeNonExpiredNonLockedNonCredentials_andNotEnabled() {
        UserAccount account = UserAccount.builder()
                .id(1L)
                .email("disabled@ue.edu")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .enabled(false)
                .build();

        UserPrincipal principal = new UserPrincipal(account);

        assertThat(principal.isEnabled()).isFalse();
        assertThat(principal.isAccountNonExpired()).isFalse();
        assertThat(principal.isAccountNonLocked()).isFalse();
        assertThat(principal.isCredentialsNonExpired()).isFalse();
    }
}

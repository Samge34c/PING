package com.ping.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthSupportTests {
    private final AccountService accounts = mock(AccountService.class);
    private final AuthSupport support = new AuthSupport(accounts, mock(Environment.class));

    private PingUser user(long id, boolean organizer, long epoch) {
        return new PingUser(id, "person" + id + "@example.test", "not-a-real-password-hash", true, false, organizer, epoch);
    }

    private void authenticated(PingUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }

    private void request() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach void cleanup() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test void repeatedChecksWithinOneRequestReadTheAccountOnce() {
        var stored = user(1, false, 0);
        var fresh = user(1, true, 1);
        authenticated(stored); request();
        when(accounts.user(1)).thenReturn(fresh);

        assertThat(support.current()).isSameAs(fresh);
        assertThat(support.require()).isSameAs(fresh);
        assertThat(support.current().organizer()).isTrue();
        verify(accounts, times(1)).user(1);
    }

    @Test void theNextRequestReadsFreshRolesAndEpochEvenWithTheSameSessionPrincipal() {
        var stored = user(1, true, 0);
        var revoked = user(1, false, 1);
        authenticated(stored);
        when(accounts.user(1)).thenReturn(stored, revoked);

        request(); assertThat(support.current()).isSameAs(stored);
        request(); assertThat(support.current()).isSameAs(revoked);
        assertThat(support.require().epoch()).isEqualTo(1);
        assertThat(support.current().organizer()).isFalse();
        verify(accounts, times(2)).user(1);
    }

    @Test void changingIdentityWithinARequestCannotReuseThePreviousAccount() {
        var first = user(1, false, 0); var second = user(2, true, 0);
        when(accounts.user(1)).thenReturn(first); when(accounts.user(2)).thenReturn(second);
        request(); authenticated(first); assertThat(support.current()).isSameAs(first);
        authenticated(second); assertThat(support.current()).isSameAs(second);
        SecurityContextHolder.clearContext(); assertThat(support.current()).isNull();
        assertThatThrownBy(support::require).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(accounts, times(1)).user(1); verify(accounts, times(1)).user(2);
    }

    @Test void demoFlagsReuseOneFreshAccountReadWithinTheRequest() {
        var env = mock(Environment.class);
        when(env.getProperty("ping.demo.enabled", Boolean.class, false)).thenReturn(true);
        var demo = new AuthSupport(accounts, env);
        var general = new PingUser(1, "general@example.test", "not-a-real-password-hash", true, true, false, 0);
        authenticated(general); request(); when(accounts.user(1)).thenReturn(general);
        assertThat(demo.demo()).isTrue(); assertThat(demo.demo()).isTrue();
        verify(accounts, times(1)).user(1);
    }

    @Test void replacingAuthenticationForTheSameAccountReadsAgain() {
        var user = user(1, false, 0);
        request(); authenticated(user); when(accounts.user(1)).thenReturn(user);
        support.current(); authenticated(user); support.current();
        verify(accounts, times(2)).user(1);
    }

    @Test void rotatingOrReplacingTheSessionReadsAgainWithinTheSameRequest() {
        var user = user(1, false, 0);
        var http = new MockHttpServletRequest(); http.getSession();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(http));
        authenticated(user); when(accounts.user(1)).thenReturn(user);
        support.current(); support.current();
        http.changeSessionId(); support.current(); support.current();
        http.getSession().invalidate(); http.getSession(); support.current();
        verify(accounts, times(3)).user(1);
    }

    @Test void signingOutAndRestoringAuthenticationCannotReuseAnEarlierRead() {
        var user = user(1, false, 0);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        request(); SecurityContextHolder.getContext().setAuthentication(authentication);
        when(accounts.user(1)).thenReturn(user);
        support.current(); SecurityContextHolder.clearContext(); assertThat(support.current()).isNull();
        SecurityContextHolder.getContext().setAuthentication(authentication); support.current();
        verify(accounts, times(2)).user(1);
    }

    @Test void nonWebCallersDoNotAcquireALongLivedAccountCache() {
        var first = user(1, true, 0); var revoked = user(1, false, 1);
        authenticated(first); when(accounts.user(1)).thenReturn(first, revoked);
        assertThat(support.current()).isSameAs(first);
        assertThat(support.current()).isSameAs(revoked);
        verify(accounts, times(2)).user(1);
    }

    @Test void failedAccountReadsAreNotRememberedAsSuccessfulAuthentication() {
        var fresh = user(1, false, 0);
        authenticated(fresh); request();
        when(accounts.user(1)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("test outage")).thenReturn(fresh);
        assertThatThrownBy(support::current).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(support.current()).isSameAs(fresh);
        assertThat(support.current()).isSameAs(fresh);
        verify(accounts, times(2)).user(1);
    }
}

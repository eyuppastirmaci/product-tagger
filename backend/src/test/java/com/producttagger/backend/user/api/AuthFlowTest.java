package com.producttagger.backend.user.api;

import com.producttagger.backend.IntegrationTest;
import com.producttagger.backend.shared.security.AuthCookies;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthFlowTest extends IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sessionLifecycleWithRotationAndReuseDetection() throws Exception {
        String email = uniqueEmail();

        MvcResult registered = register(email);

        String accessToken = cookieValue(registered, AuthCookies.TOKEN_COOKIE);
        String firstRefresh = cookieValue(registered, AuthCookies.REFRESH_COOKIE);

        assertThat(setCookieHeader(registered, AuthCookies.REFRESH_COOKIE))
                .contains("HttpOnly").contains("SameSite=Lax").contains("Path=/api/auth");

        // The access cookie authenticates /me
        mvc.perform(get("/api/auth/me").cookie(new Cookie(AuthCookies.TOKEN_COOKIE, accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // Rotation: refresh yields a different refresh token
        MvcResult refreshed = mvc.perform(refresh(firstRefresh))
                .andExpect(status().isOk())
                .andReturn();

        String secondRefresh = cookieValue(refreshed, AuthCookies.REFRESH_COOKIE);

        assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        // Reuse of the rotated-out token is an incident: every session drops,
        // so even the freshly issued token stops working
        mvc.perform(refresh(firstRefresh)).andExpect(status().isUnauthorized());
        mvc.perform(refresh(secondRefresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheSessionServerSide() throws Exception {
        MvcResult registered = register(uniqueEmail());

        String accessToken = cookieValue(registered, AuthCookies.TOKEN_COOKIE);
        String refreshToken = cookieValue(registered, AuthCookies.REFRESH_COOKIE);

        mvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie(AuthCookies.TOKEN_COOKIE, accessToken))
                        .cookie(new Cookie(AuthCookies.REFRESH_COOKIE, refreshToken)))
                .andExpect(status().isNoContent());

        mvc.perform(refresh(refreshToken)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        String email = uniqueEmail();

        register(email);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult register(String email) throws Exception {
        return mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test User\",\"email\":\"%s\",\"password\":\"password123\"}"
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder refresh(String refreshToken) {
        return post("/api/auth/refresh").cookie(new Cookie(AuthCookies.REFRESH_COOKIE, refreshToken));
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    // Cookies are written as raw Set-Cookie headers, so parse them from there
    private static String cookieValue(MvcResult result, String name) {
        String header = setCookieHeader(result, name);

        return header.split(";", 2)[0].substring(name.length() + 1);
    }

    private static String setCookieHeader(MvcResult result, String name) {
        return result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(header -> header.startsWith(name + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Set-Cookie header for " + name));
    }
}

package tn.mathzero.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

public class AuthCsrfFilter extends OncePerRequestFilter {
    public static final String COOKIE_NAME = "mathzero_csrf";
    public static final String HEADER_NAME = "X-CSRF-Token";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean cookieChangingAuthRequest =
                "POST".equals(method)
                        && (path.endsWith("/auth/login")
                        || path.endsWith("/auth/register")
                        || path.endsWith("/auth/refresh")
                        || path.endsWith("/auth/logout")
                        || path.endsWith("/auth/logout-all"));

        if (cookieChangingAuthRequest) {
            String cookieToken = null;
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (COOKIE_NAME.equals(cookie.getName())) {
                        cookieToken = cookie.getValue();
                        break;
                    }
                }
            }
            String headerToken = request.getHeader(HEADER_NAME);
            if (cookieToken == null || cookieToken.isBlank()
                    || headerToken == null || !cookieToken.equals(headerToken)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"CSRF_INVALID\",\"message\":\"Security validation failed. Refresh the page and try again.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

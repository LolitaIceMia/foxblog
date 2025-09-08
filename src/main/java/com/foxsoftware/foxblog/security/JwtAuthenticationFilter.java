package com.foxsoftware.foxblog.security;

import com.foxsoftware.foxblog.security.ProductionJwtProvider.JwtVerifyException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * 从 Authorization: Bearer <token> 中解析 & 验证 JWT。
 * 使用 ProductionJwtProvider 的 parseAndValidate。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ProductionJwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                var vt = jwtProvider.parseAndValidate(token);
                var authorities = vt.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toList());
                var auth = new UsernamePasswordAuthenticationToken(vt.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtVerifyException e) {
                writeError(response, 401, "INVALID_TOKEN", e.getMessage());
                return;
            } catch (Exception e) {
                writeError(response, 500, "TOKEN_ERROR", e.getMessage());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse resp, int status, String code, String msg) throws IOException {
        if (resp.isCommitted()) return;
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter()
                .write("{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(msg) + "\"}");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    @Value
    static class ErrorBody {
        String code;
        String message;
    }
}
package com.foxsoftware.foxblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * 从 Authorization: Bearer <token> 中解析 JWT
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ProductionJwtProvider jwtProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            try {
                var parsed = jwtProvider.parseAndValidate(token);
                var authorities = parsed.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toList());
                var authentication = new UsernamePasswordAuthenticationToken(parsed.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ProductionJwtProvider.TokenVerifyException e) {
                // 失败：返回 401 并短路
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
        resp.getWriter().write(objectMapper.writeValueAsString(new Err(code, msg)));
    }

    @Value
    static class Err {
        String code;
        String message;
    }
}
package com.intelligent.ecommerce.security;

import org.springframework.stereotype.Component;
import com.intelligent.ecommerce.dto.common.ApiResponse;
import jakarta.servlet.http.*;
import org.springframework.security.core.AuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RestAuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws java.io.IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");

        var body = ApiResponse.error(
        "Unauthorized: missing or invalid token"
        );
        response.getWriter().write(om.writeValueAsString(body));
    }
}

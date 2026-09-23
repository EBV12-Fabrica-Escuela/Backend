package com.ebv12.backend.config;

import com.ebv12.backend.api.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Filtro PROVISIONAL de autenticación.
 *
 * Exige la presencia de un header "Authorization: Bearer <token>" en
 * POST /api/reservas, para cumplir el criterio no funcional de HU-03
 * ("Solo usuarios autenticados pueden reservar").
 *
 * IMPORTANTE: esto NO valida la firma, expiración ni el emisor del token —
 * es un candado a nivel de API mientras se implementa la capa real de
 * autenticación (JWT + Spring Security) en una tarea posterior del backlog.
 * Cuando esa tarea se implemente, el id del cliente debe extraerse del
 * token (no venir en el body de CreateReservationRequest como hoy).
 */
@Component
public class AuthRequiredFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (requiresAuth(request) && !hasBearerToken(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            ErrorResponse error = new ErrorResponse(
                    Instant.now(), 401, "UNAUTHORIZED",
                    "Debe iniciar sesión para realizar una reserva", request.getRequestURI());
            response.getWriter().write(MAPPER.writeValueAsString(error));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean requiresAuth(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/reservas".equals(request.getRequestURI());
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") && header.length() > 7;
    }
}

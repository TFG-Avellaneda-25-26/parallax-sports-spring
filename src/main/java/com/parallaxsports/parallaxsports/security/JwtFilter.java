package com.parallaxsports.parallaxsports.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro para comprobar si hay un JWT incluido en los encabezados de las solicitudes.
 * En una aplicación web, cada petición HTTP pasa por una cadena de filtros (filter chain) antes de llegar al controlador
 * que realmente atiende la solicitud.
 * Es como los Filter de Jakarta Servlet API.
 * (por ejemplo, el DispatcherServlet de Spring MVC)...
 * OncePerRequestFilter: evitar que tu filtro se ejecute más de una vez por la misma petición (por ejemplo, en redirecciones o filtros interno)
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // POST /api/productos
        // Authorization: Bearer afsldjflaksd flaksdf.asdflasdjf askldfjalsdfkjalsdfjlasdfjalsdf.sadjfñklasd fklasdjflkasd

        System.out.println("Authorization header: [" + authHeader+"]");

        // Si no hay token, la solicitud sigue su curso (para endpoint públicos)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // pasa palante!
            return; // no siga la ejecución
        }

        // Lleva Bearer...
        final String token = authHeader.substring(7); //Bearer (ocupa 7)
        System.out.println("token: " + token);

        // Vamos a analizar el token
        final String username = jwtService.extractUsername(token);

        System.out.println("Username: " + username);

        // Verifica que no haya ya una autenticación previa a la solicitud
        // Si ya está autenticado, no hace falta volver a autenticar
        // Comprobamos que todavía no hay un usuario autenticado en el contexto actual.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Cargamos el usuario de la base de datos
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if(jwtService.isTokenValid(token,userDetails)){

                // Crear el objeto con el usuario autenticado y su roles
                // Segundo parámetro es null porque no necesitamos almacenar las credenciales (la contraseña) porque el token es la prueba de autenticación
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());

                // Asociar detalles de la solicitud (información adicional del contexto de la socitud http, como ip, nombre del host...)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Registramos al usuario como autenticado
                // Crea una autenticación nueva y la registra en el contexto de seguridad
                // SecurityContextHolder es un contenedor que guarda quién está autenticado en el hilo actual de ejecución.
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }

        }

        filterChain.doFilter(request, response);

    }
}

package com.parallaxsports.parallaxsports.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    // LO QUE HACEMOS NOSOTROS = En lugar de usar una clave fija en application.properties o .env, genera una nueva clave de forma dinámica.
    // Si usas SECRET_KEY generado dinámicamente en cada arranque, no podrás validar tokens emitidos en otra ejecución.
    //private final SecretKey SECRET_KEY = generateSecureKey();

    // Si lo cargo desde application.properties ... problemas de seguridad en producción
//    @Value("${jwt.secret}")
//    private String secret;

//    @Value("${jwt.expiration}")
//    private String expiration;

    @Value("${JWT_SECRET}")
    private String secret;
    @Value("${JWT_EXPIRATION}")
    private String expiration;


    /**
     * Genera una clave nueva, aleatoria y segura en tiempo de ejecución.
     * Ideal para pruebas o ejemplos donde NO necesitas persistencia del token entre reinicios del servidor.
     * Cada vez que reinicias la app, se genera una nueva clave.
     * Esto hace que los tokens emitidos antes del reinicio ya no sean válidos, porque la firma ya no coincide.
     * @return
     */
    private SecretKey generateSecureKey() {
        return Jwts.SIG.HS256.key().build(); // Genera una clave segura aleatoria de 256 bits
    }

    /**
     * Devuelve la clave secreta usada para firmar y validar tokens.
     * @return
     */
    private SecretKey getSigningKey() {
        //return SECRET_KEY;
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);

    }

    /**
     * Genera un JWT para el usuario autenticado.
     * - Incluye los roles del usuario en la sección claims.
     * - Expira después de X tiempo según configuración.
     * - Firma el token con la clave secreta SECRET_KEY usando el algoritmo HS256.
     * @param userDetails
     * @return
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet()); // Cambiamos a Set para evitar duplicados

        claims.put("roles", roles);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                //.expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hora
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * Integer.parseInt(expiration)))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }


    // -------------------------------- PROCESAR, LEER EL TOKEN ------------------
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /*
        Un claim son piezas de información sobre un usuario que se encuentran empaquetadas y firmadas con un token de seguridad
        Método genérico para extraer datos del token:
            - extractClaim() permite extraer cualquier dato del token, como:
            - getSubject() → username
            - getExpiration() → fecha de expiración
            - get("roles") → roles del usuario
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Verifica si el token es válido comparando:
     *
     * Si el username en el token es el mismo que en UserDetails.
     * Si el token no ha expirado.
     * @param token
     * @param userDetails
     * @return
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        // Defensa extra
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        //return !isTokenExpired(token);
    }

    /**
     *  Extrae la fecha de expiración del token y la compara con la fecha actual.
     *  Si la fecha de expiración ya pasó, el token es inválido.
     * @param token
     * @return
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Verifica y decodifica el token JWT.
     * Extrae todos los datos (claims), incluyendo:
     *
     * sub → nombre de usuario
     * roles → lista de roles
     * iat → fecha de emisión
     * exp → fecha de expiración
     * @param token
     * @return
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}


package dev.parallaxsports.user.repository;

import dev.parallaxsports.user.model.User;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.settings
        LEFT JOIN FETCH u.identities
        LEFT JOIN FETCH u.sportSettings
        LEFT JOIN FETCH u.sportFollows
        WHERE u.email = :email
        """)
    Optional<User> findByEmailWithFullProfile(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM User u WHERE u.emailVerified = false AND u.createdAt < :cutoff")
    int deleteUnverifiedUsersBefore(OffsetDateTime cutoff);
}

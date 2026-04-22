package dev.parallaxsports.user.repository;

import dev.parallaxsports.user.model.UserIdentity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    Optional<UserIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);

    Optional<UserIdentity> findByUser_IdAndProvider(Long userId, String provider);
}

package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.Venue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    Optional<Venue> findBySportIdAndNameAndCity(Long sportId, String name, String city);
}

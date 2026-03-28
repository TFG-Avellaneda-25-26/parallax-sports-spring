package dev.parallaxsports.sport.repository;

import dev.parallaxsports.sport.model.Venue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    Optional<Venue> findBySportIdAndNameAndCity(Long sportId, String name, String city);
}

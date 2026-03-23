package dev.parallaxsports.sport.repository;

import dev.parallaxsports.sport.model.Sport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportRepository extends JpaRepository<Sport, Long> {

    Optional<Sport> findByKey(String key);
}

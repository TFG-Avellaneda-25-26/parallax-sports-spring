package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.Sport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportRepository extends JpaRepository<Sport, Long> {

    Optional<Sport> findByKey(String key);
}

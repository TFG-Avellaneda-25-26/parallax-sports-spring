package dev.parallaxsports.loadtest.repository;

import dev.parallaxsports.loadtest.model.LoadTestRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoadTestRunRepository extends JpaRepository<LoadTestRun, Long> {

    Optional<LoadTestRun> findByRunUuid(String runUuid);

    List<LoadTestRun> findByStatus(String status);

    Page<LoadTestRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}

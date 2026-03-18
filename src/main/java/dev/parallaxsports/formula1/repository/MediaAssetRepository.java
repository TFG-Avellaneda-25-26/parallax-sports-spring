package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.MediaAsset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    boolean existsByOwnerTypeAndOwnerIdAndAssetTypeAndUrl(String ownerType, Long ownerId, String assetType, String url);

    List<MediaAsset> findByOwnerTypeAndOwnerIdInAndAssetTypeOrderByOwnerIdAscIdDesc(
        String ownerType,
        List<Long> ownerIds,
        String assetType
    );

    Optional<MediaAsset> findFirstByOwnerTypeAndOwnerIdAndAssetTypeOrderByIdDesc(String ownerType, Long ownerId, String assetType);
}

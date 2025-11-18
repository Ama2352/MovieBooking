package com.api.moviebooking.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.moviebooking.models.entities.RefreshToken;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    /**
     * Count refresh tokens by user ID (for dependency check)
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
}

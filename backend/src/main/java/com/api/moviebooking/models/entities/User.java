package com.api.moviebooking.models.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.api.moviebooking.models.enums.MembershipTier;
import com.api.moviebooking.models.enums.UserRole;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    private String username; // chosen by user or synced from OAuth2
    private String password; // can be null for OAuth2 users
    private String phoneNumber;
    private String provider; // e.g., "google", "facebook"
    private String avatarUrl;
    private String avatarCloudinaryId;
    private int loyaltyPoints;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private MembershipTier membershipTier;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

}

package com.capa.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "access_token", length = 2000, nullable = false)
    private String accessToken;

    @Column(name = "refresh_token", length = 2000)
    private String refreshToken;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
}
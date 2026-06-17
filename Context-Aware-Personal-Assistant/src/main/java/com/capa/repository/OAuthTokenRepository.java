package com.capa.repository;

import com.capa.model.OAuthToken;
import com.capa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {
    Optional<OAuthToken> findByUser(User user);
    Optional<OAuthToken> findByUserEmail(String email);
}
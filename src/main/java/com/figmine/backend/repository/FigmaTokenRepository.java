package com.figmine.backend.repository;

import com.figmine.backend.model.FigmaToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FigmaTokenRepository extends JpaRepository<FigmaToken, Long> {
    void deleteByUser_Id(Long userId);
    java.util.Optional<FigmaToken> findByAccessToken(String accessToken);
    java.util.Optional<FigmaToken> findByRefreshToken(String refreshToken);
    FigmaToken findByUser_Id(Long userId);
}

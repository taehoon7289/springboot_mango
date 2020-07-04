package com.mpresent.mangoboard.common.token;

// import 생략
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpresent.mangoboard.common.dto.user.UserTokenDTO;
import com.mpresent.mangoboard.hibernate.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class JwtTokenProvider { // JWT 토큰을 생성 및 검증 모듈

  @Value("spring.jwt.secret")
  private String secretKey;

  private final long tokenValidMilisecond = 1000L * 60; // 1분만 토큰 유효
  public final String TOKEN_PREFIX = "Bearer ";
  public final String HEADER_STRING = "Authorization";

  @PostConstruct
  protected void init() {
    secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
  }

  // Jwt 토큰 생성
  public String createToken(UserTokenDTO userTokenDTO) {
    Claims claims = Jwts.claims().setSubject(userTokenDTO.getUserNo().toString());
//    claims.put("roles", roles);
    ObjectMapper objectMapper = new ObjectMapper();
    claims.put("data", objectMapper.convertValue(userTokenDTO,Map.class));
    Date now = new Date();
    return Jwts.builder()
            .setClaims(claims) // 데이터
            .setIssuedAt(now) // 토큰 발행일자
            .setExpiration(new Date(now.getTime() + tokenValidMilisecond)) // set Expire Time
            .signWith(SignatureAlgorithm.HS256, secretKey) // 암호화 알고리즘, secret값 세팅
            .compact();
  }

  // Jwt 토큰으로 인증 정보를 조회
//  public Authentication getAuthentication(String token) {
//    UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserNo(token));
//    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
//  }

  // Jwt 토큰에서 회원 구별 정보 추출
  public String getUserNo(String token) {
    return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
  }

  // Request의 Header에서 token 파싱 : "X-AUTH-TOKEN: jwt토큰"
  public String resolveToken(HttpServletRequest req) {
    return req.getHeader("X-AUTH-TOKEN");
  }

  // Jwt 토큰의 유효성 + 만료일자 확인
  public boolean validateToken(String jwtToken) {
    try {
      Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(jwtToken);
      return !claims.getBody().getExpiration().before(new Date());
    } catch (Exception e) {
      return false;
    }
  }

  public UserTokenDTO getData(String token) {
    return (UserTokenDTO) Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().get("data");
  }
}

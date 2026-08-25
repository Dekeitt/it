package com.clean.it.repository;

import com.clean.it.domain.UserAccount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByIssuerAndSubject(String issuer, String subject);
    Optional<UserAccount> findFirstByEmailIgnoreCaseAndIssuer(String email, String issuer);

    @Query("""
      select u from UserAccount u
      where :q='' or lower(coalesce(u.email,'')) like lower(concat('%',:q,'%'))
         or lower(coalesce(u.displayName,'')) like lower(concat('%',:q,'%'))
         or lower(u.subject) like lower(concat('%',:q,'%'))
         or cast(u.id as string)=:q
      order by u.updatedAt desc
      """)
    List<UserAccount> adminSearch(@Param("q") String q, Pageable pageable);
}

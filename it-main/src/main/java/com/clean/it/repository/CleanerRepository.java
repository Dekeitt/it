package com.clean.it.repository;

import com.clean.it.domain.Cleaner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CleanerRepository extends JpaRepository<Cleaner, Long> {
    Optional<Cleaner> findByEmail(String email);
    Optional<Cleaner> findByEmailIgnoreCase(String email);

    @Query("select c from Cleaner c, UserAccount u where c.userId=u.id and c.userId=:userId and u.blockedAt is null")
    Optional<Cleaner> findFirstByUserId(@Param("userId") Long userId);

    @Override
    @Query("select c from Cleaner c, UserAccount u where c.userId=u.id and c.id=:id and u.blockedAt is null")
    Optional<Cleaner> findById(@Param("id") Long id);

    @Query("""
      select c from Cleaner c
      where :q='' or lower(c.email) like lower(concat('%',:q,'%'))
         or lower(coalesce(c.name,'')) like lower(concat('%',:q,'%'))
         or cast(c.id as string)=:q or cast(c.userId as string)=:q
      order by c.createdAt desc
      """)
    List<Cleaner> adminSearch(@Param("q") String q, Pageable pageable);
}

package com.carwash.repository;

import com.carwash.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

    Optional<Member> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** 後台會員清單：關鍵字比對帳號、姓名、電話、Email；關鍵字為空就列出全部 */
    @Query("select m from Member m where :keyword = '' "
            + "or lower(m.username) like lower(concat('%', :keyword, '%')) "
            + "or lower(m.realName) like lower(concat('%', :keyword, '%')) "
            + "or m.phone like concat('%', :keyword, '%') "
            + "or lower(m.email) like lower(concat('%', :keyword, '%'))")
    Page<Member> search(@Param("keyword") String keyword, Pageable pageable);
}

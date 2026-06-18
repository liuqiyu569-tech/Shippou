package com.taskmanagement.repository;

import com.taskmanagement.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 用户数据访问层，提供按用户名查询等方法。
 *
 * @author user-auth
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户。
     *
     * @param username 用户名
     * @return 匹配的用户（可能为空）
     */
    Optional<User> findByUsername(String username);

    /**
     * 判断用户名是否已被注册。
     *
     * @param username 用户名
     * @return 若已存在返回 {@code true}
     */
    boolean existsByUsername(String username);

    /**
     * 根据用户名关键字模糊查询，不区分大小写。
     *
     * @param keyword 关键字
     * @param pageable 分页参数
     * @return 匹配的用户分页
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY u.username ASC")
    Page<User> findByUsernameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);
}

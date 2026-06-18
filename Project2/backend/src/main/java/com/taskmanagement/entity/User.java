package com.taskmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户实体，映射数据库 users 表。
 *
 * <p>存储用户的基本认证信息，密码以 BCrypt 哈希形式存储。</p>
 *
 * @author user-auth
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    /** 用户主键 ID，自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名，唯一，4-20 位字母/数字/下划线。 */
    @Column(nullable = false, length = 20, unique = true)
    private String username;

    /** 密码哈希值（BCrypt 加密），绝不存储明文密码。 */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** 创建时间，由 {@link #onCreate()} 自动设置。 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间，由 {@link #onUpdate()} 自动更新。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 新增记录前自动设置创建时间和更新时间。
     */
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 更新记录前自动刷新更新时间。
     */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

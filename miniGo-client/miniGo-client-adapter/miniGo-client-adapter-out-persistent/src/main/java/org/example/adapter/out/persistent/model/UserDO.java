package org.example.adapter.out.persistent.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("app_user")
public class UserDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String openId;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

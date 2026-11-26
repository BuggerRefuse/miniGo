package org.example.adapter.out.persistent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.adapter.out.persistent.mapper.UserMapper;
import org.example.adapter.out.persistent.model.UserDO;
import org.example.domain.aggregate.User;
import org.example.domain.ports.UserRepository;
import org.example.domain.valueobject.AccountStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserPersistenceAdapter extends ServiceImpl<UserMapper, UserDO>
        implements UserRepository {

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(getById(id)).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserDO record = getOne(
                Wrappers.<UserDO>lambdaQuery().eq(UserDO::getUsername, username),
                false
        );
        return Optional.ofNullable(record).map(this::toDomain);
    }

    @Override
    public Optional<User> findByOpenId(String openId) {
        UserDO record = getOne(
                Wrappers.<UserDO>lambdaQuery().eq(UserDO::getOpenId, openId),
                false
        );
        return Optional.ofNullable(record).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserDO record = toRecord(user);
        if (user.getId() == null) {
            save(record);
            user.assignId(record.getId());
        } else {
            updateById(record);
        }
        return user;
    }

    @Override
    public void deleteById(Long id) {
        removeById(id);
    }

    private User toDomain(UserDO record) {
        return User.restore(
                record.getId(),
                record.getUsername(),
                record.getPasswordHash(),
                record.getOpenId(),
                record.getNickname(),
                record.getAvatarUrl(),
                record.getPhone(),
                record.getEmail(),
                AccountStatus.valueOf(record.getStatus())
        );
    }

    private UserDO toRecord(User user) {
        UserDO record = new UserDO();
        record.setId(user.getId());
        record.setUsername(user.getUsername());
        record.setPasswordHash(user.getEncodedPassword());
        record.setOpenId(user.getOpenId());
        record.setNickname(user.getNickname());
        record.setAvatarUrl(user.getAvatarUrl());
        record.setPhone(user.getPhone());
        record.setEmail(user.getEmail());
        record.setStatus(user.getStatus().name());
        return record;
    }
}

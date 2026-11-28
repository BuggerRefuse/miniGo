package org.example.admin.adapter.out.persistent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.admin.adapter.out.persistent.mapper.AdminMapper;
import org.example.admin.adapter.out.persistent.model.AdminDO;
import org.example.admin.domain.aggregate.Admin;
import org.example.admin.domain.ports.AdminRepository;
import org.example.admin.domain.valueobject.AdminStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminPersistenceAdapter extends ServiceImpl<AdminMapper, AdminDO>
        implements AdminRepository {

    @Override
    public Optional<Admin> findById(Long id) {
        return Optional.ofNullable(getById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Admin> findByUsername(String username) {
        AdminDO record = getOne(
                Wrappers.<AdminDO>lambdaQuery().eq(AdminDO::getUsername, username),
                false
        );
        return Optional.ofNullable(record).map(this::toDomain);
    }

    private Admin toDomain(AdminDO record) {
        return Admin.restore(
                record.getId(),
                record.getUsername(),
                record.getPasswordHash(),
                record.getDisplayName(),
                AdminStatus.valueOf(record.getStatus())
        );
    }
}

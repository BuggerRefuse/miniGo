package org.example.admin.adapter.out.persistent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.admin.adapter.out.persistent.model.AdminDO;

@Mapper
public interface AdminMapper extends BaseMapper<AdminDO> {
}

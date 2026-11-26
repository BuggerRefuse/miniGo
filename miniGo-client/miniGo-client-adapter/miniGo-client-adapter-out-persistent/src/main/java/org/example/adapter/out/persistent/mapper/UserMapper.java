package org.example.adapter.out.persistent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.adapter.out.persistent.model.UserDO;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}

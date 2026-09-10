package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 用户数据访问Mapper接口
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名物理删除已软删除的记录（释放唯一键约束）
     *
     * @param userAccount 用户名
     * @return 删除的记录数
     */
    @Delete("DELETE FROM user WHERE user_account = #{userAccount} AND is_deleted = 1")
    int physicalDeleteByUserAccount(@Param("userAccount") String userAccount);
}





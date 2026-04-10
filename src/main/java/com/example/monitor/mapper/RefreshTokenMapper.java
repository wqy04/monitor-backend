package com.example.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.monitor.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {

    @Select("SELECT * FROM refresh_tokens WHERE token = #{token}")
    RefreshToken selectByToken(String token);

    @Delete("DELETE FROM refresh_tokens WHERE token = #{token}")
    int deleteByToken(String token);

    @Delete("DELETE FROM refresh_tokens WHERE user_id = #{userId}")
    int deleteByUserId(Integer userId);
}
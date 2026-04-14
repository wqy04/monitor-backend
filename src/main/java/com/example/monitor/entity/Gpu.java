package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * GPU 静态信息表实体
 * 对应数据库表：gpus
 */
@Data
@TableName("gpus")
public class Gpu {
    @TableId(type = IdType.AUTO)
    private Integer gpuId;

    private Integer nodeId;

    private Integer gpuIndex;

    private String gpuModel;

    private Long memoryTotal;

    private String status;
}

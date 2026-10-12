package cn.tbzx.cloud.modules.zone.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("safe_zone")
public class SafeZone {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;

    private String zoneName;

    private Double centerLat;

    private Double centerLng;

    private Integer radiusM;

    /** 0=停用 1=启用 */
    private Integer active;

    private LocalDateTime createdAt;
}

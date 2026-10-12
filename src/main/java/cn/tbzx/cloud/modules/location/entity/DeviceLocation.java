package cn.tbzx.cloud.modules.location.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_location")
public class DeviceLocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;

    private Double lat;

    private Double lng;

    private Double accuracy;

    private Double speed;

    /** 所属出行 session.id (轨迹归属键, LocationService 落库时填) */
    private Long sessionId;

    /** 客户端采集时间戳 (去重键 uk_device_point_ts) */
    private LocalDateTime pointTs;

    private LocalDateTime createdAt;
}

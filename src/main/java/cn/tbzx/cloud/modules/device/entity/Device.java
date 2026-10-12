package cn.tbzx.cloud.modules.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备编号 TBZX-XXXXXXXXXXXX */
    private String deviceId;

    private String deviceName;

    /** 只写: 云端只存 SHA-256, 序列化时绝不外泄 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;

    /** null = 未绑定, 可被认领 */
    private Long userId;

    private Integer batteryLevel;

    private LocalDateTime lastHeartbeatAt;

    private LocalDateTime lastPointAt;

    private Double lastLat;

    private Double lastLng;

    private Long currentSessionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

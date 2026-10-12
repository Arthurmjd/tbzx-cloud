package cn.tbzx.cloud.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert")
public class Alert {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;

    private String alertType;

    /** 0=info 1=warning 2=danger 3=sos */
    private Integer severity;

    private String description;

    /** GEOFENCE_EXIT→safe_zone.id; OFFLINE/SOS→session.id */
    private Long refId;

    private Double lat;

    private Double lng;

    private Integer hasImage;

    private LocalDateTime createdAt;
}

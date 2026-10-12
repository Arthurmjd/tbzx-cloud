package cn.tbzx.cloud.modules.session.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("travel_session")
public class TravelSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;

    private LocalDateTime startTime;

    /** null = 进行中 */
    private LocalDateTime endTime;

    private Double startLat;

    private Double startLng;

    private Double endLat;

    private Double endLng;

    private Integer distanceM;

    private Integer alertCount;

    /** 0=进行中 1=已结束 */
    private Integer status;

    private LocalDateTime createdAt;
}

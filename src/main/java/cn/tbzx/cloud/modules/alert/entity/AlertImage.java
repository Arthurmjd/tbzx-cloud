package cn.tbzx.cloud.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_image")
public class AlertImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alertId;

    private String imagePath;

    private String imageUrl;

    private Integer fileSize;

    private LocalDateTime createdAt;
}

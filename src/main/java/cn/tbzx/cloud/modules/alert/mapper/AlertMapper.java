package cn.tbzx.cloud.modules.alert.mapper;

import cn.tbzx.cloud.modules.alert.entity.Alert;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AlertMapper extends BaseMapper<Alert> {

    /** 近 N 秒内同设备同类型同 ref 的告警数 (去重抗抖动用) */
    @Select("SELECT COUNT(*) FROM alert WHERE device_id = #{deviceId} AND alert_type = #{alertType} "
            + "AND ref_id = #{refId} AND created_at > DATE_SUB(NOW(), INTERVAL #{seconds} SECOND)")
    long countRecent(@Param("deviceId") String deviceId,
                     @Param("alertType") String alertType,
                     @Param("refId") Long refId,
                     @Param("seconds") int seconds);
}

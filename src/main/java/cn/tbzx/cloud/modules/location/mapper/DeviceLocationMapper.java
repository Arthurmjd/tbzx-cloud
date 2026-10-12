package cn.tbzx.cloud.modules.location.mapper;

import cn.tbzx.cloud.modules.location.entity.DeviceLocation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceLocationMapper extends BaseMapper<DeviceLocation> {

    /** 幂等去重: 重复点 (device_id+point_ts 冲突) 返回 0, 不产生重复轨迹 */
    @Insert("INSERT IGNORE INTO device_location (device_id, lat, lng, accuracy, speed, session_id, point_ts) "
            + "VALUES (#{deviceId}, #{lat}, #{lng}, #{accuracy}, #{speed}, #{sessionId}, #{pointTs})")
    int insertIgnore(DeviceLocation location);

    /** 家属侧实时轨迹尾巴: 最近 N 个点 (point_ts 降序, 走 uk_device_point_ts 索引) */
    @Select("SELECT id, device_id, lat, lng, accuracy, speed, point_ts, created_at "
            + "FROM device_location WHERE device_id = #{deviceId} "
            + "ORDER BY point_ts DESC LIMIT #{limit}")
    List<DeviceLocation> selectRecentDesc(@Param("deviceId") String deviceId, @Param("limit") int limit);
}

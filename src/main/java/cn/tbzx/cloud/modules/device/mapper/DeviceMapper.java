package cn.tbzx.cloud.modules.device.mapper;

import cn.tbzx.cloud.modules.device.entity.Device;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    @Select("SELECT device_id FROM device WHERE api_key = #{hash} LIMIT 1")
    String selectDeviceIdByApiKeyHash(String hash);
}

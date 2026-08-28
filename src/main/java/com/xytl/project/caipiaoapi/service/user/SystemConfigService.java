package com.xytl.project.caipiaoapi.service.user;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xytl.project.caipiaoapi.domain.systemconfig.SystemConfig;
import com.xytl.project.caipiaoapi.service.piaocoder.PiaoCoderService;


@Service
public class SystemConfigService  {

    private final static String TOKEN_CODE = "caipiao.token";

    @Autowired(required = false)
	private SystemConfigMapper systemConfigMapper;

	public SystemConfig getByCode(String code) {
        if (systemConfigMapper == null) {
            return null;
        }
		LambdaQueryWrapper<SystemConfig> query = Wrappers.lambdaQuery();
		query.eq(SystemConfig::getCode, code);
        return systemConfigMapper.selectOne(query);
	}

	public boolean update(SystemConfig config) {
        if (systemConfigMapper == null || config == null) {
            return false;
        }
	    return systemConfigMapper.updateById(config) > 0;
	}

	public String getValueByCode(String code) {
        SystemConfig config = getByCode(code);
        return config == null ? null : config.getValue();
	}

	public String getValueByCode(String code, String defaultValue) {
	    String val = getValueByCode(code);
	    if(StringUtils.isBlank(val)) {
	        return defaultValue;
	    }
	    return val;
	}

	public String getToken() {
	    return this.getValueByCode(TOKEN_CODE);
	}

	public boolean updateToken(String token) {
        PiaoCoderService.TOKEN_HAVE_ERROR = false;
        if (systemConfigMapper == null) {
            return true;
        }
	    SystemConfig sc = this.getByCode(TOKEN_CODE);
	    if(sc == null) {
	        sc = new SystemConfig();
	        sc.setCode(TOKEN_CODE);
	        sc.setName("彩票平台token");
	        sc.setValue(token);
	        sc.setCreateTime(new Date());
	        systemConfigMapper.insert(sc);
	        return true;
	    }
	    sc.setValue(token);
	    systemConfigMapper.updateById(sc);
	    return true;
	}
}

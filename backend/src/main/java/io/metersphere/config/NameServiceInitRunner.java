package io.metersphere.config;

import io.metersphere.commons.utils.LogUtil;
import io.metersphere.nameservice.CustomNameService;
import io.metersphere.service.SystemParameterService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class NameServiceInitRunner implements ApplicationRunner {

    @Resource
    private SystemParameterService systemParameterService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, String> mapping = systemParameterService.getHostMap();
        if (mapping.size() > 0) {
            CustomNameService.load(mapping);
            LogUtil.info("Load hosts mapping success");
        }
    }
}

package io.metersphere.nameservice;

import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;

public class CustomNameServiceDescriptor implements NameServiceDescriptor {
    @Override
    public NameService createNameService() throws Exception {
        return new CustomNameService();
    }

    @Override
    public String getProviderName() {
        return "custom";
    }

    @Override
    public String getType() {
        return "dns";
    }
}

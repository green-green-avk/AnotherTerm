package com.jcraft.jsch;

import java.util.Map;

public interface Configuration {
    String getConfig(String key);

    void setConfig(String key, String value);

    void setConfig(Map<String, String> newConf);
}

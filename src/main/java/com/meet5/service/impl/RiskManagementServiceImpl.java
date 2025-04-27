package com.meet5.service.impl;

import com.meet5.service.RiskManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisNoScriptException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.meet5.common.constants.Constants.EXPIRE_SECONDS;
import static com.meet5.common.constants.Constants.LIMIT_COUNT;


@Service
public class RiskManagementServiceImpl implements RiskManagementService {

    private final JedisPooled jedisPooled;

    @Autowired
    public RiskManagementServiceImpl(JedisPooled jedisPooled) {
        this.jedisPooled = jedisPooled;
        this.scriptSha = jedisPooled.scriptLoad(LUA_COUNTER);
    }

    private static final String USER_OPERATION_PREFIX = "user:operation:";


    private static final String LUA_COUNTER =
            //          KEYS[1]            ARGV[1]=expireSec      ARGV[2]=limit
            "local cnt = redis.call('INCR', KEYS[1]);"
                    +
                    "if cnt == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])); end;"
                    +
                    "if cnt >= tonumber(ARGV[2]) then return 1 else return 0 end;";

    private volatile String scriptSha;


    @Override
    public boolean checkSensitiveBehavior(Long userId) {
        String key = USER_OPERATION_PREFIX + userId;
        List<String> keys = Collections.singletonList(key);
        List<String> args = Arrays.asList(
                String.valueOf(EXPIRE_SECONDS),
                String.valueOf(LIMIT_COUNT));

        Object result;
        try {
            result = jedisPooled.evalsha(scriptSha, keys, args);
        } catch (JedisNoScriptException e) {
            scriptSha = jedisPooled.scriptLoad(LUA_COUNTER);
            result = jedisPooled.evalsha(scriptSha, keys, args);
        }

        return Long.valueOf(1).equals(result);
    }
}

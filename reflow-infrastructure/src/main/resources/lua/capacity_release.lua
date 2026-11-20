-- KEYS[1] = cap:{factoryId}:{lineId}:{bizDate}
-- ARGV[1] = 释放数量
-- 返回：释放后的 used 值
--
-- 释放必须用 Lua 而不是 GET+SET：并发释放两条 hold 时，
-- GET 到的旧值会被后写的覆盖，导致 used 少减，产能被永久锁死。
local used = tonumber(redis.call('HGET', KEYS[1], 'used'))
local qty = tonumber(ARGV[1])

if used == nil then
    return 0
end

local after = used - qty
if after < 0 then
    -- 兜底：DB 与 Redis 不一致时不允许出现负数
    after = 0
end
redis.call('HSET', KEYS[1], 'used', after)
return after
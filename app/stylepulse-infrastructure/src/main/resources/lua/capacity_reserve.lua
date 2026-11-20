-- KEYS[1] = cap:{factoryId}:{lineId}:{bizDate}
-- ARGV[1] = 申请数量
-- 返回：1 成功 / 0 余量不足 / -1 槽位未初始化
--
-- 关键点：读 total、读 used、判断、自增，这四步必须在一个脚本里。
-- 拆成多条命令就会变成 check-then-act：两个请求同时读到 used=100、total=150，
-- 都判断"还剩 50"，于是各自扣 40，最终 used=180 —— 超占 30 件，工厂交不出来。
local total = tonumber(redis.call('HGET', KEYS[1], 'total'))
if total == nil then
    return -1
end

local used = tonumber(redis.call('HGET', KEYS[1], 'used'))
if used == nil then
    used = 0
end

local qty = tonumber(ARGV[1])
if qty == nil or qty <= 0 then
    return 0
end

if used + qty > total then
    return 0
end

redis.call('HINCRBY', KEYS[1], 'used', qty)
return 1
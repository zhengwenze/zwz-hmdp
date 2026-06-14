
-- KEYS[1]: 锁的key
-- ARGV[1]: 线程标识
-- 仅当锁属于当前线程时才释放，防止误删他人锁
local lockValue = redis.call('GET', KEYS[1])
if lockValue and lockValue == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0

---秒杀券id
local voucherId = ARGV[1]
--用户id
local userId = ARGV[2]
--订单id
local id = ARGV[3]

--统一哈希key存储秒杀信息：库存 + 用户下单状态
local seckillKey = 'seckill:' .. voucherId
local userField = 'user:' .. userId

--一次性获取库存和用户下单状态，减少网络往返
local result = redis.call('HMGET', seckillKey, 'stock', userField)
local stock = tonumber(result[1])
local hasOrdered = tonumber(result[2])

--库存不足
if not stock or stock <= 0 then
    return 1
end

--用户已下单
if hasOrdered == 1 then
    return 2
end

--使用HSETNX原子性设置用户下单状态，防止并发重复下单
local success = redis.call('HSETNX', seckillKey, userField, 1)
if success == 0 then
    return 2
end

--扣减库存
redis.call('HINCRBY', seckillKey, 'stock', -1)
--发送消息
redis.call('XADD', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', id)
return 0

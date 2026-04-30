package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 自己注入自己为了获取代理对象 @Lazy 延迟注入 避免形成循环依赖
     */
    @Resource
    @Lazy
    private IVoucherOrderService voucherOrderService;
    @Resource(name = "seckillOrderExecutor")
    private ThreadPoolTaskExecutor seckillOrderExecutor;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    private static final String QUEUE_NAME = "stream.orders";
    private static final String GROUP_NAME = "g1";
    private static final String CONSUMER_NAME = "c1";
    private volatile boolean running = true;

    @PostConstruct
    private void init() {
        try {
            ensureOrderStreamReady();
            preloadSeckillStockCache();
            seckillOrderExecutor.execute(this::consumeOrders);
        } catch (Exception e) {
            log.error("Failed to initialize seckill order infrastructure: queue={}, group={}, consumer={}",
                    QUEUE_NAME,
                    GROUP_NAME,
                    CONSUMER_NAME,
                    e);
            throw new IllegalStateException("Failed to initialize seckill order infrastructure", e);
        }
    }

    @PreDestroy
    private void shutdown() {
        running = false;
        log.info("Stopping seckill order consumer: queue={}, group={}, consumer={}", QUEUE_NAME, GROUP_NAME, CONSUMER_NAME);
    }

    private void consumeOrders() {
        log.info("Started seckill order consumer: thread={}, queue={}, group={}, consumer={}",
                Thread.currentThread().getName(),
                QUEUE_NAME,
                GROUP_NAME,
                CONSUMER_NAME);
        while (running) {
            try {
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create(QUEUE_NAME, ReadOffset.lastConsumed())
                );
                if (list == null || list.isEmpty()) {
                    continue;
                }
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> values = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                handleVoucherOrder(voucherOrder);
                stringRedisTemplate.opsForStream().acknowledge(QUEUE_NAME, GROUP_NAME, record.getId());
            } catch (Exception e) {
                if (!running) {
                    break;
                }
                log.error("Seckill order consumer loop failed: queue={}, group={}, consumer={}",
                        QUEUE_NAME,
                        GROUP_NAME,
                        CONSUMER_NAME,
                        e);
                handlePendingList();
            }
        }
        log.info("Stopped seckill order consumer: thread={}, queue={}, group={}, consumer={}",
                Thread.currentThread().getName(),
                QUEUE_NAME,
                GROUP_NAME,
                CONSUMER_NAME);
    }

    private void handlePendingList() {
        while (running){
            try {
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(QUEUE_NAME, ReadOffset.from("0"))
                );
                if (list==null||list.isEmpty()){
                    break;
                }
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> values = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                handleVoucherOrder(voucherOrder);
                stringRedisTemplate.opsForStream().acknowledge(QUEUE_NAME, GROUP_NAME, record.getId());
            } catch (Exception e) {
                if (!running) {
                    break;
                }
                log.error("Pending list recovery failed: queue={}, group={}, consumer={}",
                        QUEUE_NAME,
                        GROUP_NAME,
                        CONSUMER_NAME,
                        e);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("Pending list recovery interrupted: queue={}, group={}, consumer={}",
                            QUEUE_NAME,
                            GROUP_NAME,
                            CONSUMER_NAME,
                            ex);
                    break;
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        //创建锁对象（兜底）
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //获取锁
        boolean isLock = lock.tryLock();
        //判断是否获取锁成功
        if (!isLock) {
            log.warn("Failed to acquire voucher order lock: userId={}, voucherId={}, orderId={}",
                    userId,
                    voucherOrder.getVoucherId(),
                    voucherOrder.getId());
            throw new IllegalStateException("Failed to acquire voucher order lock");
        }
        try {
            voucherOrderService.createVoucherOrder(voucherOrder);
        } catch (DuplicateKeyException e) {
            log.warn("Duplicate voucher order ignored during async consume: userId={}, voucherId={}, orderId={}",
                    userId,
                    voucherOrder.getVoucherId(),
                    voucherOrder.getId(),
                    e);
        } finally {
            //释放锁
            lock.unlock();
        }

    }

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 秒杀优惠券(消息队列)
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("秒杀券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (seckillVoucher.getBeginTime() != null && seckillVoucher.getBeginTime().isAfter(now)) {
            return Result.fail("秒杀尚未开始");
        }
        if (seckillVoucher.getEndTime() != null && seckillVoucher.getEndTime().isBefore(now)) {
            return Result.fail("秒杀已经结束");
        }
        //获取用户
        UserDTO user = UserHolder.getUser();
        //获取订单id
        Long orderId = redisIdWorker.nextId("order");
        //执行lua脚本
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT
                , Collections.emptyList()
                , voucherId.toString()
                , user.getId().toString()
                , orderId.toString());
        if (res == null) {
            log.error("Seckill script returned null: voucherId={}, userId={}, orderId={}",
                    voucherId,
                    user.getId(),
                    orderId);
            return Result.fail("秒杀下单失败，请稍后重试");
        }
        //判断结果是否为0
        int r = res.intValue();
        if (r != 0) {
            //不为0 没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "禁止重复下单");
        }
        return Result.ok(orderId);
    }
    /**
     * 秒杀优惠券(异步)
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    /*@Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户
        UserDTO user = UserHolder.getUser();
        //执行lua脚本
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT
                , Collections.emptyList()
                , voucherId.toString()
                ,user.getId().toString());
        //判断结果是否为0
        int r=res.intValue();
        if (r!=0){
            //不为0 没有购买资格
            return Result.fail(r==1?"库存不足":"禁止重复下单");
        }
        //为0有购买资格
        Long orderId = redisIdWorker.nextId("order");
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setId(orderId);
        //存入阻塞队列
        orderTasks.add(voucherOrder);
        //返回订单id
        return Result.ok(orderId);
    }*/

    /**
     * 秒杀优惠券
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    /*@Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //秒杀尚未开始
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //秒杀已经结束
            return Result.fail("秒杀已经结束");
        }
        //判断库存是否充足
        if (voucher.getStock() < 1) {
            //库存不足
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        //仅限单体应用使用
//        synchronized (userId.toString().intern()) {
//            //实现获取代理对象 比较复杂 我采用了自己注入自己的方式
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return voucherOrderService.getResult(voucherId);
//        }
        //创建锁对象
//        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //获取锁
//        boolean isLock = simpleRedisLock.tryLock(1200L);
        boolean isLock = lock.tryLock();
        //判断是否获取锁成功
        if (!isLock){
            //获取失败,返回错误或者重试
            return Result.fail("一人一单哦！");
        }
        try {
            return voucherOrderService.getResult(voucherId);
        } finally {
            //释放锁
            lock.unlock();
        }
    }*/
    @Override
    @NotNull
    @Transactional(rollbackFor = Exception.class)
    public Result getResult(Long voucherId) {
        //是否下单
        Long userId = UserHolder.getUser().getId();
        Long count = lambdaQuery()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, userId)
                .count();
        if (count > 0) {
            return Result.fail("禁止重复购买");
        }
        //扣减库存
        boolean isSuccess = seckillVoucherService.update(
                new LambdaUpdateWrapper<SeckillVoucher>()
                        .eq(SeckillVoucher::getVoucherId, voucherId)
                        .gt(SeckillVoucher::getStock, 0)
                        .setSql("stock=stock-1"));
        if (!isSuccess) {
            //库存不足
            return Result.fail("库存不足");
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        Long orderId = redisIdWorker.nextId("order");
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setId(orderId);
        this.save(voucherOrder);
        //返回订单id
        return Result.ok(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        Long count = lambdaQuery()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, userId)
                .count();
        if (count > 0) {
            log.info("Duplicate voucher order skipped: userId={}, voucherId={}, orderId={}",
                    userId,
                    voucherId,
                    voucherOrder.getId());
            return;
        }
        //扣减库存
        boolean isSuccess = seckillVoucherService.update(
                new LambdaUpdateWrapper<SeckillVoucher>()
                        .eq(SeckillVoucher::getVoucherId, voucherId)
                        .gt(SeckillVoucher::getStock, 0)
                        .setSql("stock=stock-1"));
        if (!isSuccess) {
            log.warn("Failed to deduct seckill voucher stock: userId={}, voucherId={}, orderId={}",
                    userId,
                    voucherId,
                    voucherOrder.getId());
            return;
        }
        //创建订单
        boolean saved = this.save(voucherOrder);
        if (!saved) {
            throw new IllegalStateException("Failed to save voucher order");
        }
    }

    private void ensureOrderStreamReady() {
        try {
            Boolean streamExists = stringRedisTemplate.hasKey(QUEUE_NAME);
            if (!Boolean.TRUE.equals(streamExists)) {
                stringRedisTemplate.opsForStream().add(QUEUE_NAME, Collections.singletonMap("bootstrap", "0"));
                log.info("Created seckill order stream key: queue={}", QUEUE_NAME);
            }
            stringRedisTemplate.opsForStream().createGroup(QUEUE_NAME, ReadOffset.latest(), GROUP_NAME);
            log.info("Created seckill order consumer group: queue={}, group={}", QUEUE_NAME, GROUP_NAME);
        } catch (Exception e) {
            if (isBusyGroupException(e)) {
                log.info("Seckill order consumer group already exists: queue={}, group={}", QUEUE_NAME, GROUP_NAME);
                return;
            }
            log.error("Failed to prepare seckill order stream: queue={}, group={}", QUEUE_NAME, GROUP_NAME, e);
            throw e;
        }
    }

    private void preloadSeckillStockCache() {
        List<SeckillVoucher> vouchers = seckillVoucherService.list(
                new LambdaQueryWrapper<SeckillVoucher>()
                        .select(SeckillVoucher::getVoucherId, SeckillVoucher::getStock));
        for (SeckillVoucher voucher : vouchers) {
            String stockKey = SECKILL_STOCK_KEY + voucher.getVoucherId();
            Boolean initialized = stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(voucher.getStock()));
            if (Boolean.TRUE.equals(initialized)) {
                log.info("Preloaded missing seckill stock cache: voucherId={}, stock={}",
                        voucher.getVoucherId(),
                        voucher.getStock());
            }
        }
    }

    private boolean isBusyGroupException(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}

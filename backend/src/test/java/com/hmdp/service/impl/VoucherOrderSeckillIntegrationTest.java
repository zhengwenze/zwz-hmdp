package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.HmDianPingApplication;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = HmDianPingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "rag.enabled=false")
class VoucherOrderSeckillIntegrationTest {

    private static final String STREAM_KEY = "stream.orders";

    @Autowired
    private VoucherOrderServiceImpl voucherOrderService;

    @Autowired
    private IVoucherService voucherService;

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private final List<Long> voucherIdsToCleanup = new ArrayList<>();

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
        for (Long voucherId : voucherIdsToCleanup) {
            voucherOrderService.remove(new LambdaQueryWrapper<VoucherOrder>()
                    .eq(VoucherOrder::getVoucherId, voucherId));
            seckillVoucherService.removeById(voucherId);
            voucherService.removeById(voucherId);
            stringRedisTemplate.delete(RedisConstants.SECKILL_STOCK_KEY + voucherId);
            stringRedisTemplate.delete("seckill:order:" + voucherId);
        }
        voucherIdsToCleanup.clear();
    }

    @Test
    void seckillVoucherShouldCreateOrderAndDeductStock() throws Exception {
        Long voucherId = createSeckillVoucher(3, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        saveUser(910001L);

        Result result = voucherOrderService.seckillVoucher(voucherId);

        assertTrue(Boolean.TRUE.equals(result.getSuccess()));
        Long orderId = ((Number) result.getData()).longValue();
        VoucherOrder order = waitForOrder(voucherId, 910001L);
        assertNotNull(order);
        assertEquals(orderId, order.getId());
        assertEquals(2, seckillVoucherService.getById(voucherId).getStock());
        assertEquals("2", stringRedisTemplate.opsForValue().get(RedisConstants.SECKILL_STOCK_KEY + voucherId));
    }

    @Test
    void seckillVoucherShouldRejectDuplicateOrder() throws Exception {
        Long voucherId = createSeckillVoucher(3, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        saveUser(910002L);

        Result first = voucherOrderService.seckillVoucher(voucherId);
        assertTrue(Boolean.TRUE.equals(first.getSuccess()));
        waitForOrder(voucherId, 910002L);

        Result second = voucherOrderService.seckillVoucher(voucherId);

        assertFalse(Boolean.TRUE.equals(second.getSuccess()));
        assertEquals("禁止重复下单", second.getErrorMsg());
        assertEquals(1L, voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, 910002L)));
        assertEquals(2, seckillVoucherService.getById(voucherId).getStock());
    }

    @Test
    void seckillVoucherShouldRejectWhenStockIsEmpty() {
        Long voucherId = createSeckillVoucher(0, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        saveUser(910003L);

        Result result = voucherOrderService.seckillVoucher(voucherId);

        assertFalse(Boolean.TRUE.equals(result.getSuccess()));
        assertEquals("库存不足", result.getErrorMsg());
        assertEquals(0L, voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, 910003L)));
        assertEquals(0, seckillVoucherService.getById(voucherId).getStock());
    }

    @Test
    void seckillVoucherShouldRejectBeforeBeginTimeWithoutWritingStream() {
        Long voucherId = createSeckillVoucher(3, LocalDateTime.now().plusMinutes(10), LocalDateTime.now().plusMinutes(30));
        saveUser(910004L);
        Long streamSizeBefore = stringRedisTemplate.opsForStream().size(STREAM_KEY);

        Result result = voucherOrderService.seckillVoucher(voucherId);

        assertFalse(Boolean.TRUE.equals(result.getSuccess()));
        assertEquals("秒杀尚未开始", result.getErrorMsg());
        assertEquals(streamSizeBefore, stringRedisTemplate.opsForStream().size(STREAM_KEY));
        assertEquals(0L, voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, 910004L)));
    }

    @Test
    void seckillVoucherShouldRejectAfterEndTimeWithoutWritingStream() {
        Long voucherId = createSeckillVoucher(3, LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(1));
        saveUser(910005L);
        Long streamSizeBefore = stringRedisTemplate.opsForStream().size(STREAM_KEY);

        Result result = voucherOrderService.seckillVoucher(voucherId);

        assertFalse(Boolean.TRUE.equals(result.getSuccess()));
        assertEquals("秒杀已经结束", result.getErrorMsg());
        assertEquals(streamSizeBefore, stringRedisTemplate.opsForStream().size(STREAM_KEY));
        assertEquals(0L, voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, 910005L)));
    }

    @Test
    void shouldRecreateMissingStockCacheThroughPreloadHook() {
        Long voucherId = createSeckillVoucher(5, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        stringRedisTemplate.delete(RedisConstants.SECKILL_STOCK_KEY + voucherId);

        ReflectionTestUtils.invokeMethod(voucherOrderService, "preloadSeckillStockCache");

        assertEquals("5", stringRedisTemplate.opsForValue().get(RedisConstants.SECKILL_STOCK_KEY + voucherId));
    }

    @Test
    void shouldKeepStreamInfrastructureUsableWithoutRedisInit() {
        ReflectionTestUtils.invokeMethod(voucherOrderService, "ensureOrderStreamReady");

        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(STREAM_KEY)));
    }

    @Test
    void createVoucherOrderShouldBeSafeForRepeatedConsumption() {
        Long voucherId = createSeckillVoucher(3, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));

        VoucherOrder first = new VoucherOrder();
        first.setId(Math.abs(System.nanoTime()));
        first.setUserId(910006L);
        first.setVoucherId(voucherId);
        voucherOrderService.createVoucherOrder(first);

        VoucherOrder repeated = new VoucherOrder();
        repeated.setId(Math.abs(System.nanoTime()) + 1);
        repeated.setUserId(910006L);
        repeated.setVoucherId(voucherId);
        voucherOrderService.createVoucherOrder(repeated);

        assertEquals(1L, voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, 910006L)));
        assertEquals(2, seckillVoucherService.getById(voucherId).getStock());
    }

    private Long createSeckillVoucher(int stock, LocalDateTime beginTime, LocalDateTime endTime) {
        Voucher voucher = new Voucher();
        voucher.setShopId(1L);
        voucher.setTitle("test-integration-" + System.nanoTime());
        voucher.setSubTitle("test");
        voucher.setRules("test rules");
        voucher.setPayValue(100L);
        voucher.setActualValue(200L);
        voucher.setType(1);
        voucher.setStatus(1);
        voucher.setStock(stock);
        voucher.setBeginTime(beginTime);
        voucher.setEndTime(endTime);
        voucherService.addSeckillVoucher(voucher);
        voucherIdsToCleanup.add(voucher.getId());
        return voucher.getId();
    }

    private void saveUser(Long userId) {
        UserDTO user = new UserDTO();
        user.setId(userId);
        user.setNickName("test-user-" + userId);
        UserHolder.saveUser(user);
    }

    private VoucherOrder waitForOrder(Long voucherId, Long userId) throws Exception {
        for (int i = 0; i < 50; i++) {
            VoucherOrder order = voucherOrderService.getOne(new LambdaQueryWrapper<VoucherOrder>()
                    .eq(VoucherOrder::getVoucherId, voucherId)
                    .eq(VoucherOrder::getUserId, userId));
            if (order != null) {
                return order;
            }
            Thread.sleep(100);
        }
        return null;
    }
}

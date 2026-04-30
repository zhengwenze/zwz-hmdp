package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.HmDianPingApplication;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = HmDianPingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "rag.enabled=false")
class VoucherOrderServiceImplTest {

    @Autowired
    private VoucherOrderServiceImpl voucherOrderService;

    @Autowired
    private IVoucherService voucherService;

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    private final List<Long> voucherIdsToCleanup = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (Long voucherId : voucherIdsToCleanup) {
            voucherOrderService.remove(new LambdaQueryWrapper<VoucherOrder>()
                    .eq(VoucherOrder::getVoucherId, voucherId));
            seckillVoucherService.removeById(voucherId);
            voucherService.removeById(voucherId);
        }
        voucherIdsToCleanup.clear();
    }

    @Test
    void createVoucherOrderShouldPersistOrderAndDeductStock() {
        Long voucherId = createSeckillVoucher(3, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));

        VoucherOrder order = new VoucherOrder();
        order.setId(redisIdWorker.nextId("test-order"));
        order.setUserId(900001L);
        order.setVoucherId(voucherId);

        voucherOrderService.createVoucherOrder(order);

        long orderCount = voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, 900001L));
        Integer remainingStock = seckillVoucherService.getById(voucherId).getStock();

        assertEquals(1L, orderCount);
        assertEquals(2, remainingStock);
    }

    @Test
    void createVoucherOrderShouldBeIdempotentForDuplicateMessage() {
        Long voucherId = createSeckillVoucher(3, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));

        VoucherOrder firstOrder = new VoucherOrder();
        firstOrder.setId(redisIdWorker.nextId("test-order"));
        firstOrder.setUserId(900002L);
        firstOrder.setVoucherId(voucherId);
        voucherOrderService.createVoucherOrder(firstOrder);

        VoucherOrder duplicateOrder = new VoucherOrder();
        duplicateOrder.setId(redisIdWorker.nextId("test-order"));
        duplicateOrder.setUserId(900002L);
        duplicateOrder.setVoucherId(voucherId);
        voucherOrderService.createVoucherOrder(duplicateOrder);

        long orderCount = voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, 900002L));
        Integer remainingStock = seckillVoucherService.getById(voucherId).getStock();

        assertEquals(1L, orderCount);
        assertEquals(2, remainingStock);
    }

    @Test
    void createVoucherOrderShouldNotPersistOrderWhenStockIsInsufficient() {
        Long voucherId = createSeckillVoucher(0, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));

        VoucherOrder order = new VoucherOrder();
        order.setId(redisIdWorker.nextId("test-order"));
        order.setUserId(900003L);
        order.setVoucherId(voucherId);

        voucherOrderService.createVoucherOrder(order);

        long orderCount = voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, 900003L));
        Integer remainingStock = seckillVoucherService.getById(voucherId).getStock();

        assertEquals(0L, orderCount);
        assertEquals(0, remainingStock);
    }

    private Long createSeckillVoucher(int stock, LocalDateTime beginTime, LocalDateTime endTime) {
        Voucher voucher = new Voucher();
        voucher.setShopId(1L);
        voucher.setTitle("test-seckill-" + System.nanoTime());
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
}

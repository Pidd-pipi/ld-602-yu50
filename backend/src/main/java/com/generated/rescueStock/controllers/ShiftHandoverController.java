package com.generated.rescueStock.controllers;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.generated.rescueStock.constructors.ShiftHandoverDtoFactory;
import com.generated.rescueStock.routes.ShiftHandoverRoutes;
import com.generated.rescueStock.services.ShiftHandoverException;
import com.generated.rescueStock.services.ShiftHandoverService;
import com.generated.rescueStock.types.ClaimShiftHandoverPayload;
import com.generated.rescueStock.types.CloseShiftHandoverPayload;
import com.generated.rescueStock.types.CreateShiftHandoverPayload;
import com.generated.rescueStock.types.ProcessHandoverItemPayload;

/**
 * 值班交接控制器：所有失败分支在此处再包装一次，返回 4xx + {code,message}，
 * 冲突/已领取等错误不会进入 service 写入逻辑，原交接单与库存保持不变。
 */
@RestController
@RequestMapping(ShiftHandoverRoutes.PATH)
public class ShiftHandoverController {
  private final ShiftHandoverService service;

  public ShiftHandoverController(ShiftHandoverService service) {
    this.service = service;
  }

  /** 交接内容重开页面后按仓库回看：GET /api/shift-handover?warehouse_id=1 */
  @GetMapping
  public List<Map<String, Object>> list(@RequestParam(name = "warehouse_id", required = false) Long warehouseId) {
    return service.list(warehouseId);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> detail(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(service.detail(id));
    } catch (ShiftHandoverException ex) {
      return wrap(ex);
    }
  }

  /** 选当班负责人和时段、逐条登记事项与接手人；同仓重叠时段拒绝。 */
  @PostMapping
  public ResponseEntity<?> create(@RequestBody CreateShiftHandoverPayload payload) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(service.create(payload));
    } catch (ShiftHandoverException ex) {
      return wrap(ex);
    }
  }

  /** 新值班员领取；已领取/已关闭时返回 409 与原因。 */
  @PostMapping("/{id}/claim")
  public ResponseEntity<?> claim(@PathVariable Long id, @RequestBody(required = false) ClaimShiftHandoverPayload payload) {
    try {
      return ResponseEntity.ok(service.claim(id, payload));
    } catch (ShiftHandoverException ex) {
      return wrap(ex);
    }
  }

  /** 逐项处理事项。 */
  @PutMapping("/{id}/items/{itemId}")
  public ResponseEntity<?> processItem(@PathVariable Long id, @PathVariable Long itemId,
                                       @RequestBody(required = false) ProcessHandoverItemPayload payload) {
    try {
      return ResponseEntity.ok(service.processItem(id, itemId, payload));
    } catch (ShiftHandoverException ex) {
      return wrap(ex);
    }
  }

  /** 全部事项处理完成后关闭。 */
  @PostMapping("/{id}/close")
  public ResponseEntity<?> close(@PathVariable Long id, @RequestBody(required = false) CloseShiftHandoverPayload payload) {
    try {
      return ResponseEntity.ok(service.close(id, payload));
    } catch (ShiftHandoverException ex) {
      return wrap(ex);
    }
  }

  /** 控制器层二次包装：冲突/状态错误 409，校验错误 400，未找到 404。 */
  private ResponseEntity<Map<String, Object>> wrap(ShiftHandoverException ex) {
    HttpStatus status = switch (ex.getCode()) {
      case "HANDOVER_NOT_FOUND", "HANDOVER_ITEM_NOT_FOUND" -> HttpStatus.NOT_FOUND;
      case "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;
      default -> HttpStatus.CONFLICT;
    };
    return ResponseEntity.status(status).body(ShiftHandoverDtoFactory.errorDto(ex.getCode(), ex.getMessage()));
  }
}

package com.generated.rescueStock.controllers;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.generated.rescueStock.constructors.ShiftHandoverDtoFactory;
import com.generated.rescueStock.models.HandoverSheet;
import com.generated.rescueStock.services.HandoverBusinessException;
import com.generated.rescueStock.services.ShiftHandoverService;
import com.generated.rescueStock.types.HandoverClaimPayload;
import com.generated.rescueStock.types.HandoverCompletePayload;
import com.generated.rescueStock.types.HandoverCreatePayload;

@RestController
@RequestMapping("/api/shift-handover")
public class ShiftHandoverController {

  private static final Logger audit = LoggerFactory.getLogger("shift-handover-audit");

  private final ShiftHandoverService service;

  public ShiftHandoverController(ShiftHandoverService service) {
    this.service = service;
  }

  @GetMapping
  public List<Map<String, Object>> list(@RequestParam(name = "warehouseId", required = false) Long warehouseId) {
    return service.list(warehouseId).stream().map(ShiftHandoverDtoFactory::sheet).toList();
  }

  @PostMapping
  public Map<String, Object> create(@RequestBody HandoverCreatePayload payload) {
    try {
      HandoverSheet sheet = service.create(payload);
      audit.info("{} warehouse={} id={}", service.logTemplate("create"), sheet.warehouseId, sheet.id);
      return ShiftHandoverDtoFactory.sheet(sheet);
    } catch (HandoverBusinessException ex) {
      // controller 层二次包装，错误码与现场参数透传给全局处理器
      throw new HandoverControllerException(ex);
    }
  }

  @PostMapping("/{id}/claim")
  public Map<String, Object> claim(@PathVariable Long id, @RequestBody HandoverClaimPayload payload) {
    try {
      HandoverSheet sheet = service.claim(id, payload.claimedBy());
      audit.info("{} id={} by={}", service.logTemplate("claim"), id, sheet.claimedBy);
      return ShiftHandoverDtoFactory.sheet(sheet);
    } catch (HandoverBusinessException ex) {
      throw new HandoverControllerException(ex);
    }
  }

  @PostMapping("/{id}/items/{itemId}/complete")
  public Map<String, Object> completeItem(
      @PathVariable Long id,
      @PathVariable Long itemId,
      @RequestBody HandoverCompletePayload payload) {
    try {
      HandoverSheet sheet = service.completeItem(id, itemId, payload.resultNote());
      audit.info("{} id={} item={}", service.logTemplate("item"), id, itemId);
      return ShiftHandoverDtoFactory.sheet(sheet);
    } catch (HandoverBusinessException ex) {
      throw new HandoverControllerException(ex);
    }
  }

  @PostMapping("/{id}/close")
  public Map<String, Object> close(@PathVariable Long id) {
    try {
      HandoverSheet sheet = service.close(id);
      audit.info("{} id={}", service.logTemplate("close"), id);
      return ShiftHandoverDtoFactory.sheet(sheet);
    } catch (HandoverBusinessException ex) {
      throw new HandoverControllerException(ex);
    }
  }
}

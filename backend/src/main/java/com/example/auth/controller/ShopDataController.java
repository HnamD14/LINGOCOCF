package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user/shop")
@RequiredArgsConstructor
public class ShopDataController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getShopData(@AuthenticationPrincipal UserDetails ud) {
        try {
            User user = userRepository.findByUsername(ud.getUsername()).orElseThrow();
            Map<String, Object> data = new HashMap<>();
            data.put("coins",         user.getCoins()         != null ? user.getCoins()         : 0);
            data.put("shopOwned",     user.getShopOwned()     != null ? user.getShopOwned()     : "[]");
            data.put("shopEquipped",  user.getShopEquipped()  != null ? user.getShopEquipped()  : "default");
            data.put("shopInventory", user.getShopInventory() != null ? user.getShopInventory() : "[]");
            data.put("spinCount",     user.getSpinCount()     != null ? user.getSpinCount()     : 0);
            data.put("spinDate",      user.getSpinDate()      != null ? user.getSpinDate()      : "");
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.error("getShopData error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "data", new HashMap<>()));
        }
    }

    @PostMapping
    public ResponseEntity<?> saveShopData(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String, Object> body) {
        try {
            User user = userRepository.findByUsername(ud.getUsername()).orElseThrow();
            if (body.containsKey("coins"))         user.setCoins(((Number) body.get("coins")).intValue());
            if (body.containsKey("shopOwned"))     user.setShopOwned((String) body.get("shopOwned"));
            if (body.containsKey("shopEquipped"))  user.setShopEquipped((String) body.get("shopEquipped"));
            if (body.containsKey("shopInventory")) user.setShopInventory((String) body.get("shopInventory"));
            if (body.containsKey("spinCount"))     user.setSpinCount(((Number) body.get("spinCount")).intValue());
            if (body.containsKey("spinDate"))      user.setSpinDate((String) body.get("spinDate"));
            userRepository.save(user);
            log.info("✅ Shop saved for user: {}", ud.getUsername());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("saveShopData error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false));
        }
    }
}

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

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user/shop")
@RequiredArgsConstructor
public class ShopDataController {

    private final UserRepository userRepository;

    /** Lấy toàn bộ shop data của user */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String,Object>>> getShopData(
            @AuthenticationPrincipal UserDetails ud) {
        User user = userRepository.findByUsername(ud.getUsername()).orElseThrow();
        Map<String,Object> data = Map.of(
            "coins",        user.getCoins()        != null ? user.getCoins()        : 0,
            "shopOwned",    user.getShopOwned()    != null ? user.getShopOwned()    : "[]",
            "shopEquipped", user.getShopEquipped() != null ? user.getShopEquipped() : "default",
            "shopInventory",user.getShopInventory()!= null ? user.getShopInventory(): "[]",
            "spinCount",    user.getSpinCount()    != null ? user.getSpinCount()    : 0,
            "spinDate",     user.getSpinDate()     != null ? user.getSpinDate()     : ""
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** Lưu toàn bộ shop data */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveShopData(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String,Object> body) {
        User user = userRepository.findByUsername(ud.getUsername()).orElseThrow();

        if (body.containsKey("coins"))
            user.setCoins(((Number) body.get("coins")).intValue());
        if (body.containsKey("shopOwned"))
            user.setShopOwned((String) body.get("shopOwned"));
        if (body.containsKey("shopEquipped"))
            user.setShopEquipped((String) body.get("shopEquipped"));
        if (body.containsKey("shopInventory"))
            user.setShopInventory((String) body.get("shopInventory"));
        if (body.containsKey("spinCount"))
            user.setSpinCount(((Number) body.get("spinCount")).intValue());
        if (body.containsKey("spinDate"))
            user.setSpinDate((String) body.get("spinDate"));

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

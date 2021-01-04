package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public String addCart(Cart cart){

        this.cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addToCart.html?skuId=" + cart.getSkuId();
    }

    @GetMapping("addToCart.html")
    public String addToCart(@RequestParam("skuId")Long skuId, Model model){

        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart", cart);
        return "addCart";
    }

    @GetMapping("test")
    @ResponseBody
    public String test(){
        System.out.println(LoginInterceptor.getUserInfo());
        return "hello interceptors!!";
    }
}

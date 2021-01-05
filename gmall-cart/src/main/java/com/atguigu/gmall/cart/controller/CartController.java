package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts", carts);
        return "cart";
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    @GetMapping("test")
    @ResponseBody
    public String test() throws InterruptedException {
        long now = System.currentTimeMillis();
        this.cartService.executor1();
        ListenableFuture<String> future2 = this.cartService.executor2();
//        try {
//            future1.get();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//        future1.addCallback(t -> System.out.println("获取到future1的成功返回结果集：" + t), ex -> System.out.println("获取到future1的失败异常信息：" + ex.getMessage()));
//        future2.addCallback(t -> System.out.println("获取到future2的成功返回结果集：" + t), ex -> System.out.println("获取到future2的失败异常信息：" + ex.getMessage()));
        System.out.println(System.currentTimeMillis() - now);
        return "hello Async!!";
    }
}

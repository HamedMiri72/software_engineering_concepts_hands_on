package com.se;


import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductService {


    private static final Map<String, Product> FAKE_DB = Map.of(
            "p1", new Product("p1", "Mechanical keyboard", 129.99),
            "p2", new Product("p2", "27-inch monitor", 349.50),
            "p3", new Product("p3", "USB-C dock", 89.00)
    );

    private final AtomicInteger callCount = new AtomicInteger(0);

    public Product getById(String id){
        callCount.incrementAndGet();
        simulateSlowIo();
        Product product = FAKE_DB.get(id);
        if(product == null) throw new ProductNotFoundException(id);
        return product;
    }

    public int getCallCount(){
        return callCount.get();
    }


    private void simulateSlowIo(){
        try{
            Thread.sleep(500);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


}

package com.sales.tax.app.entity;

import com.sales.tax.io.util.CommonUtil;
import com.sales.tax.app.registry.Quantity;

import java.util.List;

/**
 * Created by Nilesh on 04-07-2017.
 */
public class Receipt {

    private List<PurchasedProduct> purchasedProducts;
    private Quantity totalSaleTax;
    private Quantity total;


    public Receipt(List<PurchasedProduct> purchasedProducts){
        this.purchasedProducts = purchasedProducts;
        calculateTotals();
    }


    public List<PurchasedProduct> getPurchasedProducts() {
        return purchasedProducts;
    }

    public Quantity getTotalSaleTax() {
        return totalSaleTax;
    }

    public Quantity getTotal() {
        return total;
    }

    private void calculateTotals(){
        if(!CommonUtil.isNullOrEmpty(purchasedProducts)){
            for(PurchasedProduct product : purchasedProducts){
                if(totalSaleTax == null){
                    totalSaleTax = new Quantity(0, product.getTaxApplied() != null? product.getTaxApplied().getUnit(): null);
                }
                if(total == null){
                    total = new Quantity(0, product.getOriginalPrice().getUnit());
                }
                totalSaleTax.add(product.getTaxApplied());
                total.add(product.getEndPrice());
            }
        }
    }
}

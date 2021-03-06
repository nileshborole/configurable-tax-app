package com.sales.tax.app.util;

import com.sales.tax.cache.CacheManager;
import com.sales.tax.cache.impl.CacheType;
import com.sales.tax.app.entity.Tax;
import com.sales.tax.app.entity.TaxCategory;
import com.sales.tax.io.util.CommonUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by Nilesh on 05-07-2017.
 */
public class TaxUtil {

    private static final Logger logger = LoggerFactory.getLogger(TaxUtil.class);
    private static final String FILE_SUFFIX = "-tax-config.xml";
    private volatile static boolean isRefresh = true;
    private static CacheManager manager = CacheManager.getInstance(CacheType.LOCAL_CACHE);


    public static Map<Object, Object> getTaxCategories() throws Exception{
        if(manager.getAll(AppCacheIdentifier.TAX_CATEGORY_CACHE) == null)
            loadMeta();

        return manager.getAll(AppCacheIdentifier.TAX_CATEGORY_CACHE);
    }

    public static Tax getTax(String id) throws Exception{
        if(manager.get(AppCacheIdentifier.TAX_CACHE, id) == null){
            loadMeta();
        }
        return (Tax) manager.get(AppCacheIdentifier.TAX_CACHE, id);
    }

    public static TaxCategory getTaxCategory(int code) throws Exception{
        if(manager.get(AppCacheIdentifier.TAX_CATEGORY_CACHE, code) == null)
            loadMeta();
        return (TaxCategory) manager.get(AppCacheIdentifier.TAX_CATEGORY_CACHE, code);
    }

    private static void loadMeta() throws Exception{

        if(!isRefresh)
            return;

        isRefresh = false;

        String apps = System.getProperty(CommonUtil.appNames);
        String configDir = System.getProperty(CommonUtil.configDir);
        StringTokenizer tokenizer = new StringTokenizer(apps, ",");
        while(tokenizer.hasMoreElements()){
            String prefix = tokenizer.nextToken();
            InputStream stream = TaxUtil.class.getClass().getResourceAsStream("/config/"+prefix+FILE_SUFFIX);

            if(stream == null)
                stream = new FileInputStream(configDir+"/config/"+prefix+FILE_SUFFIX);

            if(stream != null){
                loadMeta(stream);
            }else
                System.out.println("tax config not found.");
        }
    }


    private static void loadMeta(InputStream stream) throws Exception{
        System.out.println("reading tax config..");
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(stream);
        Element root = document.getRootElement();

        Element taxCategoryListElement = root.getChild("tax-categories");

        List<Element> taxCategoryElementList = taxCategoryListElement != null ? taxCategoryListElement.getChildren("category") : null;
        if(CommonUtil.isNullOrEmpty(taxCategoryElementList)){
           throw new Exception("tax-categories element not exists.");
        }

        for(Element taxCategoryElement : taxCategoryElementList){
            TaxCategory category = parseTaxCategory(taxCategoryElement);
            if(category != null)
                manager.put(AppCacheIdentifier.TAX_CATEGORY_CACHE, category.getCode(), category);
        }

        Element taxesElement = root.getChild("taxes");
        List<Element> taxElementList = taxesElement != null? taxesElement.getChildren("tax") : null;

        if(!CommonUtil.isNullOrEmpty(taxElementList)){
            for(int i = 0; i < taxElementList.size(); i++){
                Tax tax = parseTax(taxElementList.get(i), i);
                if(tax != null)
                    manager.put(AppCacheIdentifier.TAX_CACHE, tax.getId(), tax);
            }
        }

    }

    private static TaxCategory parseTaxCategory(Element taxCategoryElement){
        String codeStr = taxCategoryElement.getAttributeValue("code");
        String name = taxCategoryElement.getAttributeValue("name");

        if(CommonUtil.isNullOrEmpty(codeStr) || CommonUtil.isNullOrEmpty(name))
            return null;
        return new TaxCategory(Integer.parseInt(codeStr), name);
    }

    private static Tax parseTax(Element taxElement, int index) throws Exception{
        if(taxElement == null)
            return null;

        String id = taxElement.getAttributeValue("id");
        String title = taxElement.getChildTextNormalize("title");
        String percentageStr = taxElement.getChildTextNormalize("percentage");
        String categoryCodeStr = taxElement.getChildTextNormalize("category-code");

        if(CommonUtil.isNullOrEmpty(id) ||
                CommonUtil.isNullOrEmpty(title) ||
                CommonUtil.isNullOrEmpty(percentageStr) ||
                CommonUtil.isNullOrEmpty(categoryCodeStr)){
            throw new Exception("Tax not configured properly. [element index : "+ index+"]");
        }

        int categoryCode = Integer.parseInt(categoryCodeStr);

        if(manager.get(AppCacheIdentifier.TAX_CATEGORY_CACHE, categoryCode) == null)
            throw new Exception("tax category with category code "+categoryCode+ " is not exists");

        return new Tax(id, title, Float.parseFloat(percentageStr), categoryCode);
    }

    public static void refresh(){
        isRefresh = true;
    }

}

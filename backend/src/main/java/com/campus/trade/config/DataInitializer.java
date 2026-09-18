package com.campus.trade.config;

import com.campus.trade.entity.Product;
import com.campus.trade.repository.ProductRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 演示数据：仅当商品表为空时插入示例商品
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(ProductRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.save(build("高等数学（第七版）同济大学", "九成新，无笔记，考研复习必备。",
                    15.00, 45.00, "教材书籍"));
            repository.save(build("罗技 M275 无线鼠标", "毕业出，功能完好，送一节电池。",
                    35.00, 89.00, "数码电子"));
            repository.save(build("宿舍小台灯 LED", "三档调光，USB 供电，护眼不频闪。",
                    20.00, 59.00, "生活用品"));
            repository.save(build("斯伯丁篮球 74-604", "室外场打了一学期，弹性良好。",
                    80.00, 199.00, "运动户外"));
            repository.save(build("计算机网络（谢希仁 第八版）", "课程教材，封面轻微磨损。",
                    18.00, 49.00, "教材书籍"));
            repository.save(build("小米充电宝 20000mAh", "大容量，可上飞机，快充正常。",
                    60.00, 149.00, "数码电子"));
        };
    }

    private Product build(String title, String desc, double price, double origin, String category) {
        Product p = new Product();
        p.setTitle(title);
        p.setDescription(desc);
        p.setPrice(BigDecimal.valueOf(price));
        p.setOriginalPrice(BigDecimal.valueOf(origin));
        p.setCategory(category);
        p.setStatus("ON_SALE");
        return p;
    }
}

# json2object
json 字符串 自动生成转java bean代码

# 使用方式

0、将插件部署install 或 deploy

1、引入插件
```
<plugin>
  <groupId>com.project.demo</groupId>
  <artifactId>json2JavaBean</artifactId>
  <version>1.0</version>
</plugin>
```

2、命令行生成  

```
在命令行中输入： mvn json2JavaBean:json2bean -DjsonFile=./src/main/resources/bean.json

参数说明：jsonFile  - 指定文件路径
```

3、格式化自动生成的代码即可

--------------------------
json示例
```
{
    "order_id": 124235,    //订单号
    "userId": 12423344,   //用户ID
    "weight": 23.6,   //重量
    "plus": true,  //plus 专享
    "order_detail" : {     //订单详情
        "price": 124,     //价格
        "desc": "这是一个栗子" //描述
    },
    "orderItem": [     //商品条目
        {
            "product1": "鸡肉",    //品类
            "product2": "鸡肉没有涨"  //说明
        },
        {
            "product1": "猪肉",
            "product2": "猪肉涨价了"
        }
    ],
    "item_id": [    //所含商品id
        12,
        234,
        214
    ],
    "empty_object": {   ////注意： 不能为  写成{},  空object要换行
 
    },
    "empty_array": [    //注意： 不能为  写成[],数组要换行
 
    ]
}
```
自动生成代码demo  (需要自行格式化)
```
/**
 * @author generator
 * @date 2020-03-01
 */
@Data
public class $1 {

    /**
     * 订单号  例：124235
     */
    @JsonProperty("order_id")
    private Integer orderId;
    /**
     * 用户ID  例：12423344
     */
    private Integer userId;
    /**
     * 重量  例：23.6
     */
    private Double weight;
    /**
     * plus 专享  例：true
     */
    private Boolean plus;
    /**
     * 订单详情
     */
    @JsonProperty("order_detail")
    private OrderDetail orderDetail;
    /**
     * 商品条目
     */
    private List<$2> orderItem;
    /**
     * 所含商品id
     */
    @JsonProperty("item_id")
    private List<Integer> itemId;
    /**
     * //注意： 不能为  写成{},  空object要换行
     */
    @JsonProperty("empty_object")
    private Map<String, Object> emptyObject;
    /**
     * 注意： 不能为  写成[],数组要换行
     */
    @JsonProperty("empty_array")
    private List<Object> emptyArray;

    @Data
    public static class OrderDetail {

        /**
         * 价格  例：124
         */
        private Integer price;
        /**
         * 描述  例："这是一个栗子"
         */
        private String desc;
    }

    @Data
    public static class $2 {

        /**
         * 品类  例："鸡肉"
         */
        private String product1;
        /**
         * 说明  例："鸡肉没有涨"
         */
        private String product2;
    }
}
```
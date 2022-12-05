import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class testPinyin {

    @Test
    public void chineseSuggest() throws IOException {
        if(!fastPinyin.Init(true,"/Users/liyang77/Documents/GitHub/untitled/src/main/resources/pinyin.dict")){
            System.out.println("error");
        }
        String chinese = "问题： 有序无序与标题 一次选中多个标题样式，不同层级的序号没有跟着样式变化 选中的有序列表样式，工具栏下拉选择框展开后对应样式没有选中态 字号放大与行距 段落文本，段与段之间的行距应该是 24px";
        String words = "12345 ABCDE+67890[]`12345 abcde+67890[]`12345 abcde+67890[]`chaoguo64ge";
        String English = "car_tenant_admin";
        String complicated = "北汽坤宝";
        String charcter = "和";
        charcter = charcter.toLowerCase();

        int score = 0;
        String res;
        Map<Integer,Object> terms = new HashMap<>();
        fastPinyin.GenerateTerms(complicated,terms);

//        fastPinyin_nojianpin.GenerateTerms(complicated, terms);
        StringBuilder stringBuilder = new StringBuilder();
        System.out.println(terms.toString());

        terms.forEach((key,value)->{
            ((List)value).forEach((v)->{
                stringBuilder.append(v).append(" ");
            });
        });
        System.out.println(stringBuilder.toString());
    }
}

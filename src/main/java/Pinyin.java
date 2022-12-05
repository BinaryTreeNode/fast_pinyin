
public class Pinyin {
    public String pinyin;
    public int len;
    public Pinyin(String p, int l){
        pinyin = p;
        len = l;
    }
    public String getPinyin(){
        return pinyin;
    }
    public int getLen(){
        return len;
    }
    public static String DICT_PAIR_SPLIT = " ";
    public static String DICT_PINYIN_SPLIT = ",";
    public static String PINYIN_DICT_FILE = "pinyin.dict";
    public static final int JIANPIN_TERM_SIZE = 15;
    public static final int CHINESE_TERM_SIZE = 15;
    public static final int PINYIN_TERM_SIZE = 15;
    public static final int ENGLISH_TERM_SIZE = 64;

}

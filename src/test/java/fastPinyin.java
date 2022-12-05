
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class fastPinyin {

    public static boolean Init(boolean ifInfix,String filePath) throws IOException {
        m_ifInfix = ifInfix;
        m_pinyinDict = new HashMap<String, List<Pinyin>>();
        FileInputStream inputStream = new FileInputStream(filePath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        if (!bufferedReader.ready()) {
            return false;
        }

        int lineLen = 0;
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            String[] bound = line.split(Pinyin.DICT_PAIR_SPLIT);
            if (bound.length == 1) {
                StringBuilder errorData = new StringBuilder();
                errorData.append(line);
                return false;
            }

            StringBuilder character = new StringBuilder(); // will be released in destory()
            character.append(bound[0]);

            List<Pinyin> pinyins = new ArrayList<Pinyin>();

            String[] end = bound[1].split(Pinyin.DICT_PINYIN_SPLIT);
            StringBuilder pinyin;
            int pinyinLen = 0;

            while (end.length > pinyinLen) {
                pinyin = new StringBuilder(); // will be released in destory()
                pinyin.append(end[pinyinLen]);
                pinyins.add(new Pinyin(pinyin.toString(), pinyinLen));
                pinyinLen ++;
            }
            m_pinyinDict.put(character.toString(),pinyins);
        }

        return true;
    }

    /******
     *
     * @param word
     * @param resTerms
     */
    public static void GenerateTerms(final String word,  Map<Integer,Object> resTerms){
        //TODO 开头的变量可以优化
        List<String> res; // need delete
        int wordTotalLen = word.length();
        res = Arrays.asList(word);
        //记录原始词的长度
        int lens = 0;
        int position = 0;
        int limited = 0;
        //如果是中文，则为true，表示进行拼音处理
        //如果是英文，则为false，表示不进行拼音处理
        boolean iflag = true;
        int iPolyphone = 0;

        for(int iT = 0;  iT < res.size();iT++) {
            Map<Integer,Object> terms = new HashMap<Integer, Object>();
            List<List<Pinyin>> pinyins = new ArrayList<List<Pinyin>>();
            // get utf-8 lens,pinyin of each character of word
            int k = 0;
            List<Integer> charEnds = new ArrayList<Integer>(){{
                add(0);
            }}; // each element not inclueding the first means the character end pos in word. e.x. 58同城: {0,1,2,5,8}
            List<Integer> charPinyinEnds; // each element not inclueding the first means the character end pos in word. e.x. 58同城: {0,1,2,5,8}
            //    List<int> stopEnds; // each stop word ends in word. e.x. 58 同城: {3}
            int wordLen = res.get(iT).length();
            StringBuilder revisedWord = new StringBuilder();
            Pattern p = Pattern.compile("[\u4E00-\u9FA5]");
            Matcher m = p.matcher(res.get(iT));
            if (m.find()) {
                limited = Pinyin.CHINESE_TERM_SIZE;
                iflag = true;
                revisedWord.append(res.get(iT).length() > Pinyin.CHINESE_TERM_SIZE ? res.get(iT).substring(0, Pinyin.CHINESE_TERM_SIZE):res.get(iT));
            }else{
                limited = Pinyin.ENGLISH_TERM_SIZE;
                iflag = false;
                revisedWord.append(res.get(iT).length() > Pinyin.ENGLISH_TERM_SIZE ? res.get(iT).substring(0, Pinyin.ENGLISH_TERM_SIZE):res.get(iT));
            }
            //包含中文建15个字符,不包含中文等建64个
            for (k = 0; k < wordLen && k < limited && lens < wordTotalLen && lens < limited; k++) {
                if(iPolyphone >= 5){
                    //超过5个多音字不建索引
                    return;
                }
//                                if (1 == len) revisedWord.String.valueOf(revisedWord.charAt(k));  // 转小写
                StringBuilder character = new StringBuilder();
                character.append(revisedWord.substring(k,k+1));
                charEnds.add(charEnds.get(charEnds.size()-1) + 1);
                if (null == m_pinyinDict.get(character.toString())) {
                    List<Pinyin> tmp = new ArrayList<Pinyin>();
                    tmp.add(new Pinyin(character.toString(), 1));
                    pinyins.add(new ArrayList<Pinyin>(tmp));
                    // only non chinese characeter cannot be found in pinyinDict
                } else {
                    List<Pinyin> pinyin = m_pinyinDict.get(character.toString());
                    if(pinyin.size() > 1) {
                        ++iPolyphone;
                    }
                    pinyins.add(pinyin);
                } ;

            }
            lens += k;
            ++lens;

            if(iflag) {
                // get pinyin terms: changfenggongyu
                Set<String> termSet = new HashSet<String>(); // used for filtering duplicate
                //GetPinyinDescartes(pinyins, charEnds, terms, wordLen, score, termSet);
                GetPinyinAndJianPinTerms(terms, pinyins, charEnds, wordLen,  m_ifInfix, termSet, position);
            }

            // get original character terms:长风公寓
            int posSize = charEnds.size();
            int infixEnd = m_ifInfix ? posSize : 1;
            int eachTermPos = 0;
            for (int i = 0; i < infixEnd; ++i) {
                List<String> term = new ArrayList<String>();
                for (int j = i + 1; j < posSize; ++j) {
                    String str = revisedWord.substring(i,j);
                    term.add(str);
                    // 中文索引最多只建15个中文字符，或者遇到停用词结束。
//                    if (ORIGINAL_TERM_SIZE <= term.size()) break;
                }
                if(i < ((Map<Integer,Object>)terms).size()){
                    ((List<String>)terms.get(i+position)).addAll(term);
                }else{
                    terms.put(i+position,term);
                }
            }
            position += terms.size();
            resTerms.putAll(terms);
        }
    }

    public static void GetPinyinAndJianPinTerms(Map<Integer,Object> terms, List<List<Pinyin>> pinyins, List<Integer> charEnds, int wordLen, boolean ifInfix, Set<String> termSet, int position){
        /*
         * from pinyins to pinyinDescartes
         * e.x.:
         * source: 车公庄
         * pinyins: [["che","ju"], ["gong"], ["zhuang"]]
         * pinyinDescartes: [["che", "gong", "zhuang"], ["ju", "gong", "zhuang"]]
         */
        List<Pinyin> pinyinCombination = new ArrayList<Pinyin>();
        for (short i = 0; i < pinyins.size(); ++i) pinyinCombination.add(pinyins.get(i).get(0));

        List<List<Pinyin>> pinyinDescartes = new ArrayList<List<Pinyin>>();
        pinyinDescartes.add(pinyinCombination);
        for (short i =0; i < pinyins.size(); ++i) {
            for (short j = 1; j < pinyins.get(i).size(); ++j) {
                int curDescartesSize = pinyinDescartes.size();
                for (short k = 0; k < curDescartesSize; ++k) {
                    //这里对多音字进行分别替换，组成不同的list
                    pinyinCombination = new ArrayList<Pinyin>(pinyinDescartes.get(k));
                    pinyinCombination.set(i,pinyins.get(i).get(j));
                    pinyinDescartes.add(pinyinCombination);
                }
            }
        }

        StringBuilder pinyinStr;
        StringBuilder jianpinStr;
        int pinyinSize = pinyinDescartes.get(0).size();
        int infixEnd = ifInfix ? pinyinSize : 1;
        Map<Integer,Object> headTerms = new HashMap<Integer, Object>();
        //e.x.: go through [["che","gong", "zhuang"],["ju", "gong", "zhuang"]]
        for (List<Pinyin> pinyinVec : pinyinDescartes) {

            //e.x.: go through ["che","gong", "zhuang"] to get ["c", "cg", "cgz", "g", "gz", "z"]
            for (int i = 0; i < infixEnd; ++i) {
                List<String> term = new ArrayList<String>();
                jianpinStr = new StringBuilder();
                for (int j = i; j < pinyinSize; ++j) {
//                    if (JIANPIN_TERM_SIZE <= jianpinStr.length()) break;
                    jianpinStr.append(pinyinVec.get(j).getPinyin().charAt(0));
                    term.add(jianpinStr.toString());
                }
                if (i < ((Map<Integer, Object>) headTerms).size()) {
                    if(headTerms.get(i+position).equals(term)){
                        continue;
                    }
                    for(String t : term){
                        if(!((List<String>) headTerms.get(i+position)).contains(t)){
                            ((List<String>) headTerms.get(i+position)).add(t);
                        }
                    }
                } else {
                    headTerms.put(i+position, term);
                }
            }
        }
        Map<Integer,Object> pinyinTerms = new HashMap<Integer, Object>();

        for (List<Pinyin> pinyinVec : pinyinDescartes) {
            for (int i = 0; i < infixEnd; ++i) {
                List<String> term = new ArrayList<String>();
                pinyinStr = new StringBuilder();
                for (int j = i; j < pinyinSize; ++j) {
//                    if(PINYIN_TERM_SIZE <term.size()) break;
                    pinyinStr.append(pinyinVec.get(j).getPinyin());
                    term.add(pinyinStr.toString());
                }
                if(i < ((Map<Integer,Object>)pinyinTerms).size()){
                    if(pinyinTerms.get(i+position).equals(term)){
                        continue;
                    }
                    for(String t : term){
                        if(!((List<String>) pinyinTerms.get(i+position)).contains(t)){
                            ((List<String>) pinyinTerms.get(i+position)).add(t);
                        }
                    }
                }else{
                    pinyinTerms.put(i+position,term);
                }
            }
        }
        terms.putAll(headTerms);
        for(int i = 0 ; i < terms.size();i++){
            ((List<String>)terms.get(i+position)).addAll((List<String>)pinyinTerms.get(i+position));
        }
    }

    /****
     *
     * @param word
     * @return
     *
     */
    public static String GenerateChineseTerms(final String word ){
        if(word == null || word.isEmpty()){
            return null;
        }
        StringBuilder resSb = new StringBuilder();
        int wordTotalLen = word.length();

        //记录原始词的长度
        int lens = 0;
        int position = 0;
        //如果是中文，则为true，表示进行拼音处理
        //如果是英文，则为false，表示不进行拼音处理

        Map<Integer,Object> terms = new HashMap<Integer, Object>();
        // get utf-8 lens,pinyin of each character of word
        int k = 0;
        // each element not inclueding the first means the character end pos in word. e.x. 58同城: {0,1,2,5,8}
        List<Integer> charEnds = new ArrayList<Integer>(){{
            add(0);
        }};
        int wordLen = word.length();
        StringBuilder revisedWord = new StringBuilder();
        revisedWord.append(word);

        for (k = 0; k < wordLen && lens < wordTotalLen ; k++) {
            charEnds.add(charEnds.get(charEnds.size()-1) + 1);
        }

        int posSize = charEnds.size();
        int infixEnd = posSize;
        for (int i = 0; i < infixEnd; ++i) {
            Set<String> term = new HashSet<>();
            for (int j = i + 1; j < posSize; ++j) {
                String str = revisedWord.substring(i,j);
                term.add(str);
                // 中文索引最多只建15个中文字符，或者遇到停用词结束。
//                    if (ORIGINAL_TERM_SIZE <= term.size()) break;
            }
            if(i < ((Map<Integer,Object>)terms).size()){
                ((Set<String>)terms.get(i+position)).addAll(term);
            }else{
                terms.put(i+position,term);
            }
        }
        System.out.println(terms);

        for (Map.Entry<Integer, Object> map : terms.entrySet()) {
            for(String s : (Set<String>)map.getValue()){
                resSb.append(s).append(" ");
            }
        }
        return resSb.toString();
    }


    static boolean m_ifInfix;

    static Map<String, List<Pinyin>> m_pinyinDict;
}


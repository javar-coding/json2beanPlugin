package com.project.demo.json;


import static com.project.demo.json.Node.NodeType.*;

import com.google.common.collect.Lists;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.maven.plugin.logging.Log;


/**
 * @author libo
 * @date 2020-02-19
 */
public class BeanUtils {

    /**
     * 解决模式
     *  1、"example1" : {
     *  2、  {
     *  3、"example1" : {    // 注释1{
     *  4、   {              //{ 注释2
     */
    private static Pattern OBJECT_PATTERN = Pattern.compile("^\\s*(\"[a-zA-Z_0-9]*\"\\s*:\\s*)?(\\{)\\s*(//.*)?$");
    /**
     * 解决模式
     * 1、}
     * 2、},
     */
    private static Pattern OBJECT_PATTERN_END = Pattern.compile("^\\s*(\\})[,]?\\s*$");
    private static int OBJECT_CLASS_NAME = 1;
    private static int OBJECT_CLASS_FLAG = 2;
    private static int OBJECT_CLASS_ANNOTATION = 3;
    private static int OBJECT_PATTERN_END_FLAG = 1;
    /**
     * 解决模式
     * 1、"example1": [
     * 2、   [
     * 3、"example1": [  // 注释[
     * 4、  [    //    zhu注[释]
     */
    private static Pattern ARRAY_PATTERN = Pattern.compile("^\\s*(\"[a-zA-Z_0-9]*\"\\s*:\\s*)?(\\[)\\s*(//.*)?$");
    /**
     * 解决模式
     * 1、]
     * 2、],
     */
    private static Pattern ARRAY_PATTERN_END = Pattern.compile("^\\s*(\\])[,]?\\s*$");
    private static int ARRAY_NAME = 1;
    private static int ARRAY_FLAG = 2;
    private static int ARRAY_ANNOTATION = 3;
    private static int ARRAY_END_FLAG = 1;
    /**
     * 解决模式
     * 1、"example1": "43546"
     * 2、"example2": "34343",
     * 3、"example3": "zdsfsd"
     * 4、"example4": "zdsfsd",
     * 11、"example11": "",
     * 14、"example14": "435,46"   //注释  似懂非"懂是
     * 15、"example15": "SDA43546"   //注释 // 似懂非懂是
     * 16、"example16": "S4//3546",   //注释 // 似懂非懂是
     * 17、"example17": "S4//3546"   //注释 // 似懂非懂是
     * 18、"example18": "S4{3546"   //注释 // 似懂非懂是
     * 19、"example19": "[]"
     * 20、"example20": "[",
     * 21、"example21": "]",
     * 22、"example22": "}",
     * 23、"example23": "true",
     * 24、"example24": "true"
     * 25、"example25": "S4{3546"   //注释 // 似懂"非懂是
     * 26、"example26": "S4{3"546"   //注释 // 似懂"非懂是
     * 27、"example27": "S4{3"546"
     * 28、"example28": """
     */
    private static Pattern NORMAL_STRING_PATTERN = Pattern.compile("^\\s*\"([a-zA-Z_0-9]*)\"\\s*:\\s*(\".*\")[,]?\\s*(//.*)?$");
    /**
     * 5、"example5": 1
     * 6、"example6": 1,
     * 7、"example7": 1232
     * 8、"example8": 1232,
     * 9、"example9": 12.32
     * 10、"example10": 12.32,
     * 12、"example12": 1232    // 注释  32r5345
     * 13、"example13": 1232,    // 注释  32r5345
     */
    private static Pattern NORMAL_NUMBER_PATTERN = Pattern.compile("^\\s*\"([a-zA-Z_0-9]*)\"\\s*:\\s*([0-9\\.]+)[,]?\\s*(//.*)?$");
    private static int NORMAL_FIELD_NAME = 1;
    private static int NORMAL_FILED_VALUE = 2;
    private static int NORMAL_FILED_ANNOTATION = 3;
    /**
     * 解决模式
     * 1、"example1": true
     * 2、"example2": true,
     * 3、"example3": false
     * 4、"example4": false,
     * 5、"example5": TRUE,
     * 6、"example6": TRUE
     * 7、"example7": FALSE
     * 8、"example8": FALSE,
     * 9、"example9": TRUE     //   true注释1
     * 10、"example9": TRUE,     //   false注释1
     */
    private static Pattern BOOLEAN_PATTERN = Pattern.compile("^\\s*\"([a-zA-Z_0-9]*)\"\\s*:\\s*(true|false|TRUE|FALSE)[,]?[\\s]*(//.*)?$");
    private static int BOOLEAN_FIELD_NAME = 1;
    private static int BOOLEAN_FILED_VALUE = 2;
    private static int BOOLEAN_FILED_ANNOTATION = 3;
    /**
     * 解决模式
     * 1、"sdfd"
     * 2、12432
     * 3、"124"sfs",
     * 4、1244,
     * 5、true
     * 6、true,
     * 7、0.34
     * 8、"0.24"
     * 9、"0.24",
     * 10、"324",
     */
    private static Pattern ARRAY_PLAIN_ITEM_STRING = Pattern.compile("^\\s*\"(.*)\"[,]?\\s*$");
    private static Pattern ARRAY_PLAIN_ITEM_NUMBER = Pattern.compile("^\\s*([0-9\\.]+)[,]?\\s*$");
    private static Pattern ARRAY_PLAIN_ITEM_BOOLEAN = Pattern.compile("^\\s*(true|false|TRUE|FALSE)[,]?\\s*$");
    private static int ARRAY_PLAIN_ITEM_VALUE = 1;

    public static void main(String[] args) throws IOException {
        assertCase();
        URL file = BeanUtils.class.getResource("/bean.json");
        InputStream inputStream = file.openStream();
        List<String> lines = IOUtils.readLines(inputStream, Charset.forName("UTF-8"));
        Node node = treeify(lines, 0);
        System.out.println(node);
        IOUtils.writeLines(getFileContent(node), "\n" , new FileOutputStream(node.getClassName() + ".java"), Charset.defaultCharset());
    }

    private static int CLASS_COUNT = 1;

    public static void analyzing(File in, Log log) throws IOException {
        List<String> lines = IOUtils.readLines(new FileInputStream(in), Charset.forName("UTF-8"));
        Node node = treeify(lines, 0);
        log.debug("节点树：" + node);
        log.debug("生成文件目录：" + in.getParentFile().getPath());
        IOUtils.writeLines(getFileContent(node), "\n" , new FileOutputStream(in.getParentFile().getPath() + "/" + node.getClassName() + ".java"), Charset.defaultCharset());
    }

    private static Node treeify(List<String> lines, int lineCount) {
        String start = lines.get(lineCount);
        Node root = createNode(start, lineCount);
        List<Node> nodeList = Lists.newArrayList();
        for (lineCount = lineCount + 1;lineCount < lines.size(); lineCount ++) {
            String line = lines.get(lineCount);
            Node node;
            Matcher matcher;
            if (StringUtils.isEmpty(line)) {
                continue;
            } else if ((matcher = OBJECT_PATTERN.matcher(line)).find() && matcher.group(OBJECT_CLASS_FLAG).equals("{")) {
                node = treeify(lines, lineCount);
                lineCount = node.getLineEnd();
            } else if ((matcher = ARRAY_PATTERN.matcher(line)).find() && matcher.group(ARRAY_FLAG).equals("[")) {
                node = treeify(lines, lineCount);
                lineCount = node.getLineEnd();
            } else if ((matcher = OBJECT_PATTERN_END.matcher(line)).find() && matcher.group(OBJECT_PATTERN_END_FLAG).equals("}")) {
                break;
            } else if ((matcher = ARRAY_PATTERN_END.matcher(line)).find() && matcher.group(ARRAY_END_FLAG).equals("]")) {
                break;
            } else {
                node = createNormal(line, lineCount);
            }
            nodeList.add(node);
        }
        root.setChild(nodeList);
        root.setLineEnd(lineCount);
        return root;
    }

    private static Node createNode(String string, int line) {
        Matcher matcher = OBJECT_PATTERN.matcher(string);
        if (matcher.find()) {
            Node node = new Node();
            node.setNodeType(Node.NodeType.OBJECT);
            if (matcher.group(OBJECT_CLASS_NAME) != null) {
                String name = matcher.group(OBJECT_CLASS_NAME);
                setFiledName(name, node);
                node.setClassName(node.getName().substring(0,1).toUpperCase() + node.getName().substring(1));
            } else {
                node.setClassName("$" + (CLASS_COUNT++));
            }
            if (matcher.group(OBJECT_CLASS_ANNOTATION) != null) {
                String annotation = matcher.group(OBJECT_CLASS_ANNOTATION);
                //截取注释
                node.setAnnotation(annotation.substring(2));
            }
            node.setLineStart(line);
            return node;
        }
        matcher = ARRAY_PATTERN.matcher(string);
        if (matcher.find()) {
            Node node = new Node();
            node.setNodeType(Node.NodeType.ARRAY);
            if (matcher.group(ARRAY_NAME) != null) {
                String name = matcher.group(1);
                setFiledName(name, node);
            }
            if (matcher.group(ARRAY_ANNOTATION) != null) {
                String annotation = matcher.group(ARRAY_ANNOTATION);
                //截取注释
                node.setAnnotation(annotation.substring(2));
            }
            node.setLineStart(line);
            return node;
        }

        throw new IllegalArgumentException("第" + line + "行：【" + string + "】格式错误");
    }

    private static Node createNormal(String string, int line) {
        Matcher matcher;
        if ((matcher = NORMAL_STRING_PATTERN.matcher(string)).find()
                || (matcher = BOOLEAN_PATTERN.matcher(string)).find()
                || (matcher = NORMAL_NUMBER_PATTERN.matcher(string)).find()) {
            Node node = new Node();
            node.setNodeType(PLAIN);
            String name = matcher.group(NORMAL_FIELD_NAME);
            setFiledName(name, node);
            String value = matcher.group(NORMAL_FILED_VALUE);
            node.setClassName(getPlainClassName(value));
            if (matcher.group(NORMAL_FILED_ANNOTATION) != null) {
                String annotation = matcher.group(NORMAL_FILED_ANNOTATION);
                node.setAnnotation(annotation.substring(2) + "  例：" + value);
            } else {
                node.setAnnotation("例：" + value);
            }
            node.setLineStart(line);
            node.setLineEnd(line);
            return node;
        }
        matcher = ARRAY_PLAIN_ITEM_STRING.matcher(string);
        if (matcher.find()) {
            Node node = new Node();
            node.setNodeType(Node.NodeType.ARRAY_PLAIN_ITEM);
            if (matcher.group(ARRAY_PLAIN_ITEM_VALUE) != null) {
                node.setClassName("String");
            }
            node.setLineStart(line);
            node.setLineEnd(line);
            return node;
        }
        matcher = ARRAY_PLAIN_ITEM_NUMBER.matcher(string);
        if (matcher.find() || (matcher = ARRAY_PLAIN_ITEM_BOOLEAN.matcher(string)).find()) {
            Node node = new Node();
            node.setNodeType(Node.NodeType.ARRAY_PLAIN_ITEM);
            if (matcher.group(ARRAY_PLAIN_ITEM_VALUE) != null) {
                node.setClassName(getPlainClassName(matcher.group(ARRAY_PLAIN_ITEM_VALUE)));
            }
            node.setLineStart(line);
            node.setLineEnd(line);
            return node;
        }

        throw new IllegalArgumentException("第" + line + "行：【" + string + "】格式错误");
    }

    private static String getPlainClassName(String value) {
        if ("true".equals(value) || "TRUE".equals(value) || "false".equals(value) || "FALSE".equals(value)) {
            return "Boolean";
        } else if (NumberUtils.isNumber(value)) {
            if (value.contains(".")) {
                return "Double";
            } else {
                Long val = NumberUtils.toLong(value);
                if (val > Integer.MAX_VALUE) {
                    return "Long";
                } else {
                    return "Integer";
                }
            }
        }
        return "String";
    }

    private static List<String> getFileContent(Node root) {
        Deque<Node> nodeDeque = new LinkedList<>();
        if (root.getNodeType() == ARRAY) {
            throw new IllegalArgumentException("不能解析json最外层为数组格式");
        }
        List<String> content = getClassContent(root, nodeDeque);
        content.add(0, "/**\n*@author generator \n * @date " + LocalDate.now() + "\n*/\n@Data\npublic class " + root.getClassName() + " { ");
        content.addAll(getStaticClassContent(nodeDeque));
        content.add("}");
        return content;
    }

    private static List<String> getStaticClassContent(Deque<Node> nodeDeque) {
        if (nodeDeque == null || nodeDeque.isEmpty()) {
            return Lists.newLinkedList();
        }
        List<String> staticClassContent = Lists.newLinkedList();
        while (nodeDeque.size() > 0) {
            Node node = nodeDeque.pollFirst();
            List<String> classContent = getClassContent(node, nodeDeque);
            classContent.add(0, "@Data\npublic static class " + node.getClassName() + "{");
            classContent.add("}");
            staticClassContent.addAll(classContent);
        }
        return staticClassContent;
    }

    private static List<String> getClassContent(Node root, Deque<Node> nodeDeque) {
        int childCount = CollectionUtils.isNotEmpty(root.getChild()) ? root.getChild().size() : 0;
        List<String> content = Lists.newLinkedList();
        for (int i = 0; i < childCount ; i ++) {
            Node child = root.getChild().get(i);
            if (child.getNodeType() == PLAIN) {
                content.addAll(getAnnotationAndLineName(child));
                content.add("private " + child.getClassName() + " " + child.getName() + ";" );
            } else if (child.getNodeType() == ARRAY) {
                content.addAll(getAnnotationAndLineName(child));
                if (CollectionUtils.isEmpty(child.getChild()) || !isOneClassInArrayNode(child.getChild())) {
                    content.add("private List<Object> " + child.getName() + ";");
                    if (CollectionUtils.isNotEmpty(child.getChild())) {
                        child.getChild().stream()
                                .filter(arrayChild -> arrayChild.getNodeType() == OBJECT)
                                .forEach(arrayChild -> nodeDeque.addLast(arrayChild));
                    }
                } else {
                    child.setChild(child.getChild().subList(0, 1));
                    content.add("private List<" + child.getChild().get(0).getClassName()  + "> " + child.getName() + ";");
                    if (child.getChild().get(0).getNodeType() == OBJECT) {
                        nodeDeque.addLast(child.getChild().get(0));
                    }
                }
            } else if (child.getNodeType() == OBJECT) {
                content.addAll(getAnnotationAndLineName(child));
                if (CollectionUtils.isEmpty(child.getChild())) {
                    content.add("private Map<String, Object> " + child.getName() + ";");
                } else {
                    content.add("private " + child.getClassName() + " " + child.getName() + ";");
                    nodeDeque.addLast(child);
                }
            }
        }
        return content;
    }


    private static boolean isOneClassInArrayNode(List<Node> nodeList) {
        Set<String> set = new HashSet<>();
        for (int i = 0 ; i < nodeList.size(); i ++) {
            Node node = nodeList.get(i);
            set.add(node.getClassName().replaceAll("[0-9]", ""));
        }
        return set.size() == 1;
    }

    private static List<String> getAnnotationAndLineName(Node node) {
        List<String> content = Lists.newArrayList();
        if (node.getAnnotation() != null) {
            content.add("/** \n * " + node.getAnnotation() + " \n */");
        }
        if (node.getLineName() != null) {
            content.add("@JsonProperty(\"" + node.getLineName() +"\")");
        }
        return content;
    }

    private static void setFiledName(String name, Node node) {
        String temp = name;
        if (temp.contains("\"")) {
            temp = name.substring(name.indexOf("\"") + 1, name.lastIndexOf("\""));
        }
        if (temp.contains("_")) {
            node.setLineName(temp);
            String hump = StringUtil.line2Hump(temp);
            node.setName(hump);
        } else {
            node.setName(temp);
        }
    }

    private static void assertCase(){
        assertObjectCase();
        assertObjectEnd();
        assertArrayCase();
        assertArrayEnd();
        assertBooleanCase();
        assertNormalStringCase();
        assertNormalIntegerCase();
        assertArrayPlainItemCase();
    }
    private static List<String> ARRAY_END_TEST_LIST = Lists.newArrayList(
            "]",
            " ],    "
    );
    private static void assertArrayEnd() {
        String testCase = ARRAY_END_TEST_LIST.get(0);
        Matcher matcher = ARRAY_PATTERN_END.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_END_FLAG).equals("]")) {

        } else {
            throw new IllegalArgumentException("array end case0 失败");
        }

        testCase = ARRAY_END_TEST_LIST.get(1);
        matcher = ARRAY_PATTERN_END.matcher(testCase);
        if (matcher.find() && matcher.group(OBJECT_PATTERN_END_FLAG).equals("]")) {

        } else {
            throw new IllegalArgumentException("array end case1 失败");
        }
    }


    private static List<String> OBJECT_END_TEST_LIST = Lists.newArrayList(
            " }",
            " }, "
    );
    private static void assertObjectEnd() {
        String testCase = OBJECT_END_TEST_LIST.get(0);
        Matcher matcher = OBJECT_PATTERN_END.matcher(testCase);
        if (matcher.find() && matcher.group(OBJECT_PATTERN_END_FLAG).equals("}")) {

        }else {
            throw new IllegalArgumentException("object end case0 失败");
        }

        testCase = OBJECT_END_TEST_LIST.get(1);
        matcher = OBJECT_PATTERN_END.matcher(testCase);
        if (matcher.find() && matcher.group(OBJECT_PATTERN_END_FLAG).equals("}")) {

        }else {
            throw new IllegalArgumentException("object end case1 失败");
        }
    }

    private static List<String> OBJECT_TEST_LIST = Lists.newArrayList(
            "    \"example1\" : {",
            "  { ",
            "\"example1\" : {    // 注释1{  , ",
            "   {              //{ 注//释2 "
    );
    private static void assertObjectCase() {
        //"    \"example1\" : {",
        String testCase = OBJECT_TEST_LIST.get(0);
        Matcher matcher = OBJECT_PATTERN.matcher(testCase);
        if (matcher.find() && "\"example1\" : ".equals(matcher.group(OBJECT_CLASS_NAME)) && matcher.group(OBJECT_CLASS_FLAG) != null) {

        } else {
            throw new IllegalArgumentException("object case1 失败");
        }
        //"  { ",
        testCase =  OBJECT_TEST_LIST.get(1);
        matcher = OBJECT_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(OBJECT_CLASS_NAME) == null && matcher.group(OBJECT_CLASS_FLAG) != null) {

        }else {
            throw new IllegalArgumentException("object case2 失败");
        }

        //"\"example1\" : {    // 注释1{  , ",
        testCase =  OBJECT_TEST_LIST.get(2);
        matcher = OBJECT_PATTERN.matcher(testCase);
        if (matcher.find() && "\"example1\" : ".equals(matcher.group(OBJECT_CLASS_NAME))
                && matcher.group(OBJECT_CLASS_FLAG) != null
                && matcher.group(OBJECT_CLASS_ANNOTATION).equals("// 注释1{  , ")) {

        }else {
            throw new IllegalArgumentException("object case3 失败");
        }

        //"   {              //{ 注//释2 ";
        testCase =  OBJECT_TEST_LIST.get(3);
        matcher = OBJECT_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(OBJECT_CLASS_NAME) == null
                && matcher.group(OBJECT_CLASS_FLAG) != null
                && matcher.group(OBJECT_CLASS_ANNOTATION).equals("//{ 注//释2 ")) {

        }else {
            throw new IllegalArgumentException("object case4 失败");
        }
    }

    private static List<String> ARRAY_TEST_LIST = Lists.newArrayList(
            "\"example1\": [   ",
            "   [",
            "   [    //[ 注释   ]",
            "  \"example1\": [ //注释1   "
    );
    private static void assertArrayCase() {
        String testCase = ARRAY_TEST_LIST.get(0);
        Matcher matcher = ARRAY_PATTERN.matcher(testCase);
        if (matcher.find() && "\"example1\": ".equals(matcher.group(ARRAY_NAME)) && matcher.group(ARRAY_FLAG) != null){

        } else {
            throw new IllegalArgumentException("array case1 失败");
        }

        testCase = ARRAY_TEST_LIST.get(1);
        matcher = ARRAY_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_NAME) == null && matcher.group(ARRAY_FLAG) != null){

        } else {
            throw new IllegalArgumentException("array case2 失败");
        }

        testCase = ARRAY_TEST_LIST.get(2);
        matcher = ARRAY_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_NAME) == null
                && matcher.group(ARRAY_FLAG) != null
                && matcher.group(ARRAY_ANNOTATION).equals("//[ 注释   ]")){

        } else {
            throw new IllegalArgumentException("array case3 失败");
        }


        testCase = ARRAY_TEST_LIST.get(3);
        matcher = ARRAY_PATTERN.matcher(testCase);
        if (matcher.find() && "\"example1\": ".equals(matcher.group(ARRAY_NAME))
                && matcher.group(ARRAY_FLAG) != null
                && matcher.group(ARRAY_ANNOTATION).equals("//注释1   ")){

        } else {
            throw new IllegalArgumentException("array case4 失败");
        }

    }

    private static List<String> BOOLEAN_TEST_CASE_LIST = Lists.newArrayList(
            "\"example1\": true",
            "\"example2\": true,",
            "\"example3\": false",
            "\"example4\": false,",
            "\"example5\": TRUE,",
            "\"example6\": TRUE",
            "\"example7\": FALSE",
            "\"example8\": FALSE,",
            "\"example9\": TRUE     //   true注释1",
            "\"example10\": TRUE,     //   false注释1"
    );
    private static void assertBooleanCase() {
        String testCase = BOOLEAN_TEST_CASE_LIST.get(0);
        Matcher matcher = BOOLEAN_PATTERN.matcher(testCase);
        if(matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example1")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("true")
                && matcher.group(BOOLEAN_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("boolean case1 失败");
        }

        testCase = BOOLEAN_TEST_CASE_LIST.get(1);
        matcher = BOOLEAN_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example2")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("true")
                && matcher.group(BOOLEAN_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("boolean case2 失败");
        }

        testCase = BOOLEAN_TEST_CASE_LIST.get(2);
        matcher = BOOLEAN_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example3")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("false")
                && matcher.group(BOOLEAN_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("boolean case3 失败");
        }

        testCase = BOOLEAN_TEST_CASE_LIST.get(3);
        matcher = BOOLEAN_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example4")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("false")
                && matcher.group(BOOLEAN_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("boolean case4 失败");
        }

        testCase = BOOLEAN_TEST_CASE_LIST.get(4);
        matcher = BOOLEAN_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example5")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("TRUE")
                && matcher.group(BOOLEAN_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("boolean case5 失败");
        }

        testCase = BOOLEAN_TEST_CASE_LIST.get(5);
        matcher = BOOLEAN_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example6")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("TRUE")
                && matcher.group(BOOLEAN_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("boolean case6 失败");
        }

        testCase = BOOLEAN_TEST_CASE_LIST.get(6);
        matcher = BOOLEAN_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example7")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("FALSE")
                && matcher.group(BOOLEAN_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("boolean case7 失败");
        }

        testCase = BOOLEAN_TEST_CASE_LIST.get(7);
        matcher = BOOLEAN_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example8")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("FALSE")
                && matcher.group(BOOLEAN_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("boolean case8 失败");
        }

        testCase = BOOLEAN_TEST_CASE_LIST.get(8);
        matcher = BOOLEAN_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example9")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("TRUE")
                && matcher.group(BOOLEAN_FILED_ANNOTATION).equals("//   true注释1")) {

        }else {
            throw new IllegalArgumentException("boolean case9 失败");
        }

        testCase = BOOLEAN_TEST_CASE_LIST.get(9);
        matcher = BOOLEAN_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(BOOLEAN_FIELD_NAME).equals("example10")
                && matcher.group(BOOLEAN_FILED_VALUE).equals("TRUE")
                && matcher.group(BOOLEAN_FILED_ANNOTATION).equals("//   false注释1")) {

        }else {
            throw new IllegalArgumentException("boolean case10 失败");
        }

        if (countCase(NORMAL_STRING_CASE_LIST, BOOLEAN_PATTERN) != 0) {
            throw new IllegalArgumentException("boolean正则表达式针对普通字符串元素有误");
        }

        if (countCase(NORMAL_NUMBER_CASE_LIST, BOOLEAN_PATTERN) != 0) {
            throw new IllegalArgumentException("boolean正则表达式针对普通Number元素有误");
        }

        if (countCase(ARRAY_PLAIN_ITEM_CASE_LIST, BOOLEAN_PATTERN) != 0) {
            throw new IllegalArgumentException("boolean正则表达式针对数组item有误");
        }
    }

    private static long countCase(List<String> cases, Pattern pattern) {
        return cases.stream().filter(tc -> {
            Matcher matcher1 = BOOLEAN_PATTERN.matcher(tc);
            return matcher1.find();
        }).count();
    }

    private static List<String> NORMAL_STRING_CASE_LIST = Lists.newArrayList(
            "     \"example1\": \"43546\"",
            "\"example2\": \"34343\",",
            "\"example3\":      \"zdsfsd\"",
            "\"example4\": \"zdsfsd\",",
            "\"example11\"    : \"\",",
            "\"example14\": \"435,46\"   //注释  似懂非\"懂是",
            "\"example15\": \"SDA43546\"   //注释 // 似懂非\"懂是",
            "\"example16\":      \"S4//3546\",   //注释 // 似懂非懂是",
            "\"example17\": \"S4//3546\"   //注释 // 似懂非懂是",
            "\"example18\": \"S4{3546\"   //注释 // 似懂非懂是",
            "     \"example19\"     : \"[]\"",
            "\"example20\": \"[\",",
            "\"example21\": \"]\",",
            "\"example22\": \"}\",",
            "\"example23\": \"true\",",
            "\"example24\": \"true\"",
            "\"example25\": \"S4{3546\"   //注释 // 似懂\"非懂是",
            "\"example26\": \"S4{3\"546\"   //注释 // 似懂\"非懂是",
            "\"example27\": \"S4{3\"546\"",
            "\"example28\": \"\"\""
    );

    private static void assertNormalStringCase() {
        //"     \"example1\": \"43546\"",
        String testCase = NORMAL_STRING_CASE_LIST.get(0);
        Matcher matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example1")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"43546\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        } else {
            throw new IllegalArgumentException("normal string case0 失败");
        }

        //"\"example2\": \"34343\",",
        testCase = NORMAL_STRING_CASE_LIST.get(1);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example2")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"34343\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal string case1 失败");
        }

        //"\"example4\": \"zdsfsd\",",
        testCase = NORMAL_STRING_CASE_LIST.get(2);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example3")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"zdsfsd\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal string case2 失败");
        }

        //"\"example4\": \"zdsfsd\",",
        testCase = NORMAL_STRING_CASE_LIST.get(3);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example4")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"zdsfsd\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal string case4 失败");
        }

        //"\"example11\"    : \"\",",
        testCase = NORMAL_STRING_CASE_LIST.get(4);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example11")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal string case11 失败");
        }

        //"example14": "435,46"   //注释  似懂非"懂是
        testCase = NORMAL_STRING_CASE_LIST.get(5);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example14")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"435,46\"")
                && matcher.group(NORMAL_FILED_ANNOTATION).equals("//注释  似懂非\"懂是")) {

        }else {
            throw new IllegalArgumentException("normal string case14 失败");
        }

        //"example15": "SDA43546"   //注释 // 似懂非"懂是
        testCase = NORMAL_STRING_CASE_LIST.get(6);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example15")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"SDA43546\"")
                && matcher.group(NORMAL_FILED_ANNOTATION).equals("//注释 // 似懂非\"懂是")) {

        }else {
            throw new IllegalArgumentException("normal string case15 失败");
        }

        //"example16":      "S4//3546",   //注释 // 似懂非懂是
        testCase = NORMAL_STRING_CASE_LIST.get(7);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example16")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"S4//3546\"")
                && matcher.group(NORMAL_FILED_ANNOTATION).equals("//注释 // 似懂非懂是")) {

        } else {
            throw new IllegalArgumentException("normal string case16 失败");
        }


        //"example17": "S4//3546"   //注释 // 似懂非懂是
        testCase = NORMAL_STRING_CASE_LIST.get(8);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example17")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"S4//3546\"")
                && matcher.group(NORMAL_FILED_ANNOTATION).equals("//注释 // 似懂非懂是")) {

        } else {
            throw new IllegalArgumentException("normal string case17 失败");
        }


        //"example18": "S4{3546"   //注释 // 似懂非懂是
        testCase = NORMAL_STRING_CASE_LIST.get(9);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example18")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"S4{3546\"")
                && matcher.group(NORMAL_FILED_ANNOTATION).equals("//注释 // 似懂非懂是")) {

        } else {
            throw new IllegalArgumentException("normal string case18 失败");
        }

        //     "example19"     : "[]"
        testCase = NORMAL_STRING_CASE_LIST.get(10);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example19")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"[]\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        } else {
            throw new IllegalArgumentException("normal string case19 失败");
        }


        //"example20": "[",
        testCase = NORMAL_STRING_CASE_LIST.get(11);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example20")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"[\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        } else {
            throw new IllegalArgumentException("normal string case20 失败");
        }
        //"example21": "]",
        testCase = NORMAL_STRING_CASE_LIST.get(12);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example21")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"]\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        } else {
            throw new IllegalArgumentException("normal string case21 失败");
        }
        //"example22": "}",
        testCase = NORMAL_STRING_CASE_LIST.get(13);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example22")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"}\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        } else {
            throw new IllegalArgumentException("normal string case22 失败");
        }
        //"example23": "true",
        testCase = NORMAL_STRING_CASE_LIST.get(14);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example23")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"true\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        } else {
            throw new IllegalArgumentException("normal string case23 失败");
        }
        //"example24": "true"
        testCase = NORMAL_STRING_CASE_LIST.get(15);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example24")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"true\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        } else {
            throw new IllegalArgumentException("normal string case24 失败");
        }
        //"example25": "S4{3546"   //注释 // 似懂"非懂是
        testCase = NORMAL_STRING_CASE_LIST.get(16);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example25")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"S4{3546\"")
                && matcher.group(NORMAL_FILED_ANNOTATION).equals("//注释 // 似懂\"非懂是")) {

        } else {
            throw new IllegalArgumentException("normal string case25 失败");
        }
        //"example26": "S4{3"546"   //注释 // 似懂"非懂是
        testCase = NORMAL_STRING_CASE_LIST.get(17);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example26")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"S4{3\"546\"")
                && matcher.group(NORMAL_FILED_ANNOTATION).equals("//注释 // 似懂\"非懂是")) {

        } else {
            throw new IllegalArgumentException("normal string case26 失败");
        }
        //"example27": "S4{3"546"
        testCase = NORMAL_STRING_CASE_LIST.get(18);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example27")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"S4{3\"546\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        } else {
            throw new IllegalArgumentException("normal string case27 失败");
        }
        //"example28": """
        testCase = NORMAL_STRING_CASE_LIST.get(19);
        matcher = NORMAL_STRING_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example28")
                && matcher.group(NORMAL_FILED_VALUE).equals("\"\"\"")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        } else {
            throw new IllegalArgumentException("normal string case28 失败");
        }
    }

    private static List<String> NORMAL_NUMBER_CASE_LIST = Lists.newArrayList(
            "\"example5\": 1",
            "\"example6\": 1,",
            "\"example7\": 1232",
            "\"example8\": 1232,",
            "\"example9\": 12.32",
            "\"example10\": 12.32,",
            "\"example12\": 1232    // 注释  32r5345",
            "\"example13\": 1232,    // 注释  32r5345"
    );
    private static void assertNormalIntegerCase() {
        //"example5": 1
        String testCase = NORMAL_NUMBER_CASE_LIST.get(0);
        Matcher matcher = NORMAL_NUMBER_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example5")
                && matcher.group(NORMAL_FILED_VALUE).equals("1")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal integer case5失败");
        }

        //"example6": 1,
        testCase = NORMAL_NUMBER_CASE_LIST.get(1);
        matcher = NORMAL_NUMBER_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example6")
                && matcher.group(NORMAL_FILED_VALUE).equals("1")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal integer case6失败");
        }

        //"example7": 1232
        testCase = NORMAL_NUMBER_CASE_LIST.get(2);
        matcher = NORMAL_NUMBER_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example7")
                && matcher.group(NORMAL_FILED_VALUE).equals("1232")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal integer case7失败");
        }

        //"example8": 1232,
        testCase = NORMAL_NUMBER_CASE_LIST.get(3);
        matcher = NORMAL_NUMBER_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example8")
                && matcher.group(NORMAL_FILED_VALUE).equals("1232")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal integer case8失败");
        }

        //"example9": 12.32
        testCase = NORMAL_NUMBER_CASE_LIST.get(4);
        matcher = NORMAL_NUMBER_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example9")
                && matcher.group(NORMAL_FILED_VALUE).equals("12.32")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal integer case9失败");
        }

        //"example10": 12.32,
        testCase = NORMAL_NUMBER_CASE_LIST.get(5);
        matcher = NORMAL_NUMBER_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example10")
                && matcher.group(NORMAL_FILED_VALUE).equals("12.32")
                && matcher.group(NORMAL_FILED_ANNOTATION) == null) {

        }else {
            throw new IllegalArgumentException("normal integer case10失败");
        }

        //"example12": 1232    // 注释  32r5345
        testCase = NORMAL_NUMBER_CASE_LIST.get(6);
        matcher = NORMAL_NUMBER_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example12")
                && matcher.group(NORMAL_FILED_VALUE).equals("1232")
                && matcher.group(NORMAL_FILED_ANNOTATION).equals("// 注释  32r5345")) {

        }else {
            throw new IllegalArgumentException("normal integer case12失败");
        }

        //"example13": 1232,    // 注释  32r5345
        testCase = NORMAL_NUMBER_CASE_LIST.get(7);
        matcher = NORMAL_NUMBER_PATTERN.matcher(testCase);
        if (matcher.find() && matcher.group(NORMAL_FIELD_NAME).equals("example13")
                && matcher.group(NORMAL_FILED_VALUE).equals("1232")
                && matcher.group(NORMAL_FILED_ANNOTATION).equals("// 注释  32r5345")) {

        }else {
            throw new IllegalArgumentException("normal integer case13失败");
        }

    }

    private static List<String> ARRAY_PLAIN_ITEM_CASE_LIST = Lists.newArrayList(
            "\"sdfd\"",
            "12432",
            "\"124\"sfs\",",
            "1244,",
            "true",
            "true,",
            "0.34",
            "\"0.24\"",
            "\"0.24\",",
            "\"324\",",
            "\"我是中国人\"",
            "\"我是中国人\","
    );

    private static void assertArrayPlainItemCase() {
        //"sdfd"
        String testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(0);
        Matcher matcher = ARRAY_PLAIN_ITEM_STRING.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("sdfd")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }

        //12432
        testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(1);
        matcher = ARRAY_PLAIN_ITEM_NUMBER.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("12432")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }

        //"124"sfs",
        testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(2);
        matcher = ARRAY_PLAIN_ITEM_STRING.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("124\"sfs")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }

        //1244,
        testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(3);
        matcher = ARRAY_PLAIN_ITEM_NUMBER.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("1244")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }

        //true
        testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(4);
        matcher = ARRAY_PLAIN_ITEM_BOOLEAN.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("true")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }
        //true,
        testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(5);
        matcher = ARRAY_PLAIN_ITEM_BOOLEAN.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("true")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }
        //0.34
        testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(6);
        matcher = ARRAY_PLAIN_ITEM_NUMBER.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("0.34")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }
        //"0.24"
        testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(7);
        matcher = ARRAY_PLAIN_ITEM_STRING.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("0.24")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }
        //"0.24",
        testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(8);
        matcher = ARRAY_PLAIN_ITEM_STRING.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("0.24")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }
        //"324",
        testCase = ARRAY_PLAIN_ITEM_CASE_LIST.get(9);
        matcher = ARRAY_PLAIN_ITEM_STRING.matcher(testCase);
        if (matcher.find() && matcher.group(ARRAY_PLAIN_ITEM_VALUE).equals("324")) {

        } else {
            throw new IllegalArgumentException("array plain item 【" + testCase +"】" + "失败");
        }
    }
}

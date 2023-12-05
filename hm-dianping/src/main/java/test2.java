import java.util.ArrayList;

/**
 * @create 2023-10-17
 */
public class test2 {
    public class StringSplitter {
        public  String[] split(String input, String delimiter) {
            // 判断输入是否为空或分隔符是否为空
            if (input == null || delimiter == null) {
                return new String[0]; // 返回空数组
            }
            // 使用 StringBuilder 构建结果数组
            StringBuilder builder = new StringBuilder();
            int startIndex = 0;
            int delimiterIndex;
            // 使用 ArrayList 存储分割后的字符串
            ArrayList<String> result = new ArrayList<>();
            // 遍历输入字符串，查找分隔符并进行分割
            while ((delimiterIndex = input.indexOf(delimiter, startIndex)) != -1) {
                // 将分隔符之前的部分添加到结果数组
                builder.append(input, startIndex, delimiterIndex);
                result.add(builder.toString());
                // 清空 StringBuilder，准备下一次迭代
                builder.setLength(0);
                // 更新 startIndex 以继续搜索下一个分隔符
                startIndex = delimiterIndex + delimiter.length();
            }
            // 添加剩余部分到结果数组
            builder.append(input, startIndex, input.length());
            result.add(builder.toString());
            // 转换为数组并返回
            return result.toArray(new String[0]);
        }
        public  void main(String[] args) {
            String input = "Hello,World,Java";
            String delimiter = ",";
            String[] result = split(input, delimiter);

            // 打印分割后的结果
            for (String item : result) {
                System.out.println(item);
            }
        }
    }

}

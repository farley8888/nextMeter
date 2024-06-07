package com.ex.electric;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
        System.out.println("ExampleUnitTest.addition_isCorrect");
        String result = "55AA0031020100E4415830303031202020200002202302252047202302252119000000020000000000162500130000000000130001A355AA";
//
//        if (result.startsWith("55AA") && result.endsWith("55AA") &&
//                result.length() > 16 && result.startsWith("E4", 14)){
//            System.out.println("ExampleUnitTest.addition_isCorrect 结束 ");
//        }
        result = "83030355AA0031020100E4415031202020200002202302252047202302252130000000000130001A355AA190000000200000000001625001";
        StringBuffer sb = new StringBuffer(result);
        checkData(sb);
        System.out.println("ExampleUnitTest. result = " + sb);
        checkData(sb);
        System.out.println("ExampleUnitTest. result = " + sb);

    }


    private void checkData(StringBuffer strBuf) {
        int start = 0, end = 0;
        if ((start = strBuf.indexOf("55AA")) >= 0){
            end = strBuf.indexOf("55AA", start + 2);
            if (end > 0){
                String itemData = strBuf.substring(start, end + 4);
                distributeData(itemData, "check");
                if (start > 0){
                    distributeData(strBuf.substring(0, start), "errorData");
                }
                strBuf.delete(start, end + 4); // 删除分发的数据
            }
        }
    }

    void distributeData(String data, String where){
        System.out.println("ExampleUnitTest.distributeData data = " + data + ", where = " + where);

    }

}
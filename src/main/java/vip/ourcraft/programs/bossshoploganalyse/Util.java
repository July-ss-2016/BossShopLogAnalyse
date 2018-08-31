package vip.ourcraft.programs.bossshoploganalyse;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

/**
 * Created by July on 2018/06/08.
 */
public class Util {
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static String readFile(File file) {
        if (!file.exists()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append(LINE_SEPARATOR);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return sb.toString();
    }

    public static double formatDouble(double d) {
        BigDecimal bigDecimal = new BigDecimal(d).setScale(2, RoundingMode.UP);

        return bigDecimal.doubleValue();
    }
}

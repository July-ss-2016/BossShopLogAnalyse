package vip.ourcraft.programs.bossshoploganalyse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;

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

    public static void setCellValue(Cell cell, String value) {
        setCellStyle(cell);
        cell.setCellValue(value);
    }

    public static void setCellValue(Cell cell, double value) {
        setCellStyle(cell);
        cell.setCellValue(value);
    }

    private static void setCellStyle(Cell cell) {
        Workbook workbook = cell.getRow().getSheet().getWorkbook();
        CellStyle cellStyle = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();

        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        cellStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        cellStyle.setFont(font);
        cell.setCellStyle(cellStyle);
    }
}

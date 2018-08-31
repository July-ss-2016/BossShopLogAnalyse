package vip.ourcraft.programs.bossshoploganalyse;

import com.google.gson.Gson;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainWindow {
    private static final String[] XLSX_HEADERS = new String[] {"交易总金额", "交易总次数", "交易总人数"};
    private JFrame frame;
    private JPanel panel;
    private JButton runButton;
    private JTextField priceFileTextField;
    private JTextField logFilePathTextField;
    private JButton cleanOldAnalysesButton;

    MainWindow() {
        runButton.addActionListener(e -> {
            if (priceFileTextField.getText().equals("")) {
                sendWarningMsgBox("价格文件路径不能为空!");
                return;
            }

            if (logFilePathTextField.getText().equals("")) {
                sendWarningMsgBox("日志文件路径不能为空!");
                return;
            }

            output();
        });

        cleanOldAnalysesButton.addActionListener(e -> {
            cleanOldAnalyses();
        });
        
        // 实现拖拽路径
        registerTextFieldDropTarget(logFilePathTextField);
        registerTextFieldDropTarget(priceFileTextField);
    }

    private void output() {
        File priceFile = new File(priceFileTextField.getText());
        File logFile = new File(logFilePathTextField.getText());

        if (!priceFile.exists()) {
            sendWarningMsgBox("价格文件不存在!");
            return;
        }

        if (!logFile.exists()) {
            sendWarningMsgBox("日志文件不存在!");
            return;
        }

        Workbook workbook = null;

        try {
            workbook = new XSSFWorkbook(new BufferedInputStream(new FileInputStream(priceFile), 2048)); // 不知道为什么不能用File作为参数, FileInputStream可行
        } catch (IOException e) {
            sendErrorMsgBox(e.getLocalizedMessage());
            System.out.println(e.getMessage());
        }

        if (workbook == null || workbook.getNumberOfSheets() == 0) {
            sendWarningMsgBox("没有找到数据表!");
            return;
        }

        String logLines = Util.readFile(logFile);

        if (logLines == null) {
            sendWarningMsgBox("日志文件为空!");
            return;
        }

        HashMap<String, BossShopLogAnalyse> bsLogAnalyses = new HashMap<>();

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            String sheetName = sheet.getSheetName();
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                continue;
            }

            // 逐行统计
            for (String line : logLines.split(Util.LINE_SEPARATOR)) {
                Gson gson = new Gson();
                BossShopLog bossShopLog = gson.fromJson(line.substring(10), BossShopLog.class);

                if (!bossShopLog.getShopName().equalsIgnoreCase(sheetName)) {
                    continue;
                }

                String itemIndex = bossShopLog.getShopName() + ":" + bossShopLog.getItemName();
                String player = bossShopLog.getPlayerName();
                boolean isSellShop = bossShopLog.getReward().startsWith("[");

                // 不存在统计类则创建
                if (!bsLogAnalyses.containsKey(itemIndex)) {
                    BossShopLogAnalyse temp = new BossShopLogAnalyse();

                    bsLogAnalyses.put(itemIndex, temp);
                    temp.setItemIndex(itemIndex);
                    temp.setItemInfo(isSellShop ? bossShopLog.getReward() : bossShopLog.getPrice());
                    temp.setSourceFile(logFile);
                }

                // 得到统计结果，继续统计
                BossShopLogAnalyse logAnalyse = bsLogAnalyses.get(itemIndex);

                // 递增总价
                logAnalyse.addTotalTradedPrice(Double.parseDouble(isSellShop ? bossShopLog.getPrice() : bossShopLog.getReward()));
                // 递增交易次数
                logAnalyse.addTotalTradedCount(1);
                // 添加交易过的玩家id
                logAnalyse.putTradedPlayer(player);
            }

            // 没统计数据直接跳过以下代码
            if (bsLogAnalyses.size() == 0) {
                continue;
            }

            int firstAnalyseHeader = headerRow.getLastCellNum();
            final CellStyle CENTER_STYLE = workbook.createCellStyle();

            CENTER_STYLE.setAlignment(XSSFCellStyle.ALIGN_CENTER);

            // 设置统计表头
            for (int i = 0; i < XLSX_HEADERS.length; i++) {
                int index = firstAnalyseHeader + i;
                Cell cell = headerRow.getCell(index);

                if (cell == null) {
                    cell = headerRow.createCell(index);
                }

                cell.setCellValue("BSLA-" + logFile.getName() + "-" + XLSX_HEADERS[i]);
                cell.setCellStyle(CENTER_STYLE);
            }

            HashMap<String, Row> firstColumnRows = new HashMap<>();

            // 把第一列对应的物品索引和表格索引存储起来
            for (int i = 1; i < sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                Cell cell = row.getCell(0);

                if (cell == null) {
                    continue;
                }

                String value = String.valueOf(sheet.getRow(i).getCell(0).getNumericCellValue());
                int tmp = value.indexOf(".");

                firstColumnRows.put(sheet.getSheetName() + ":" + value.substring(0, tmp == -1 ? value.length() : tmp), sheet.getRow(i));
            }

            for (Map.Entry<String, BossShopLogAnalyse> entry : bsLogAnalyses.entrySet()) {
                BossShopLogAnalyse analyse = entry.getValue();
                // 在excel中物品索引没有i作为前缀
                String itemIndex = entry.getKey().replace("i", "");


                if (firstColumnRows.containsKey(itemIndex)) {
                    Row row = firstColumnRows.get(itemIndex);

                    // 填充数据
                    row.createCell(firstAnalyseHeader).setCellValue(Util.halfDownDouble(analyse.getTotalTradedPrice()) + "(" + Util.halfDownDouble(analyse.getPercentOfGlobalTradedPrice()) * 100 + "%)");
                    row.createCell(firstAnalyseHeader + 1).setCellValue(analyse.getTotalTradedCount() + "(" + Util.halfDownDouble(analyse.getPercentOfGlobalTradedCount()) * 100 + "%)");
                    row.createCell(firstAnalyseHeader + 2).setCellValue(analyse.getTradedPlayerCount() + "(" + Util.halfDownDouble(analyse.getPercentOfGlobalTradedPlayerCount()) * 100 + "%)");

                    // 设置居中
                    row.getCell(firstAnalyseHeader).setCellStyle(CENTER_STYLE);
                    row.getCell(firstAnalyseHeader + 1).setCellStyle(CENTER_STYLE);
                    row.getCell(firstAnalyseHeader + 2).setCellStyle(CENTER_STYLE);
                }
            }

            for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                sheet.autoSizeColumn(i);
            }
        }

        try {
            workbook.write(new FileOutputStream(priceFile));
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMsgBox(e.getMessage());
        }

        sendInfoMsgBox("更新文件成功!");
    }

    private void cleanOldAnalyses() {
        File priceFile = new File(priceFileTextField.getText());

        if (!priceFile.exists()) {
            sendWarningMsgBox("价格文件不存在!");
            return;
        }

        Workbook priceWorkook = null;

        try {
            priceWorkook = new XSSFWorkbook(new BufferedInputStream(new FileInputStream(priceFile), 2048)); // 不知道为什么不能用File作为参数, FileInputStream可行
        } catch (IOException e) {
            sendErrorMsgBox(e.getLocalizedMessage());
            System.out.println(e.getMessage());
        }

        if (priceWorkook == null || priceWorkook.getNumberOfSheets() == 0) {
            sendWarningMsgBox("没有找到数据表!");
            return;
        }

        for (int sheetIndex = 0; sheetIndex < priceWorkook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = priceWorkook.getSheetAt(sheetIndex);
            Row headerRow = sheet.getRow(0);
            List<Integer> analyseColumns = new ArrayList<>();

            // 首先记录需要删除的列数
            for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
                Cell cell = headerRow.getCell(cellIndex);

                if (cell != null && cell.getCellTypeEnum() == CellType.STRING) {
                    String value = cell.getStringCellValue();

                    if (value != null && value.startsWith("BSLA")) {
                        analyseColumns.add(cellIndex);
                    }
                }
            }

            // 删除指定列
            for (int rowIndex = 0; rowIndex < sheet.getLastRowNum(); rowIndex++) {
                for (int column : analyseColumns) {
                    Cell cell = sheet.getRow(rowIndex).getCell(column);

                    if (cell != null) {
                        cell.getRow().removeCell(cell);
                    }
                }
            }

            try {
                priceWorkook.write(new FileOutputStream(priceFile));
            } catch (IOException e) {
                e.printStackTrace();
                sendErrorMsgBox(e.getMessage());
            }

            sendInfoMsgBox("清除旧统计信息成功!");
        }
    }

    private void registerTextFieldDropTarget(JTextField textField) {
        // 文件路径接收器
        new DropTarget(textField, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    // 接收拖拽来的数据
                    event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

                    try {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) (event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));

                        textField.setText(files.get(0).getAbsolutePath());
                        event.dropComplete(true);
                    } catch (UnsupportedFlavorException | IOException e) {
                        e.printStackTrace();
                        sendErrorMsgBox(e.getLocalizedMessage());
                    }

                    return;
                }

                event.rejectDrop();
            }
        });
    }

    public void init() {
        this.frame = new JFrame("BossShopLogAnalyse");

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(500,200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void sendInfoMsgBox(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "警告", JOptionPane.INFORMATION_MESSAGE);
    }

    private void sendWarningMsgBox(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "警告", JOptionPane.WARNING_MESSAGE);
    }

    private void sendErrorMsgBox(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "错误", JOptionPane.ERROR_MESSAGE);
    }
}
